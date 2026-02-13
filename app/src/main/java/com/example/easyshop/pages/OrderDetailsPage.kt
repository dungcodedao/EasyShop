package com.example.easyshop.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import coil.compose.AsyncImage
import com.example.easyshop.AppUtil
import com.example.easyshop.GlobalNavigation.navController
import com.example.easyshop.model.OrderModel
import com.example.easyshop.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsPage(modifier: Modifier = Modifier, orderId: String) {
    var order by remember { mutableStateOf<OrderModel?>(null) }
    var products by remember { mutableStateOf<Map<String, ProductModel>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(orderId) {
        val firestore = Firebase.firestore
        firestore.collection("orders").document(orderId).get()
            .addOnSuccessListener { document ->
                val orderData = document.toObject(OrderModel::class.java)
                order = orderData
                if (orderData != null && orderData.items.isNotEmpty()) {
                    firestore.collection("data").document("stock").collection("products")
                        .whereIn("id", orderData.items.keys.toList())
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            products = querySnapshot.documents.mapNotNull { it.toObject(ProductModel::class.java) }.associateBy { it.id }
                            isLoading = false
                        }
                        .addOnFailureListener { isLoading = false }
                } else { isLoading = false }
            }
            .addOnFailureListener { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.order_details), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (order == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(id = R.string.order_not_found)) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Order Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.order_status), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Surface(
                                    color = when (order!!.status) {
                                        "DELIVERED" -> Color(0xFF4CAF50).copy(alpha = 0.12f)
                                        "CANCELLED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = order!!.status,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        color = when (order!!.status) {
                                            "DELIVERED" -> Color(0xFF4CAF50)
                                            "CANCELLED" -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(12.dp))
                            Text("ID: #${order!!.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Date: ${AppUtil.formatData(order!!.date)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Items title
                item {
                    Text(stringResource(id = R.string.ordered_items), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                items(order!!.items.toList()) { (productId, quantity) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        OrderItemRow(products[productId], quantity)
                    }
                }

                // Summary
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(stringResource(id = R.string.summary), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(stringResource(id = R.string.total_amount), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    AppUtil.formatPrice(order!!.total),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(id = R.string.shipping_address), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(order!!.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Cancel
                if (order!!.status == "ORDERED") {
                    item {
                        var showCancelDialog by remember { mutableStateOf(false) }
                        val scope = rememberCoroutineScope()

                        Button(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(id = R.string.cancel_order), fontWeight = FontWeight.Bold)
                        }

                        if (showCancelDialog) {
                            AlertDialog(
                                onDismissRequest = { showCancelDialog = false },
                                title = { Text(stringResource(id = R.string.cancel_order_confirm_title)) },
                                text = { Text(stringResource(id = R.string.cancel_order_confirm_msg)) },
                                shape = RoundedCornerShape(24.dp),
                                confirmButton = {
                                    TextButton(onClick = {
                                        scope.launch {
                                            Firebase.firestore.collection("orders").document(orderId)
                                                .update("status", "CANCELLED")
                                                .addOnSuccessListener { order = order?.copy(status = "CANCELLED"); showCancelDialog = false }
                                        }
                                    }) { Text(stringResource(id = R.string.yes_cancel), color = MaterialTheme.colorScheme.error) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCancelDialog = false }) { Text(stringResource(id = R.string.no_keep_order)) }
                                }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun OrderItemRow(product: ProductModel?, quantity: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (product != null) {
            AsyncImage(
                model = product.images.firstOrNull(),
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text("${stringResource(id = R.string.quantity)}: $quantity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(AppUtil.formatPrice(product.actualPrice), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        } else {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
            Spacer(Modifier.width(16.dp))
            Text(stringResource(id = R.string.product_info_not_available), modifier = Modifier.weight(1f))
            Text("x$quantity")
        }
    }
}
