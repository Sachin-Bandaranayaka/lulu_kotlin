package com.example.luluuu.model

data class InvoiceItem(
    var stockId: Long = 0,
    var productName: String = "",
    var quantity: Int = 0,
    var price: Double = 0.0,
    var total: Double = 0.0,
    var isFree: Boolean = false
)
