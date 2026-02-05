package com.example.easyshop.productdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import androidx.compose.ui.unit.sp

@Composable
fun ProductHeader(
    title: String,
    inStock: Boolean,
    rating: Float = 4.5f,  // ← Thêm parameter
    reviewCount: Int = 120  // ← Thêm parameter
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        // Title với expand/collapse
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = Color.Black,
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { isExpanded = !isExpanded }
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Rating
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    tint = Color(0xFFFFC107), // Keep star color
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = rating.toString(),  // ← Dùng parameter
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(id = R.string.reviews_count, reviewCount),  // ← Dùng parameter
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Stock Status Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (inStock) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (inStock) Color(0xFF4CAF50) else Color(0xFFF44336),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = if (inStock) stringResource(id = R.string.in_stock) else stringResource(id = R.string.out_of_stock),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (inStock) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }
    }
}