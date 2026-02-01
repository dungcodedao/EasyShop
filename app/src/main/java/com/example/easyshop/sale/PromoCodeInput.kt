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
    onDiscountApplied: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var promoCode by remember { mutableStateOf("") }
    var appliedPromo by remember { mutableStateOf<PromoCodeModel?>(null) }
    var isPromoError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Calculate discount based on applied promo
    val discountAmount: Float = appliedPromo?.let { promo ->
        when (promo.type) {
            "percentage" -> {
                val discount = subtotal * (promo.value.toFloat() / 100f)
                // Apply max discount limit if set
                if (promo.maxDiscount > 0 && discount > promo.maxDiscount.toFloat()) {
                    promo.maxDiscount.toFloat()
                } else {
                    discount
                }
            }
            "fixed" -> promo.value.toFloat()
            else -> 0f
        }
    } ?: 0f

    // Update parent component when discount changes
    LaunchedEffect(discountAmount) {
        onDiscountApplied(discountAmount)
    }

    Column(modifier = modifier) {
        // Promo Code Input Field
        OutlinedTextField(
            value = promoCode,
            onValueChange = {
                promoCode = it.uppercase()
                isPromoError = false
            },
            label = { Text("Promo Code") },
            placeholder = { Text("Enter code") },
            modifier = Modifier.fillMaxWidth(),
            isError = isPromoError,
            trailingIcon = {
                if (appliedPromo == null) {
                    TextButton(
                        onClick = {
                            if (promoCode.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Please enter a promo code",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@TextButton
                            }

                            isLoading = true
                            validatePromoCode(promoCode, subtotal) { promo ->
                                isLoading = false
                                if (promo != null) {
                                    appliedPromo = promo
                                    isPromoError = false
                                    Toast.makeText(
                                        context,
                                        "Promo code applied!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    isPromoError = true
                                    Toast.makeText(
                                        context,
                                        "Invalid or expired promo code",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
                            Text("Apply")
                        }
                    }
                } else {
                    IconButton(
                        onClick = {
                            appliedPromo = null
                            promoCode = ""
                        }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
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
                "Invalid or expired promo code",
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
                        "-$${"%.2f".format(discountAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// Validate Promo Code from Firebase
fun validatePromoCode(
    code: String,
    subtotal: Float,
    callback: (PromoCodeModel?) -> Unit
) {
    Firebase.firestore
        .collection("promoCodes")
        .document(code)
        .get()
        .addOnSuccessListener { doc ->
            if (!doc.exists()) {
                callback(null)
                return@addOnSuccessListener
            }

            val promo = doc.toObject(PromoCodeModel::class.java)
            if (promo == null) {
                callback(null)
                return@addOnSuccessListener
            }

            // Validation checks
            val now = System.currentTimeMillis()

            when {
                !promo.active -> callback(null) // Promo not active
                promo.expiryDate > 0 && now > promo.expiryDate -> callback(null) // Expired
                subtotal < promo.minOrder -> callback(null) // Minimum order not met
                promo.usageLimit > 0 && promo.usedCount >= promo.usageLimit -> callback(null) // Usage limit reached
                else -> callback(promo) // Valid
            }
        }
        .addOnFailureListener {
            callback(null)
        }
}