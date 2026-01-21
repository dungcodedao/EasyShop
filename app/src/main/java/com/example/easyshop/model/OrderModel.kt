package com.example.easyshop.model

import com.google.firebase.Timestamp
data class OrderModel(
    var id : String = "",
    val date : Timestamp = Timestamp.now(),
    val userId : String ="",
    val items: List<OrderItem> = emptyList(),
    val status : String = "",
    val address: String = "",

    ///admin
    val orderId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val totalAmount: Double = 0.0,
    val orderDate: Long = System.currentTimeMillis(),
    val shippingAddress: String = "",
    val paymentMethod: String = ""
)
