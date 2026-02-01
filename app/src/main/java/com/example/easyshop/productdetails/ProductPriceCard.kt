package com.example.easyshop.productdetails


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProductPriceCard(price: String, actualPrice: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$$price",
                    fontSize = 16.sp,
                    textDecoration = TextDecoration.LineThrough,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$$actualPrice",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1976D2),
                    letterSpacing = (-0.5).sp
                )
            }

            // Discount Badge
            val priceValue = price.toDoubleOrNull() ?: 0.0
            val actualPriceValue = actualPrice.toDoubleOrNull() ?: 0.0
            if (priceValue != actualPriceValue && priceValue > 0) {
                val discount = ((priceValue - actualPriceValue) / priceValue * 100).toInt()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "$discount%",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "OFF",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}