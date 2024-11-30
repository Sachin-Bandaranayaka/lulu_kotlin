package com.example.luluuu.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val category: ExpenseCategory,
    val date: Date
)

enum class ExpenseCategory {
    FUEL,
    VEHICLE_MAINTENANCE,
    OTHER
} 