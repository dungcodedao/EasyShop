package com.example.easyshop.model

import com.google.firebase.firestore.PropertyName

data class ProductModel(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val price: String = "",
    @get:PropertyName("isStock") @set:PropertyName("isStock")
    var inStock: Boolean = true,
    val stockCount: Int = 0,
    val actualPrice: String = "",
    // Firestore may store otherPrice as String or Map<String,Any>, use Any? to avoid crash
    val otherPrice: Any? = null,
    val category: String = "",
    val images: List<String> = emptyList(),
    val otherDetails: Map<String, String> = emptyMap(),
    val rating: Float = 0f,
    val reviewCount: Int = 0
) {
    /**
     * Lấy otherPrice dưới dạng String an toàn.
     * - Nếu Firestore lưu Map → trả về ""
     * - Nếu Firestore lưu String → trả về chính string đó
     */
    val otherPriceString: String
        get() = otherPrice as? String ?: ""
}
