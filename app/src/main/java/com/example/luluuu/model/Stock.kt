package com.example.luluuu.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

@Entity(tableName = "stocks")
data class Stock(
    @PrimaryKey(autoGenerate = true)
    @get:Exclude  // Exclude from Firestore serialization
    var id: Long = 0,
    
    @get:Exclude  // Exclude from Firestore serialization
    var firebaseId: String = "",
    
    var name: String = "",
    var price: Double = 0.0,
    var quantity: Int = 0,
    var description: String = ""
) 