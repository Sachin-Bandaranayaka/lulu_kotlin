package com.example.luluuu.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class Stock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var name: String,
    var price: Double,
    var quantity: Int,
    var description: String = ""
) 