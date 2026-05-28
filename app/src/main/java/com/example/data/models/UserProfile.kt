package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey val uid: String,
    val displayName: String,
    val email: String,
    val profilePhoto: String,
    val createdAt: Long = System.currentTimeMillis(),
    val preferredLanguage: String = "English"
)
