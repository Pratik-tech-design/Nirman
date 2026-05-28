package com.example.data.firebase

import android.content.Context
import android.content.SharedPreferences
import com.example.data.database.LabourDatabase
import com.example.data.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Task failed"))
        }
    }
}

/**
 * Robust Production-Grade Firebase Authentication Service.
 * Persists app-level configurations and manages real user-sessions.
 */
class FirebaseAuthService private constructor(context: Context, private val database: LabourDatabase) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences("firebase_auth_prefs", Context.MODE_PRIVATE)
    
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        // Automatically hook into Firebase session changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                val preferredLanguage = prefs.getString("current_language", "English") ?: "English"
                val profile = UserProfile(
                    uid = user.uid,
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Contractor",
                    email = user.email ?: "",
                    profilePhoto = user.photoUrl?.toString() ?: "",
                    preferredLanguage = preferredLanguage
                )
                _currentUser.value = profile
            } else {
                _currentUser.value = null
            }
        }
    }

    suspend fun login(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank() || password.isBlank()) {
                return@withContext Result.failure(Exception("Email and password cannot be empty"))
            }

            val cleanEmail = email.trim().lowercase()

            // Safe Auto-migration/Register for local predefined seeded Contractor
            if (cleanEmail == "contractor@test.com" && password == "password") {
                try {
                    // Try to log in directly
                    val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).awaitTask()
                    val user = authResult.user!!
                    val profile = UserProfile(
                        uid = user.uid,
                        displayName = "Aditya Verma",
                        email = cleanEmail,
                        profilePhoto = "",
                        preferredLanguage = "English"
                    )
                    database.labourDao().insertUserProfile(profile)
                    persistSession(profile)
                    _currentUser.value = profile
                    return@withContext Result.success(profile)
                } catch (e: Exception) {
                    // If login fails because user wasn't registered in Firebase, automatically sign them up!
                    try {
                        val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).awaitTask()
                        val user = authResult.user!!
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName("Aditya Verma")
                            .build()
                        user.updateProfile(profileUpdates).awaitTask()
                        val profile = UserProfile(
                            uid = user.uid,
                            displayName = "Aditya Verma",
                            email = cleanEmail,
                            profilePhoto = "",
                            preferredLanguage = "English"
                        )
                        database.labourDao().insertUserProfile(profile)
                        persistSession(profile)
                        _currentUser.value = profile
                        return@withContext Result.success(profile)
                    } catch (signupError: Exception) {
                        // If signup on Firebase failed (e.g. timeout or no network), proceed to fallback simulated authenticate if necessary or throw
                    }
                }
            }

            val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).awaitTask()
            val user = authResult.user ?: return@withContext Result.failure(Exception("Failed to locate authenticated user"))

            val profile = UserProfile(
                uid = user.uid,
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Contractor",
                email = cleanEmail,
                profilePhoto = user.photoUrl?.toString() ?: "",
                preferredLanguage = prefs.getString("current_language", "English") ?: "English"
            )

            // Cache local SQLite profile
            database.labourDao().insertUserProfile(profile)

            persistSession(profile)
            _currentUser.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(displayName: String, email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            if (displayName.isBlank() || email.isBlank() || password.isBlank()) {
                return@withContext Result.failure(Exception("All fields are mandatory"))
            }

            if (password.length < 6) {
                return@withContext Result.failure(Exception("Password must be at least 6 characters"))
            }

            val cleanEmail = email.trim().lowercase()
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).awaitTask()
            val user = authResult.user ?: return@withContext Result.failure(Exception("Failed to register Firebase User"))

            // Update user profile display name
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
                user.updateProfile(profileUpdates).awaitTask()
            } catch (e: Exception) {
                // Ignore non-blocking display-name update exceptions
            }

            val newProfile = UserProfile(
                uid = user.uid,
                displayName = displayName.trim(),
                email = cleanEmail,
                profilePhoto = "",
                preferredLanguage = "English"
            )

            // Cache local SQLite profile
            database.labourDao().insertUserProfile(newProfile)

            persistSession(newProfile)
            _currentUser.value = newProfile
            Result.success(newProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
        prefs.edit().apply {
            remove("current_uid")
            remove("current_email")
            remove("current_display_name")
            remove("current_language")
            apply()
        }
        _currentUser.value = null
    }

    private fun persistSession(user: UserProfile) {
        prefs.edit().apply {
            putString("current_uid", user.uid)
            putString("current_email", user.email)
            putString("current_display_name", user.displayName)
            putString("current_language", user.preferredLanguage)
            apply()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseAuthService? = null

        fun getInstance(context: Context, database: LabourDatabase): FirebaseAuthService {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseAuthService(context.applicationContext, database)
                INSTANCE = instance
                instance
            }
        }
    }
}
