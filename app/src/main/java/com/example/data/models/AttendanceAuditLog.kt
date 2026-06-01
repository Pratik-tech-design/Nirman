package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_audit_logs")
data class AttendanceAuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val labourId: Int,
    val date: String,
    val oldValue: String,
    val newValue: String,
    val editedBy: String,
    val editedAt: Long = System.currentTimeMillis(),
    val reason: String = ""
)
