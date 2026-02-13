package com.example.easyshop.productdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easyshop.R

@Composable
fun ProductHeader(
    title: String,
    inStock: Boolean,
    rating: Float = 4.5f,
    reviewCount: Int = 120
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { isExpanded = !isExpanded }
        )
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Rating
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    "Rating",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    rating.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(id = R.string.reviews_count, reviewCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))

            // Stock Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (inStock) Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier.size(8.dp)
                            .background(if (inStock) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (inStock) stringResource(id = R.string.in_stock) else stringResource(id = R.string.out_of_stock),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (inStock) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}