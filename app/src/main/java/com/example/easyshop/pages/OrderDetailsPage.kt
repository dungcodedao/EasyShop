package com.example.easyshop.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.sp
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
                    // Fetch product details for all items in the order
                    val productIds = orderData.items.keys.toList()
                    firestore.collection("data").document("stock").collection("products")
                        .whereIn("id", productIds)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            val productMap = querySnapshot.documents.mapNotNull { doc ->
                                doc.toObject(ProductModel::class.java)
                            }.associateBy { it.id }
                            products = productMap
                            isLoading = false
                        }
                        .addOnFailureListener { isLoading = false }
                } else {
                    isLoading = false
                }
            }
            .addOnFailureListener { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.order_details), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8F9FA) // Màu nền nhẹ giúp các thẻ nổi bật hơn
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (order == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(id = R.string.order_not_found))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp), // Padding đồng nhất cho toàn bộ nội dung
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Order Info Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(id = R.string.order_status), fontSize = 14.sp, color = Color.Gray)
                                Surface(
                                    color = when(order!!.status) {
                                        "DELIVERED" -> Color(0xFFE8F5E9)
                                        "CANCELLED" -> Color(0xFFFFEBEE)
                                        else -> Color(0xFFE3F2FD)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = order!!.status,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = when(order!!.status) {
                                            "DELIVERED" -> Color(0xFF4CAF50)
                                            "CANCELLED" -> Color(0xFFF44336)
                                            else -> Color(0xFF2196F3)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))

                            Divider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(Modifier.height(12.dp))
                            Text("ID: #${order!!.id}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Date: ${AppUtil.formatData(order!!.date)}", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }

                // Items list Section
                item {
                    Text(
                        text = stringResource(id = R.string.ordered_items), 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 17.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(order!!.items.toList()) { (productId, quantity) ->
                    val product = products[productId]
                    OrderItemRow(product, quantity)
                }

                // Summary and Shipping Section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(stringResource(id = R.string.summary), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(id = R.string.total_amount), fontSize = 15.sp)
                                Text(AppUtil.formatPrice(order!!.total), 
                                    fontWeight = FontWeight.ExtraBold, 
                                    fontSize = 18.sp,
                                    color = Color(0xFF1976D2)
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            Divider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            
                            Text(stringResource(id = R.string.shipping_address), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = order!!.address, 
                                fontSize = 14.sp, 
                                color = Color.Gray,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                
                // Cancel Order Section (Only for ORDERED status)
                if (order!!.status == "ORDERED") {
                    item {
                        var showCancelDialog by remember { mutableStateOf(false) }
                        val scope = rememberCoroutineScope()

                        Button(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFEBEE),
                                contentColor = Color(0xFFD32F2F)
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
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                Firebase.firestore.collection("orders")
                                                    .document(orderId)
                                                    .update("status", "CANCELLED")
                                                    .addOnSuccessListener {
                                                        order = order?.copy(status = "CANCELLED")
                                                        showCancelDialog = false
                                                    }
                                            }
                                        }
                                    ) {
                                        Text(stringResource(id = R.string.yes_cancel), color = Color.Red)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCancelDialog = false }) {
                                        Text(stringResource(id = R.string.no_keep_order))
                                    }
                                }
                            )
                        }
                    }
                }

                // Bottom spacing for better scrolling experience
                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun OrderItemRow(product: ProductModel?, quantity: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (product != null) {
            AsyncImage(
                model = product.images.firstOrNull(),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text("${stringResource(id = R.string.quantity)}: $quantity", fontSize = 12.sp, color = Color.Gray)
            }
            Text(AppUtil.formatPrice(product.actualPrice), fontWeight = FontWeight.Bold)
        } else {
            // Placeholder for missing product info
            Box(Modifier.size(60.dp).background(Color.LightGray))
            Spacer(Modifier.width(16.dp))
            Text(stringResource(id = R.string.product_info_not_available), modifier = Modifier.weight(1f))
            Text("x$quantity")
        }
    }
}
