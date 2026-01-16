package com.example.easyshop.model

data class  ProductModel(
    var id : String = "",
    val title : String = "",
    val description : String = "",
    val price : String = "",
    val inStock: Boolean = false,
    val actualPrice : String = "",
    val category : String = "",
    val images : List<String> = emptyList(),
    val otherDetails : Map<String, String> = emptyMap() ,

)


