package com.example.luluuu.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

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
data class StockHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stockId: Long,
    val date: Date,
    val oldQuantity: Int,
    val newQuantity: Int,
    val oldPrice: Double,
    val newPrice: Double,
    val action: String // e.g., "EDIT", "DELETE", "CREATE"
)