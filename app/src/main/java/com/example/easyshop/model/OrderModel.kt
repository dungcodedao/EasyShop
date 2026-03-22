package com.example.easyshop.model

import com.google.firebase.Timestamp

data class OrderModel(
    val id: String = "",                        // Order ID (document ID)
    val userId: String = "",                    // Customer user ID
    val userName: String = "",                  // Customer name
    val userEmail: String = "",                 // Customer email
    val userPhone: String = "",                 // Customer phone number
    val date: Timestamp = Timestamp.now(),      // Order timestamp
    val items: Map<String, Long> = emptyMap(),  // Map: productId -> quantity
    val subtotal: Double = 0.0,                 // Amount before discount
    val discount: Double = 0.0,                 // Discount amount 
    val promoCode: String = "",                 // Applied promo code
    val total: Double = 0.0,                    // Final total amount
    val status: String = "ORDERED",             // ORDERED, SHIPPING, DELIVERED, CANCELLED
    val address: String = "",                   // Shipping address
    val paymentMethod: String = "COD"           // Payment method
)
