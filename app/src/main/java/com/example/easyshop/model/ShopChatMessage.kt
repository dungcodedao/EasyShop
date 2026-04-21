package com.example.easyshop.model

import com.google.firebase.Timestamp

data class ShopChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    val senderName: String = "" // Optional: for display purposes
)
