package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val labourId: Int,
    val labourName: String,
    val siteId: Int,             // Associated site
    val amount: Double,
    val date: String,            // YYYY-MM-DD
    val paymentMode: String,     // Cash, Bank Transfer, UPI
    val paymentType: String = "Daily Wage (Kharchi)",  // e.g. Daily Wage (Kharchi), Extra Expense, Weekly Advance, Bonus, Deduction
    val remarks: String,
    val status: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
