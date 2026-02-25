package com.example.easyshop.ai.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

// Dùng 'var' để Firestore có thể tạo setter đúng chuẩn
// Dùng Timestamp? cho trường timestamp để khớp với kiểu dữ liệu Firestore
data class ChatMessage(
    var id: String = "",
    var content: String = "",
    @get:PropertyName("isUser") @set:PropertyName("isUser") var isUser: Boolean = true,
    var timestamp: Timestamp? = null
)
