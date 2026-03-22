package com.example.easyshop.sale

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.model.PromoCodeModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun PromoCodeInput(
    subtotal: Float,
    onDiscountApplied: (Float, String) -> Unit, // Bây giờ trả về cả số tiền và mã code
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var promoCode by remember { mutableStateOf("") }
    var appliedPromo by remember { mutableStateOf<PromoCodeModel?>(null) }
    var isPromoError by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Calculate discount based on applied promo
    val discountAmount: Float = appliedPromo?.let { promo ->
        val calculation = when (promo.type) {
            "percentage" -> {
                val disc = subtotal * (promo.value.toFloat() / 100f)
                // Apply max discount limit if set
                if (promo.maxDiscount > 0 && disc > promo.maxDiscount.toFloat()) {
                    promo.maxDiscount.toFloat()
                } else {
                    disc
                }
            }
            "fixed" -> promo.value.toFloat()
            else -> 0f
        }
        // Cap discount to subtotal
        if (calculation > subtotal) subtotal else calculation
    } ?: 0f

    // Update parent component when discount or promo changes
    LaunchedEffect(discountAmount, appliedPromo) {
        onDiscountApplied(discountAmount, appliedPromo?.code ?: "")
    }

    Column(modifier = modifier) {
        // Promo Code Input Field
        OutlinedTextField(
            value = promoCode,
            onValueChange = {
                promoCode = it.uppercase()
                isPromoError = false
            },
            label = { Text("Mã giảm giá") },
            placeholder = { Text("Nhập mã tại đây") },
            modifier = Modifier.fillMaxWidth(),
            isError = isPromoError,
            trailingIcon = {
                if (appliedPromo == null) {
                    TextButton(
                        onClick = {
                            val trimmedCode = promoCode.trim()
                            if (trimmedCode.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Vui lòng nhập mã giảm giá",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@TextButton
                            }

                            isLoading = true
                            validatePromoCode(trimmedCode, subtotal) { result ->
                                isLoading = false
                                when (result) {
                                    is PromoResult.Success -> {
                                        appliedPromo = result.promo
                                        isPromoError = false
                                        errorText = ""
                                        Toast.makeText(
                                            context,
                                            "Áp dụng mã thành công!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    is PromoResult.Error -> {
                                        isPromoError = true
                                        errorText = result.message
                                        Toast.makeText(
                                            context,
                                            result.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Áp dụng")
                        }
                    }
                } else {
                    IconButton(
                        onClick = {
                            appliedPromo = null
                            promoCode = ""
                            errorText = ""
                        }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Xóa mã",
                            tint = Color.Red
                        )
                    }
                }
            },
            singleLine = true,
            enabled = appliedPromo == null && !isLoading
        )

        // Error Message
        if (isPromoError) {
            Text(
                errorText,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        // Applied Promo Success Card
        appliedPromo?.let { promo ->
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "✓",
                        color = Color(0xFF4CAF50),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            promo.code,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            promo.description,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        "-${com.example.easyshop.AppUtil.formatPrice(discountAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// Sealed class for better result handling
sealed class PromoResult {
    data class Success(val promo: PromoCodeModel) : PromoResult()
    data class Error(val message: String) : PromoResult()
}

// Validate Promo Code from Firebase
fun validatePromoCode(
    code: String,
    subtotal: Float,
    callback: (PromoResult) -> Unit
) {
    Firebase.firestore
        .collection("promoCodes")
        .document(code)
        .get()
        .addOnSuccessListener { doc ->
            if (!doc.exists()) {
                callback(PromoResult.Error("Mã giảm giá không tồn tại"))
                return@addOnSuccessListener
            }

            val promo = doc.toObject(PromoCodeModel::class.java)
            if (promo == null) {
                callback(PromoResult.Error("Lỗi dữ liệu mã giảm giá"))
                return@addOnSuccessListener
            }

            // Validation checks
            val now = System.currentTimeMillis()

            when {
                !promo.active -> callback(PromoResult.Error("Mã giảm giá hiện không khả dụng"))
                promo.expiryDate > 0 && now > promo.expiryDate -> callback(PromoResult.Error("Mã giảm giá đã hết hạn"))
                subtotal < promo.minOrder -> callback(PromoResult.Error("Đơn hàng phải tối thiểu đ${promo.minOrder.toLong()} để dùng mã này"))
                promo.usageLimit > 0 && promo.usedCount >= promo.usageLimit -> callback(PromoResult.Error("Mã giảm giá đã hết lượt sử dụng"))
                else -> callback(PromoResult.Success(promo))
            }
        }
        .addOnFailureListener {
            callback(PromoResult.Error("Lỗi kết nối máy chủ"))
        }
}