package com.example.luluuu.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey
    @DocumentId
    var id: String = "",
    var name: String = "",
    var phoneNumber: String = "",
    var address: String = ""
)
