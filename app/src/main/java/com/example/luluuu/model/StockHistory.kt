package com.example.luluuu.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.luluuu.db.Converters
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "stock_history",
    foreignKeys = [
        ForeignKey(
            entity = Stock::class,
            parentColumns = ["id"],
            childColumns = ["stockId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stockId")]
)
@TypeConverters(Converters::class)
data class StockHistory(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val stockId: Long,
    val date: Date,
    val oldQuantity: Int,
    val newQuantity: Int,
    val oldPrice: Double,
    val newPrice: Double,
    val action: String, // e.g., "EDIT", "DELETE", "CREATE", "SALE", "FREE_ITEM"
    val invoiceNumber: Int? = null,
    val regularQuantity: Int = 0,
    val freeQuantity: Int = 0,
    val description: String = "" // For additional context like "Free items with bulk purchase"
)