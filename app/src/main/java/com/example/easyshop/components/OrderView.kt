package com.example.easyshop.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.AppUtil
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.model.OrderModel

@Composable
fun OrderView(orderItems: OrderModel, modifier: Modifier = Modifier) {

    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                GlobalNavigation.navController.navigate("order-details/${orderItems.id}")
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Order ID: " + orderItems.id,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                AppUtil.formatData(orderItems.date),
                fontSize = 14.sp
            )

            val statusColor = when (orderItems.status) {
                "ORDERED" -> Color(0xFF2196F3) // Blue
                "SHIPPING" -> Color(0xFFFF9800) // Orange
                "DELIVERED" -> Color(0xFF4CAF50) // Green
                "CANCELLED" -> Color(0xFFF44336) // Red
                else -> Color.Gray
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = orderItems.status,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                orderItems.items.size.toString() + " items",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}