package com.example.easyshop.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Order Model
data class OrderModel(
    val id: String = "",
    val userId: String = "",
    val address: String = "",
    val date: Long = 0,
    val items: Map<String, Int> = emptyMap(),
    val status: String = "ORDERED",
    val total: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var orders by remember { mutableStateOf<List<OrderModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStatus by remember { mutableStateOf("ALL") }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val firestore = Firebase.firestore

    // Load orders from Firebase
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        try {
            val result = firestore.collection("orders").get().await()
            orders = result.documents.mapNotNull { doc ->
                try {
                    doc.toObject(OrderModel::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }.sortedByDescending { it.date }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders Management") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "ORDERED", "SHIPPING", "DELIVERED", "CANCELLED").forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status, fontSize = 12.sp) },
                        leadingIcon = if (selectedStatus == status) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                orders.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No orders yet",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    val filteredOrders = if (selectedStatus == "ALL") {
                        orders
                    } else {
                        orders.filter { it.status == selectedStatus }
                    }

                    if (filteredOrders.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No orders with status: $selectedStatus",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredOrders) { order ->
                                OrderCard(
                                    order = order,
                                    onStatusChange = { newStatus ->
                                        scope.launch {
                                            try {
                                                firestore.collection("orders")
                                                    .document(order.id)
                                                    .update("status", newStatus)
                                                    .await()
                                                refreshTrigger++
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: OrderModel,
    onStatusChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.id.take(8).uppercase()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = try {
                            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                .format(Date(order.date))
                        } catch (e: Exception) {
                            "N/A"
                        },
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                // Status Badge
                Surface(
                    color = when (order.status) {
                        "ORDERED" -> Color(0xFFFF9800)
                        "SHIPPING" -> Color(0xFF2196F3)
                        "DELIVERED" -> Color(0xFF4CAF50)
                        "CANCELLED" -> Color(0xFFF44336)
                        else -> Color.Gray
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Order Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${order.items.size} items",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "$${"%.2f".format(order.total)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            // Expanded Content
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                Spacer(Modifier.height(12.dp))

                // User ID
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Customer ID",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = order.userId.take(12) + "...",
                            fontSize = 14.sp,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Delivery Address
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Delivery Address",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = order.address,
                            fontSize = 14.sp,
                            color = Color(0xFF1A1A1A),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Update Status Button
                Button(
                    onClick = { showStatusDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Update Order Status")
                }
            }
        }
    }

    // Status Update Dialog
    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Update Order Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Select new status for order #${order.id.take(8).uppercase()}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))

                    listOf("ORDERED", "SHIPPING", "DELIVERED", "CANCELLED").forEach { status ->
                        Button(
                            onClick = {
                                onStatusChange(status)
                                showStatusDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (status) {
                                    "ORDERED" -> Color(0xFFFF9800)
                                    "SHIPPING" -> Color(0xFF2196F3)
                                    "DELIVERED" -> Color(0xFF4CAF50)
                                    "CANCELLED" -> Color(0xFFF44336)
                                    else -> Color.Gray
                                }
                            ),
                            enabled = status != order.status
                        ) {
                            Text(status)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}