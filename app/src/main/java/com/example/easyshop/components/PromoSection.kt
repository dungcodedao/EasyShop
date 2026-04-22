package com.example.easyshop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.AppUtil
import com.example.easyshop.model.PromoCodeModel

@Composable
fun PromoSection(
    promos: List<PromoCodeModel>,
    onCollect: (String) -> Unit
) {
    if (promos.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "🎁 Ưu Đãi Dành Cho Bạn",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 14.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(promos) { promo ->
                    PromoCard(
                        promo = promo,
                        onCollect = { onCollect(promo.code) }
                    )
                }
            }
        }
    }
}

@Composable
fun PromoCard(promo: PromoCodeModel, onCollect: () -> Unit) {
    val primaryColor = Color(0xFF4F46E5)
    val secondaryColor = Color(0xFF818CF8)

    Card(
        modifier = Modifier
            .width(280.dp)
            .height(115.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left part (Discount Info) - Gradient background
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(90.dp)
                    .background(
                        Brush.verticalGradient(listOf(primaryColor, secondaryColor))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (promo.type == "percentage") "${promo.value.toInt()}%" else "OFF",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                    Text(
                        text = "GIẢM GIÁ",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right part (Details & Action)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = promo.code,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = primaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = promo.description,
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        maxLines = 2,
                        minLines = 2,
                        lineHeight = 14.sp,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Đơn từ ${AppUtil.formatPrice(promo.minOrder.toFloat())}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = onCollect,
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text(
                            text = "Lưu",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
