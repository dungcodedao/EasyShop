package com.example.easyshop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vẽ đường răng cưa cho tờ hóa đơn POS
 */
@Composable
fun SawtoothEdge(
    modifier: Modifier = Modifier,
    isBottom: Boolean = false,
    color: Color = Color.White,
    toothWidth: Dp = 10.dp,
    toothHeight: Dp = 6.dp
) {
    Canvas(modifier = modifier.fillMaxWidth().height(toothHeight)) {
        val width = size.width
        val height = size.height
        val toothW = toothWidth.toPx()
        val toothH = toothHeight.toPx()
        
        val path = Path().apply {
            if (isBottom) {
                // Răng cưa ở dưới (hướng lên)
                moveTo(0f, 0f)
                var x = 0f
                while (x < width) {
                    lineTo(x + toothW / 2, toothH)
                    lineTo(x + toothW, 0f)
                    x += toothW
                }
                lineTo(width, 0f)
                close()
            } else {
                // Răng cưa ở trên (hướng xuống)
                moveTo(0f, toothH)
                var x = 0f
                while (x < width) {
                    lineTo(x + toothW / 2, 0f)
                    lineTo(x + toothW, toothH)
                    x += toothW
                }
                lineTo(width, toothH)
                close()
            }
        }
        drawPath(path, color)
    }
}

/**
 * Vẽ đường kẻ nét đứt (Dashed Line) cho hóa đơn
 */
@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.LightGray.copy(alpha = 0.5f),
    thickness: Dp = 1.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp
) {
    Canvas(modifier = modifier.fillMaxWidth().height(thickness)) {
        val dashLen = dashLength.toPx()
        val gapLen = gapLength.toPx()
        
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLen, gapLen), 0f),
            strokeWidth = thickness.toPx()
        )
    }
}
