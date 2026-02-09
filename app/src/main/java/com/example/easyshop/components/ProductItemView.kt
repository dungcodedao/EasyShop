package com.example.easyshop.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import androidx.compose.ui.unit.sp
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
            .padding(8.dp)
            .clickable {
                GlobalNavigation.navController.navigate("product-details/" + product.id)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // ========== PRODUCT IMAGE WITH STOCK BADGE ==========
            Box(
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    model = product.images.firstOrNull(),
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (product.inStock) 1f else 0.6f) // Làm mờ ảnh nếu hết hàng
                )

                // Stock Status Badge (góc trên phải)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = if (product.inStock)
                        Color(0xFF4CAF50)  // Xanh lá nếu còn hàng
                    else
                        Color(0xFFE53935)   // Đỏ nếu hết hàng
                ) {
                    Text(
                        text = if (product.inStock) stringResource(id = R.string.in_stock) else stringResource(id = R.string.out_of_stock),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ========== PRODUCT TITLE ==========
            Text(
                text = product.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // ========== PRICE & ADD TO CART ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price Column
                Column {
                    Text(
                        text = AppUtil.formatPrice(product.price),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        style = TextStyle(textDecoration = TextDecoration.LineThrough)
                    )
                    Text(
                        text = AppUtil.formatPrice(product.actualPrice),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (product.inStock)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Add to Cart Button
                IconButton(
                    onClick = {
                        if (product.inStock) {
                            AppUtil.addItemToCart(context, product.id)
                        } else {
                            AppUtil.showToast(context, context.getString(R.string.product_out_of_stock_msg))
                        }
                    },
                    enabled = product.inStock
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Add to cart",
                        tint = if (product.inStock)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray
                    )
                }
            }
        }
    }
}