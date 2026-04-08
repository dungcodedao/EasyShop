package com.example.easyshop.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.util.ConnectivityObserver

@Composable
fun NetworkStatusBanner(status: ConnectivityObserver.Status) {
    AnimatedVisibility(
        visible = status != ConnectivityObserver.Status.Available,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (status == ConnectivityObserver.Status.Lost) Color(0xFFEF5350) else Color(0xFFFF9800))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (status == ConnectivityObserver.Status.Lost) "Mất kết nối Internet" else "Đang kết nối lại...",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}
