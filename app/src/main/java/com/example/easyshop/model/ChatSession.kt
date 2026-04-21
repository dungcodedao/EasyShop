package com.example.easyshop.model

import com.google.firebase.Timestamp

data class ChatSession(
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Timestamp = Timestamp.now(),
    val unreadCountByAdmin: Int = 0,
    val unreadCountByUser: Int = 0,
    val userProfileImage: String? = null
)
