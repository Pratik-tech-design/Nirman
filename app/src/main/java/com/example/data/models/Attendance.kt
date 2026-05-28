package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val labourId: Int,
    val siteId: Int,
    val date: String,             // YYYY-MM-DD
    val status: String,           // "Present" or "Absent"
    val generatedPaymentId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
