package com.example.easyshop.model

data class PromoCodeModel (
    val code: String = "",
    val type: String = "percentage", // "percentage" hoặc "fixed"
    val value: Double = 0.0,
    val description: String = "",
    val minOrder: Double = 0.0,
    val maxDiscount: Double = 0.0,
    val expiryDate: Long = 0L,
    val active: Boolean = true,
    val usageLimit: Int = -1, // -1 = không giới hạn
    val usedCount: Int = 0
)

/**
 * Extension functions để validate
 */
fun PromoCodeModel.isExpired(): Boolean {
    return expiryDate > 0 && System.currentTimeMillis() > expiryDate
}

fun PromoCodeModel.isUsageLimitReached(): Boolean {
    return usageLimit > 0 && usedCount >= usageLimit
}

fun PromoCodeModel.meetsMinOrder(subtotal: Float): Boolean {
    return subtotal >= minOrder
}

fun PromoCodeModel.isValid(subtotal: Float): Boolean {
    return active && !isExpired() && !isUsageLimitReached() && meetsMinOrder(subtotal)
}