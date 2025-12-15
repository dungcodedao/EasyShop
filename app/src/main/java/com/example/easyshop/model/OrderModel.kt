package com.example.easyshop.model

import com.google.firebase.Timestamp

data class OrderModel(
    var id : String = "",
    val date : Timestamp = Timestamp.now(),
    val userId : String ="",
    val items : Map<String, Long> = mapOf(),
    val status : String = "",
    val address: String = ""

)
