package com.example.data.repository

import com.example.data.database.LabourDao
import com.example.data.models.Labour
import com.example.data.models.Payment
import com.example.data.models.Site
import com.example.data.models.UserProfile
import com.example.data.models.Attendance
import com.example.data.models.AttendanceDraft
import com.example.data.firebase.awaitTask
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LabourRepository(private val labourDao: LabourDao) {

    private val firestore = FirebaseFirestore.getInstance()

    // Retrieve User Profile
    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = firestore.collection("users")
                .document(uid)
                .get()
                .awaitTask()
            if (doc.exists()) {
                UserProfile(
                    uid = doc.getString("uid") ?: uid,
                    displayName = doc.getString("displayName") ?: "Contractor",
                    email = doc.getString("email") ?: "",
                    profilePhoto = doc.getString("profilePhoto") ?: "",
                    preferredLanguage = doc.getString("preferredLanguage") ?: "English"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            // Fallback to local cache in case of offline / no config
            labourDao.getUserProfile(uid)
        }
    }

    suspend fun insertUserProfile(profile: UserProfile) {
        try {
            firestore.collection("users")
                .document(profile.uid)
                .set(profile)
                .awaitTask()
        } catch (e: Exception) {
            // Ignore if offline
        }
        labourDao.insertUserProfile(profile)
    }

    // User-Isolated collections
    fun getAllSites(userId: String): Flow<List<Site>> = callbackFlow {
        val listener = firestore.collection("sites")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Site(
                                id = doc.getLong("id")?.toInt() ?: 0,
                                userId = doc.getString("userId") ?: "",
                                name = doc.getString("name") ?: "",
                                location = doc.getString("location") ?: "",
                                managerName = doc.getString("managerName") ?: "",
                                status = doc.getString("status") ?: "Active",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.id }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAllLabour(userId: String): Flow<List<Labour>> = callbackFlow {
        val listener = firestore.collection("labour")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Labour(
                                id = doc.getLong("id")?.toInt() ?: 0,
                                userId = doc.getString("userId") ?: "",
                                name = doc.getString("name") ?: "",
                                phoneNumber = doc.getString("phoneNumber") ?: "",
                                skillType = doc.getString("skillType") ?: "",
                                dailyWage = doc.getDouble("dailyWage") ?: 0.0,
                                siteId = doc.getLong("siteId")?.toInt(),
                                status = doc.getString("status") ?: "Active",
                                repeatAutomatically = doc.getBoolean("repeatAutomatically") ?: false,
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedBy { it.name }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAllPayments(userId: String): Flow<List<Payment>> = callbackFlow {
        val listener = firestore.collection("payments")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Payment(
                                id = doc.getLong("id")?.toInt() ?: 0,
                                userId = doc.getString("userId") ?: "",
                                labourId = doc.getLong("labourId")?.toInt() ?: 0,
                                labourName = doc.getString("labourName") ?: "",
                                siteId = doc.getLong("siteId")?.toInt() ?: 0,
                                amount = doc.getDouble("amount") ?: 0.0,
                                date = doc.getString("date") ?: "",
                                paymentMode = doc.getString("paymentMode") ?: "",
                                paymentType = doc.getString("paymentType") ?: "Daily Wage (Kharchi)",
                                remarks = doc.getString("remarks") ?: "",
                                status = doc.getString("status") ?: "",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedWith(compareByDescending<Payment> { it.date }.thenByDescending { it.id })
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAllAttendance(userId: String): Flow<List<Attendance>> = callbackFlow {
        val listener = firestore.collection("attendance")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Attendance(
                                id = doc.getLong("id")?.toInt() ?: 0,
                                userId = doc.getString("userId") ?: "",
                                labourId = doc.getLong("labourId")?.toInt() ?: 0,
                                siteId = doc.getLong("siteId")?.toInt() ?: 0,
                                date = doc.getString("date") ?: "",
                                status = doc.getString("status") ?: "",
                                generatedPaymentId = doc.getLong("generatedPaymentId")?.toInt(),
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedWith(compareByDescending<Attendance> { it.date }.thenByDescending { it.id })
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertAttendance(attendance: Attendance): Long {
        val id = if (attendance.id == 0) (System.currentTimeMillis() % 1000000000).toInt() + (1..100000).random() else attendance.id
        val updated = attendance.copy(id = id)
        firestore.collection("attendance")
            .document("${updated.userId}_${updated.id}")
            .set(updated)
            .awaitTask()
        return id.toLong()
    }

    suspend fun getAttendanceByDate(userId: String, date: String): List<Attendance> {
        val q = firestore.collection("attendance")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .get()
            .awaitTask()
        return q.documents.mapNotNull { doc ->
            try {
                Attendance(
                    id = doc.getLong("id")?.toInt() ?: 0,
                    userId = doc.getString("userId") ?: "",
                    labourId = doc.getLong("labourId")?.toInt() ?: 0,
                    siteId = doc.getLong("siteId")?.toInt() ?: 0,
                    date = doc.getString("date") ?: "",
                    status = doc.getString("status") ?: "",
                    generatedPaymentId = doc.getLong("generatedPaymentId")?.toInt(),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    // Attendance Drafts
    suspend fun getAttendanceDraftsByDate(userId: String, date: String): List<AttendanceDraft> {
        val q = firestore.collection("attendance_drafts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .get()
            .awaitTask()
        return q.documents.mapNotNull { doc ->
            try {
                AttendanceDraft(
                    id = doc.getLong("id")?.toInt() ?: 0,
                    userId = doc.getString("userId") ?: "",
                    labourId = doc.getLong("labourId")?.toInt() ?: 0,
                    siteId = doc.getLong("siteId")?.toInt() ?: 0,
                    date = doc.getString("date") ?: "",
                    status = doc.getString("status") ?: "",
                    finalized = doc.getBoolean("finalized") ?: false,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getAttendanceDraftsByDateFlow(userId: String, date: String): Flow<List<AttendanceDraft>> = callbackFlow {
        val listener = firestore.collection("attendance_drafts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            AttendanceDraft(
                                id = doc.getLong("id")?.toInt() ?: 0,
                                userId = doc.getString("userId") ?: "",
                                labourId = doc.getLong("labourId")?.toInt() ?: 0,
                                siteId = doc.getLong("siteId")?.toInt() ?: 0,
                                date = doc.getString("date") ?: "",
                                status = doc.getString("status") ?: "",
                                finalized = doc.getBoolean("finalized") ?: false,
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAllAttendanceDraftsFlow(userId: String): Flow<List<AttendanceDraft>> = callbackFlow {
        val listener = firestore.collection("attendance_drafts")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            AttendanceDraft(
                                id = doc.getLong("id")?.toInt() ?: 0,
                                userId = doc.getString("userId") ?: "",
                                labourId = doc.getLong("labourId")?.toInt() ?: 0,
                                siteId = doc.getLong("siteId")?.toInt() ?: 0,
                                date = doc.getString("date") ?: "",
                                status = doc.getString("status") ?: "",
                                finalized = doc.getBoolean("finalized") ?: false,
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.date }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertAttendanceDraft(draft: AttendanceDraft): Long {
        val id = if (draft.id == 0) (System.currentTimeMillis() % 1000000000).toInt() + (1..100000).random() else draft.id
        val updated = draft.copy(id = id)
        firestore.collection("attendance_drafts")
            .document("${updated.userId}_${updated.id}")
            .set(updated)
            .awaitTask()
        return id.toLong()
    }

    suspend fun insertAttendanceDrafts(drafts: List<AttendanceDraft>) {
        if (drafts.isEmpty()) return
        val batch = firestore.batch()
        drafts.forEach { draft ->
            val id = if (draft.id == 0) (System.currentTimeMillis() % 1000000000).toInt() + (1..100000).random() else draft.id
            val updated = draft.copy(id = id)
            val ref = firestore.collection("attendance_drafts")
                .document("${updated.userId}_${updated.id}")
            batch.set(ref, updated)
        }
        batch.commit().awaitTask()
    }

    suspend fun updateAttendanceDraft(draft: AttendanceDraft) {
        firestore.collection("attendance_drafts")
            .document("${draft.userId}_${draft.id}")
            .set(draft)
            .awaitTask()
    }

    suspend fun deleteAttendanceDraftsByDate(userId: String, date: String) {
        val q = firestore.collection("attendance_drafts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .get()
            .awaitTask()
        if (q.isEmpty) return
        val batch = firestore.batch()
        q.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().awaitTask()
    }

    // Site ops
    suspend fun insertSite(site: Site): Long {
        val id = if (site.id == 0) (System.currentTimeMillis() % 1000000000).toInt() + (1..100000).random() else site.id
        val updated = site.copy(id = id)
        firestore.collection("sites")
            .document("${updated.userId}_${updated.id}")
            .set(updated)
            .awaitTask()
        return id.toLong()
    }

    suspend fun updateSite(site: Site) {
        firestore.collection("sites")
            .document("${site.userId}_${site.id}")
            .set(site)
            .awaitTask()
    }

    suspend fun deleteSiteById(id: Int, userId: String) {
        firestore.collection("sites")
            .document("${userId}_${id}")
            .delete()
            .awaitTask()
    }

    // Labour ops
    suspend fun insertLabour(labour: Labour): Long {
        val id = if (labour.id == 0) (System.currentTimeMillis() % 1000000000).toInt() + (1..100000).random() else labour.id
        val updated = labour.copy(id = id)
        firestore.collection("labour")
            .document("${updated.userId}_${updated.id}")
            .set(updated)
            .awaitTask()
        return id.toLong()
    }

    suspend fun updateLabour(labour: Labour) {
        firestore.collection("labour")
            .document("${labour.userId}_${labour.id}")
            .set(labour)
            .awaitTask()
    }

    suspend fun deleteLabourById(id: Int, userId: String) {
        firestore.collection("labour")
            .document("${userId}_${id}")
            .delete()
            .awaitTask()
    }

    // Payment ops
    suspend fun insertPayment(payment: Payment): Long {
        val id = if (payment.id == 0) (System.currentTimeMillis() % 1000000000).toInt() + (1..100000).random() else payment.id
        val updated = payment.copy(id = id)
        firestore.collection("payments")
            .document("${updated.userId}_${updated.id}")
            .set(updated)
            .awaitTask()
        return id.toLong()
    }

    fun getLabourBySite(siteId: Int, userId: String): Flow<List<Labour>> = callbackFlow {
        val listener = firestore.collection("labour")
            .whereEqualTo("userId", userId)
            .whereEqualTo("siteId", siteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Labour(
                                id = doc.getLong("id")?.toInt() ?: 0,
                                userId = doc.getString("userId") ?: "",
                                name = doc.getString("name") ?: "",
                                phoneNumber = doc.getString("phoneNumber") ?: "",
                                skillType = doc.getString("skillType") ?: "",
                                dailyWage = doc.getDouble("dailyWage") ?: 0.0,
                                siteId = doc.getLong("siteId")?.toInt(),
                                status = doc.getString("status") ?: "Active",
                                repeatAutomatically = doc.getBoolean("repeatAutomatically") ?: false,
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun preseedFirestoreDataIfEmpty(userId: String) {
        try {
            val q = firestore.collection("sites")
                .whereEqualTo("userId", userId)
                .get()
                .awaitTask()
            if (q.isEmpty) {
                // Seed default sites
                val site1Id = (System.currentTimeMillis() % 10000000).toInt() + 1
                val site2Id = site1Id + 1
                val site3Id = site1Id + 2

                insertSite(Site(id = site1Id, userId = userId, name = "Metro Station Phase 2", location = "Sector 62, Noida", managerName = "R.K. Sharma"))
                insertSite(Site(id = site2Id, userId = userId, name = "High-Rise Tower A", location = "Whitefield, Bengaluru", managerName = "Anurag Verma"))
                insertSite(Site(id = site3Id, userId = userId, name = "City Bypass Roadwork", location = "NH-53, Raipur", managerName = "S.K. Yadav"))

                // Seed initial labor
                val l1Id = site1Id + 10
                val l2Id = site1Id + 20
                val l3Id = site1Id + 30
                val l4Id = site1Id + 40

                insertLabour(Labour(
                    id = l1Id,
                    userId = userId,
                    name = "Ram Singh",
                    phoneNumber = "9876543210",
                    skillType = "Mason (राजमिस्त्री)",
                    dailyWage = 650.0,
                    siteId = site1Id
                ))

                insertLabour(Labour(
                    id = l2Id,
                    userId = userId,
                    name = "Shyam Lal",
                    phoneNumber = "8765432109",
                    skillType = "Helper (मददगार)",
                    dailyWage = 400.0,
                    siteId = site1Id
                ))

                insertLabour(Labour(
                    id = l3Id,
                    userId = userId,
                    name = "Karan Patel",
                    phoneNumber = "7654321098",
                    skillType = "Welder (वेल्डर)",
                    dailyWage = 600.0,
                    siteId = site3Id
                ))

                insertLabour(Labour(
                    id = l4Id,
                    userId = userId,
                    name = "Amit Kumar",
                    phoneNumber = "9123456780",
                    skillType = "Electrician (बिजली मिस्त्री)",
                    dailyWage = 550.0,
                    siteId = site2Id
                ))

                // Seed initial payments
                insertPayment(Payment(
                    id = site1Id + 100,
                    userId = userId,
                    labourId = l1Id,
                    labourName = "Ram Singh",
                    siteId = site1Id,
                    amount = 1300.0,
                    date = "2026-05-25",
                    paymentMode = "UPI",
                    paymentType = "Daily Wage (Kharchi)",
                    remarks = "Paid for 2 days wage"
                ))

                insertPayment(Payment(
                    id = site1Id + 200,
                    userId = userId,
                    labourId = l2Id,
                    labourName = "Shyam Lal",
                    siteId = site1Id,
                    amount = 1400.0,
                    date = "2026-05-26",
                    paymentMode = "Cash",
                    paymentType = "Weekly Advance",
                    remarks = "Advance for weekly expenses"
                ))

                insertPayment(Payment(
                    id = site1Id + 300,
                    userId = userId,
                    labourId = l3Id,
                    labourName = "Karan Patel",
                    siteId = site3Id,
                    amount = 250.0,
                    date = "2026-05-26",
                    paymentMode = "Cash",
                    paymentType = "Deduction",
                    remarks = "Safety helmet damage deduction"
                ))

                insertPayment(Payment(
                    id = site1Id + 400,
                    userId = userId,
                    labourId = l4Id,
                    labourName = "Amit Kumar",
                    siteId = site2Id,
                    amount = 1000.0,
                    date = "2026-05-27",
                    paymentMode = "Bank Transfer",
                    paymentType = "Bonus",
                    remarks = "Overtime and holiday completion bonus"
                ))

                insertPayment(Payment(
                    id = site1Id + 500,
                    userId = userId,
                    labourId = l1Id,
                    labourName = "Ram Singh",
                    siteId = site1Id,
                    amount = 350.0,
                    date = "2026-05-27",
                    paymentMode = "UPI",
                    paymentType = "Extra Expense",
                    remarks = "Purchased site alignment string and tape"
                ))
            }
        } catch (e: Exception) {
            // Ignore preseed failure
        }
    }
}
