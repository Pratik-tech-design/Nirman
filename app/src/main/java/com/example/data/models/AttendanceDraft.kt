package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_drafts")
data class AttendanceDraft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val labourId: Int,
    val siteId: Int,
    val date: String,             // YYYY-MM-DD
    val status: String,           // "Present", "Absent", "Half Day"
    val finalized: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
