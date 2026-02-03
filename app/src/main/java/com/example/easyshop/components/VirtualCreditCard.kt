package com.example.easyshop.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VirtualCreditCard(
    number: String,
    name: String,
    expiry: String,
    cvv: String,
    isVisa: Boolean = true
) {
    val cardBgColor by animateColorAsState(
        targetValue = if (isVisa) Color(0xFF1A1A1A) else Color(0xFF303F9F),
        animationSpec = tween(500), label = ""
    )

    val gradient = if (isVisa) {
        Brush.linearGradient(listOf(Color(0xFF2C3E50), Color(0xFF000000)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF3F51B5), Color(0xFF1A237E)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Card Brand & Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sim Chip
                Box(
                    modifier = Modifier
                        .size(45.dp, 35.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFD700).copy(alpha = 0.8f))
                )

                Text(
                    text = if (isVisa) "VISA" else "MASTERCARD",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Card Number
            Text(
                text = number.ifEmpty { "XXXX XXXX XXXX XXXX" },
                color = Color.White,
                fontSize = 22.sp,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Card Holder & Expiry
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CARD HOLDER",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = name.uppercase().ifEmpty { "FULL NAME" },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "EXPIRES",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = expiry.ifEmpty { "MM/YY" },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
