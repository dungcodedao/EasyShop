package com.example.easyshop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.easyshop.AppUtil
import com.example.easyshop.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun CartItemView(modifier: Modifier = Modifier, productId: String, qty: Long) {
    var product by remember { mutableStateOf(ProductModel()) }

    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("data")
            .document("stock").collection("products")
            .document(productId).get().addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result.toObject(ProductModel::class.java)?.let { p -> product = p }
                }
            }
    }

    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            AsyncImage(
                model = product.images.firstOrNull(),
                contentDescription = product.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = AppUtil.formatPrice(product.actualPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                // Qty controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = { AppUtil.removeItemFromCart(context, productId) },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("−", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "$qty",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalIconButton(
                        onClick = { AppUtil.addItemToCart(context, productId) },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // Delete
            IconButton(onClick = { AppUtil.removeItemFromCart(context, productId, removeAll = true) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}