package com.example.easyshop.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.easyshop.AppUtil
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.model.OrderModel

@Composable
fun OrderView(orderItems: OrderModel, modifier: Modifier = Modifier) {
    val statusColor = when (orderItems.status) {
        "ORDERED" -> MaterialTheme.colorScheme.primary
        "SHIPPING" -> Color(0xFFFF9800)
        "DELIVERED" -> Color(0xFF4CAF50)
        "CANCELLED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { GlobalNavigation.navController.navigate("order-details/${orderItems.id}") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "#${orderItems.id}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = orderItems.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    AppUtil.formatData(orderItems.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${orderItems.items.size} items",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}