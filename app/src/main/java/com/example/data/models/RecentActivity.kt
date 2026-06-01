package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_activities")
data class RecentActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val description: String,
    val type: String, // "labour", "expense", "site"
    val timestamp: Long = System.currentTimeMillis()
)
