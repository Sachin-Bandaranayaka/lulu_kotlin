package com.example.luluuu.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.luluuu.db.Converters
import java.util.*

@Entity(tableName = "invoices")
@TypeConverters(Converters::class)
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: Int = 0,
    val customerId: String? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    @TypeConverters(Converters::class)
    val date: Date = Date(),
    @TypeConverters(Converters::class)
    val items: List<InvoiceItem> = emptyList(),
    val total: Double = 0.0,
    val discount: Double = 0.0,
    val returnAmount: Double = 0.0,
    val returnDescription: String = ""
)