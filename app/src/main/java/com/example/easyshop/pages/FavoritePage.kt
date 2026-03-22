package com.example.easyshop.pages

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.easyshop.AppUtil
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.R
import com.example.easyshop.model.ProductModel
import com.example.easyshop.ui.theme.SuccessColor
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun FavoritePage(modifier: Modifier = Modifier) {
    var productsList by remember { mutableStateOf<List<ProductModel>>(emptyList()) }
    val context = LocalContext.current

    // Hàm load lại danh sách — dùng sau khi bỏ yêu thích
    fun reload() {
        val favoriteList = AppUtil.getFavoriteList(context)
        if (favoriteList.isEmpty()) {
            productsList = emptyList()
        } else {
            Firebase.firestore.collection("data").document("stock")
                .collection("products")
                .whereIn("id", favoriteList.toList())
                .get().addOnSuccessListener { result ->
                    productsList = result.documents.mapNotNull { it.toObject(ProductModel::class.java) }
                }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.your_favorites),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${productsList.size} sản phẩm",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        if (productsList.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(items = productsList, key = { it.id }) { product ->
                    FavoriteProductCard(
                        product = product,
                        onRemoveFavorite = {
                            AppUtil.addOrRemoveFromFavorite(context, product.id)
                            productsList = productsList.filter { it.id != product.id }
                        }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.no_favorites_yet),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Hãy thêm sản phẩm bạn thích vào đây!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
private fun FavoriteProductCard(
    product: ProductModel,
    onRemoveFavorite: () -> Unit
) {
    val context = LocalContext.current
    var isFav by remember { mutableStateOf(true) }
    val heartColor by animateColorAsState(
        targetValue = if (isFav) Color(0xFFE53935) else Color.Gray,
        animationSpec = spring(),
        label = "heart_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { GlobalNavigation.navController.navigate("product-details/${product.id}") },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = product.images.firstOrNull(),
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Stock badge
                if (!product.inStock) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = stringResource(R.string.out_of_stock),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Product Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))

                // Price row
                if (product.actualPrice != product.price) {
                    Text(
                        text = AppUtil.formatPrice(product.price),
                        style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = AppUtil.formatPrice(product.actualPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (product.inStock) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                // Thêm vào giỏ
                FilledTonalButton(
                    onClick = {
                        if (product.inStock) AppUtil.addItemToCart(context, product.id)
                        else AppUtil.showToast(context, context.getString(R.string.product_out_of_stock_msg))
                    },
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = stringResource(R.string.add_to_cart), style = MaterialTheme.typography.labelMedium)
                }
            }

            // Nút bỏ yêu thích
            IconButton(
                onClick = {
                    isFav = false
                    onRemoveFavorite()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Bỏ yêu thích",
                    tint = heartColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}