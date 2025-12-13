package com.example.easyshop.components

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
import com.example.easyshop.model.OrderModel

@Composable
fun OrderView(orderItems: OrderModel,modifier: Modifier = Modifier){

    Card(
        modifier = modifier.padding(8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Order ID: " + orderItems.id,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(AppUtil.formatData(orderItems.date),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(orderItems.status,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5722),
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(orderItems.items.size.toString()+ " items",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )


        }
    }
}