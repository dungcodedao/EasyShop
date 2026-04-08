package com.example.easyshop.model

import com.google.firebase.Timestamp

data class ReviewModel(
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
