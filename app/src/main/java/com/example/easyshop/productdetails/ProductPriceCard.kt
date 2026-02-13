package com.example.easyshop.productdetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.easyshop.R
import com.example.easyshop.AppUtil

@Composable
fun ProductPriceCard(price: String, actualPrice: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (price != actualPrice) {
                    Text(
                        text = AppUtil.formatPrice(price),
                        style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = AppUtil.formatPrice(actualPrice),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Discount Badge
            val priceValue = price.toDoubleOrNull() ?: 0.0
            val actualPriceValue = actualPrice.toDoubleOrNull() ?: 0.0
            if (priceValue != actualPriceValue && priceValue > 0) {
                val discount = ((priceValue - actualPriceValue) / priceValue * 100).toInt()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "$discount%",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(id = R.string.off_label),
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}