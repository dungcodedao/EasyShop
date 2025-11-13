package com.example.easyshop.model

data class UserModel(
    val name: String = "",
    val email: String = "",
    val uid: String = "",
    val carrItems : Map<String, Long> = emptyMap()
)