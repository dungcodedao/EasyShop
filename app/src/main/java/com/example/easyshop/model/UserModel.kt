package com.example.easyshop.model

data class UserModel(
    val name: String = "",
    val email: String = "",
    val uid: String = "",
    val phone: String = "",      // Số điện thoại nhận hàng
    val cartItems: Map<String, Long> = emptyMap(),
    val address: String = "",
    val role: String = "user"
)