package com.example.easyshop.model

data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0
)