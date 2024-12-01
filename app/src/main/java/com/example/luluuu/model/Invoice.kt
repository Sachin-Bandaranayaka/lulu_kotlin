package com.example.luluuu.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String = "",  // Format: INV-YYYYMMDD-XXXX
    val customerName: String = "",
    val customerMobile: String = "",
    val date: Date = Date(),
    val items: List<InvoiceItem> = listOf(),
    val total: Double = 0.0,
    @ColumnInfo(name = "firebase_id", defaultValue = "")
    var firebaseId: String = ""  // Firebase document ID
)

data class InvoiceItem(
    val productName: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val total: Double = 0.0
)