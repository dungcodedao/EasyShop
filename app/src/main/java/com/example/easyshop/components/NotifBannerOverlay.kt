package com.example.easyshop.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest

data class NotifBannerEvent(
    val title: String,
    val body: String,
    val type: String = "SYSTEM"
)

object NotifBannerController {
    private val _events = MutableSharedFlow<NotifBannerEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun show(title: String, body: String, type: String = "SYSTEM") {
        _events.tryEmit(NotifBannerEvent(title, body, type))
    }
}

@Composable
fun NotifBannerOverlay() {
    var currentEvent by remember { mutableStateOf<NotifBannerEvent?>(null) }
    var visible by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        NotifBannerController.events.collectLatest { event ->
            if (visible) {
                visible = false
                delay(150)
            }
            currentEvent = event
            progress = 1f
            visible = true
            
            // Thanh chuyển động lùi của progress bar (Giảm xuống 1 giây theo yêu cầu)
            val duration = 1000L
            val steps = 50
            val stepTime = duration / steps
            for (i in steps downTo 0) {
                delay(stepTime)
                progress = i / 100f
            }
            
            visible = false
            delay(300)
            currentEvent = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.statusBarsPadding(),
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            currentEvent?.let { event ->
                // ✅ Định nghĩa màu sắc viền dựa trên trạng thái (Dùng màu chuẩn từ Color.kt)
                val statusColor = when (event.type) {
                    "SUCCESS" -> com.example.easyshop.ui.theme.SuccessColor
                    "ERROR" -> com.example.easyshop.ui.theme.ErrorColor
                    "WARNING" -> com.example.easyshop.ui.theme.WarningColor
                    else -> com.example.easyshop.ui.theme.PrimaryColor
                }

                val icon = when (event.type) {
                    "SUCCESS" -> {
                        if (event.title.contains("thích", ignoreCase = true)) Icons.Default.Favorite 
                        else Icons.Default.CheckCircle
                    }
                    "ERROR" -> Icons.Default.Error
                    "WARNING" -> Icons.Default.Warning
                    else -> Icons.Default.Info
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .padding(top = 16.dp)
                        .graphicsLayer {
                            shadowElevation = 16.dp.toPx()
                            shape = RoundedCornerShape(24.dp)
                            clip = true
                        }
                        .animateContentSize(),
                    shadowElevation = 12.dp
                ) {
                    // ✅ Box này phải có height(IntrinsicSize.Min) để thanh màu bên trong không bị tràn
                    Box(modifier = Modifier.height(IntrinsicSize.Min)) {
                        // ✅ Điểm nhấn viền màu ở cạnh trái
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(6.dp)
                                .background(statusColor)
                        )

                        Row(
                            modifier = Modifier.padding(start = 22.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(32.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.title,
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.5.sp
                                )
                                if (event.body.isNotBlank()) {
                                    Text(
                                        text = event.body,
                                        color = Color(0xFF64748B),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(progress)
                                .height(3.dp)
                                .background(statusColor.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }
}
