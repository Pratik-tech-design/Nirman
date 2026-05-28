package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "labour")
data class Labour(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val name: String,
    val phoneNumber: String,
    val skillType: String,      // e.g. Mason (राजमिस्त्री), Helper (मददगार), etc.
    val dailyWage: Double,
    val siteId: Int?,           // Associated Site
    val status: String = "Active",
    val repeatAutomatically: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
