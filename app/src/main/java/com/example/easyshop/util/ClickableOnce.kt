package com.example.easyshop.util

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modifier để chỉ xử lý 1 click trong khoảng debounce time,
 * tránh double-click gây navigate nhiều lần.
 */
fun Modifier.clickableOnce(
    debounceMs: Long = 600L,
    onClick: () -> Unit
): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    pointerInput(Unit) {
        awaitPointerEventScope {
            // không dùng, chỉ là placeholder
        }
    }
    // Dùng clickable trên nền androidx
    this.then(
        Modifier.pointerInput(onClick) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val now = System.currentTimeMillis()
                    if (event.changes.any { it.pressed } && now - lastClickTime > debounceMs) {
                        lastClickTime = now
                        onClick()
                    }
                }
            }
        }
    )
}

/**
 * Trả về lambda đã được debounce để dùng với onClick = rememberDebouncedClick { ... }
 */
@Composable
fun rememberDebouncedClick(
    debounceMs: Long = 600L,
    enabled: Boolean = true,
    action: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    return {
        if (enabled) {
            val now = System.currentTimeMillis()
            if (now - lastClickTime > debounceMs) {
                lastClickTime = now
                action()
            }
        }
    }
}
