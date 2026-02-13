package com.example.easyshop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import coil.compose.AsyncImage
import com.example.easyshop.AppUtil
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.model.ProductModel

@Composable
fun ProductItemView(modifier: Modifier = Modifier, product: ProductModel) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .width(180.dp)
            .height(280.dp)
            .padding(6.dp)
            .clickable { GlobalNavigation.navController.navigate("product-details/${product.id}") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Image + Badge
            Box(
                modifier = Modifier
                    .height(110.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                AsyncImage(
                    model = product.images.firstOrNull(),
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (product.inStock) 1f else 0.5f),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (product.inStock)
                        Color(0xFF4CAF50).copy(alpha = 0.9f)
                    else
                        MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = if (product.inStock) stringResource(id = R.string.in_stock) else stringResource(id = R.string.out_of_stock),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                text = product.title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.weight(1f))

            // Price + Cart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (product.actualPrice != product.price) {
                        Text(
                            text = AppUtil.formatPrice(product.price),
                            style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = AppUtil.formatPrice(product.actualPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (product.inStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.weight(1f))

                FilledTonalIconButton(
                    onClick = {
                        if (product.inStock) AppUtil.addItemToCart(context, product.id)
                        else AppUtil.showToast(context, context.getString(R.string.product_out_of_stock_msg))
                    },
                    enabled = product.inStock,
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, "Add to cart", Modifier.size(18.dp))
                }
            }
        }
    }
}