package com.example.easyshop.model

import com.google.firebase.Timestamp
data class OrderModel(
//    var id : String = "",
//    val date : Timestamp = Timestamp.now(),
//    val userId : String ="",
//    val items: Map<String, Long> = emptyMap(), // Map productId to quantity
//    val status : String = "",
//    val address: String = "",
//
//    ///admin
//    val orderId: String = "",
//    val userName: String = "",
//    val userEmail: String = "",
//    val totalAmount: Double = 0.0,
//    val orderDate: Long = System.currentTimeMillis(),
//    val shippingAddress: String = "",
//    val paymentMethod: String = ""
//
//
    val id: String = "",                        // Order ID (document ID)
    val userId: String = "",                    // Customer user ID
    val userName: String = "",                  // Customer name
    val userEmail: String = "",                 // Customer email
    val date: Timestamp = Timestamp.now(), // Order timestamp
    val items: Map<String, Long> = emptyMap(),  // Map: productId -> quantity
    val total: Double = 0.0,                    // Total amount
    val status: String = "ORDERED",             // ORDERED, SHIPPING, DELIVERED, CANCELLED
    val address: String = "",                   // Shipping address
    val paymentMethod: String = "COD"           // Payment method

)
