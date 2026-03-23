package com.example.easyshop.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private fun snackbarStyle(type: SnackbarType): Pair<Color, ImageVector> = when (type) {
    SnackbarType.SUCCESS -> Pair(Color(0xFF4CAF50), Icons.Rounded.CheckCircle)
    SnackbarType.ERROR -> Pair(Color(0xFFEF5350), Icons.Rounded.Cancel)
    SnackbarType.WARNING -> Pair(Color(0xFFFF9800), Icons.Rounded.FavoriteBorder) // Dùng tim cho bỏ yêu thích hoặc warning
    SnackbarType.INFO -> Pair(Color(0xFF7C4DFF), Icons.Rounded.Info)
}

@Composable
fun AppSnackbarHost() {
    var currentEvent by remember { mutableStateOf<SnackbarEvent?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SnackbarController.events.collectLatest { event ->
            if (visible) {
                visible = false
                delay(200)
            }
            currentEvent = event
            visible = true
            delay(2500) // Tự ẩn sau 2.5s
            visible = false
            delay(300)
            currentEvent = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(250)
            ) + fadeOut()
        ) {
            currentEvent?.let { event ->
                TopNotificationCard(event)
            }
        }
    }
}

@Composable
private fun TopNotificationCard(event: SnackbarEvent) {
    val (primaryColor, icon) = snackbarStyle(event.type)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Thanh màu bên trái
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(primaryColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon tròn
                Surface(
                    shape = CircleShape,
                    color = primaryColor,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Nội dung text
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = event.message,
                        color = Color(0xFF1F2937),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    if (event.subtext != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = event.subtext,
                            color = Color(0xFF6B7280),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
