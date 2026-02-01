package com.example.easyshop.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.easyshop.model.OrderModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersManagementScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var orders by remember { mutableStateOf<List<OrderModel>>(emptyList()) }
    var filteredOrders by remember { mutableStateOf<List<OrderModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showOrderDetails by remember { mutableStateOf<OrderModel?>(null) }

    val scope = rememberCoroutineScope()

    // ✅ Load orders
    fun loadOrders() {
        isLoading = true
        Firebase.firestore.collection("orders")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    orders = task.result.documents.mapNotNull { doc ->
                        doc.toObject(OrderModel::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.date }
                    filteredOrders = if (selectedFilter == "ALL") orders else orders.filter { it.status == selectedFilter }
                }
                isLoading = false
            }
    }

    LaunchedEffect(key1 = Unit) {
        loadOrders()
    }

    // Filter orders when status changes
    LaunchedEffect(selectedFilter, orders) {
        filteredOrders = if (selectedFilter == "ALL") orders else orders.filter { it.status == selectedFilter }
    }

    // Update order status
    fun updateOrderStatus(order: OrderModel, newStatus: String) {
        scope.launch {
            try {
                Firebase.firestore.collection("orders")
                    .document(order.id)
                    .update("status", newStatus)
                    .await()
                loadOrders()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Orders Management",
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(
                    onClick = { loadOrders() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("ALL", "ORDERED", "SHIPPING", "DELIVERED", "CANCELLED")) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = when (filter) {
                                    "ALL" -> "All (${orders.size})"
                                    else -> "$filter (${orders.count { it.status == filter }})"
                                }
                            )
                        },
                        leadingIcon = if (selectedFilter == filter) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            // Orders List
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                filteredOrders.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                if (selectedFilter == "ALL") "No orders yet" else "No $selectedFilter orders",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredOrders) { order ->
                            OrderCard(
                                order = order,
                                onUpdateStatus = { newStatus -> updateOrderStatus(order, newStatus) },
                                onViewDetails = { showOrderDetails = order }
                            )
                        }
                    }
                }
            }
        }
    }

    // Order Details Dialog
    if (showOrderDetails != null) {
        OrderDetailsDialog(
            order = showOrderDetails!!,
            onDismiss = { showOrderDetails = null },
            onUpdateStatus = { newStatus ->
                updateOrderStatus(showOrderDetails!!, newStatus)
                showOrderDetails = null
            }
        )
    }
}

@Composable
fun OrderCard(
    order: OrderModel,
    onUpdateStatus: (String) -> Unit,
    onViewDetails: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onViewDetails
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.id.take(8)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = order.date?.toDate()?.let { dateFormat.format(it) } ?: "N/A",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OrderStatusBadge(status = order.status)
            }

            HorizontalDivider()

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = order.userName.ifBlank { "Customer" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = order.userEmail.ifBlank { "N/A" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${order.items.size} items",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = currencyFormat.format(order.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (order.status != "DELIVERED" && order.status != "CANCELLED") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (order.status) {
                        "ORDERED" -> {
                            OutlinedButton(
                                onClick = { onUpdateStatus("SHIPPING") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ship")
                            }
                            OutlinedButton(
                                onClick = { onUpdateStatus("CANCELLED") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        }
                        "SHIPPING" -> {
                            Button(
                                onClick = { onUpdateStatus("DELIVERED") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CheckCircle, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Mark as Delivered")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStatusBadge(status: String) {
    val (color, text, icon) = when (status) {
        "ORDERED" -> Triple(MaterialTheme.colorScheme.primaryContainer, "Ordered", Icons.Default.ShoppingBag)
        "SHIPPING" -> Triple(MaterialTheme.colorScheme.tertiaryContainer, "Shipping", Icons.Default.LocalShipping)
        "DELIVERED" -> Triple(MaterialTheme.colorScheme.secondaryContainer, "Delivered", Icons.Default.CheckCircle)
        "CANCELLED" -> Triple(MaterialTheme.colorScheme.errorContainer, "Cancelled", Icons.Default.Cancel)
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, "Unknown", Icons.Default.HelpOutline)
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsDialog(
    order: OrderModel,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    AlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text(text = "Order Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                DetailRow("Order ID", "#${order.id.take(8)}")
                DetailRow("Date", order.date?.toDate()?.let { dateFormat.format(it) } ?: "N/A")
                DetailRow("Status", order.status)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(text = "Customer Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                DetailRow("Name", order.userName.ifBlank { "N/A" })
                DetailRow("Email", order.userEmail.ifBlank { "N/A" })
                DetailRow("Address", order.address.ifBlank { "N/A" })
                DetailRow("Payment", order.paymentMethod.ifBlank { "COD" })

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(text = "Items (${order.items.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                order.items.forEach { (productId, quantity) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "${productId.take(8)}... x$quantity", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Total Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = currencyFormat.format(order.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}