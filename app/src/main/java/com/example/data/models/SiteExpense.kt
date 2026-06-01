package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "site_expenses")
data class SiteExpense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val siteId: Int,
    val expenseName: String,
    val amount: Double,
    val paidTo: String,
    val category: String,
    val description: String,
    val expenseDate: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
