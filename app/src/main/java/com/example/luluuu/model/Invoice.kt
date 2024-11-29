package com.example.luluuu.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Date,
    val items: List<InvoiceItem>,
    val total: Double
)

data class InvoiceItem(
    val productName: String,
    val quantity: Int,
    val price: Double,
    val total: Double
) 