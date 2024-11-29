package com.example.luluuu

data class Product(
    var name: String = "",
    var price: Double = 0.0,
    var quantity: Int = 1
) {
    fun total(): Double = price * quantity
} 