package com.example.easyshop.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.easyshop.model.OrderModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrdersManagementScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                    text = stringResource(id = R.string.orders_management_title),
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(
                    onClick = { loadOrders() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.reset),
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
                                    "ALL" -> "${stringResource(id = R.string.all_filter)} (${orders.size})"
                                    else -> {
                                        val statusLabel = when(filter) {
                                            "ORDERED" -> stringResource(id = R.string.ordered_status)
                                            "SHIPPING" -> stringResource(id = R.string.shipping_status)
                                            "DELIVERED" -> stringResource(id = R.string.delivered_status)
                                            "CANCELLED" -> stringResource(id = R.string.cancelled_status)
                                            else -> filter
                                        }
                                        "$statusLabel (${orders.count { it.status == filter }})"
                                    }
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
                                if (selectedFilter == "ALL") stringResource(id = R.string.no_orders_yet) 
                                else {
                                    val statusLabel = when(selectedFilter) {
                                        "ORDERED" -> stringResource(id = R.string.ordered_status)
                                        "SHIPPING" -> stringResource(id = R.string.shipping_status)
                                        "DELIVERED" -> stringResource(id = R.string.delivered_status)
                                        "CANCELLED" -> stringResource(id = R.string.cancelled_status)
                                        else -> selectedFilter
                                    }
                                    stringResource(id = R.string.no_filtered_orders, statusLabel)
                                },
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
                    items(
                        items = filteredOrders,
                        key = { it.id }
                    ) { order ->
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
    val context = LocalContext.current
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
                        text = "${stringResource(id = R.string.order_number_prefix)}${order.id.take(8)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = order.date?.toDate()?.let { dateFormat.format(it) } ?: stringResource(id = R.string.no_active_orders),
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
                        text = order.userEmail.ifBlank { stringResource(id = R.string.no_active_orders) },
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
                    text = "${order.items.size} ${stringResource(id = R.string.cart_items)}",
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
                                Text(stringResource(id = R.string.ship_btn))
                            }
                            OutlinedButton(
                                onClick = { onUpdateStatus("CANCELLED") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(context.getString(R.string.cancel_btn))
                            }
                        }
                        "SHIPPING" -> {
                            Button(
                                onClick = { onUpdateStatus("DELIVERED") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CheckCircle, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(id = R.string.mark_as_delivered))
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
        "ORDERED" -> Triple(MaterialTheme.colorScheme.primaryContainer, stringResource(id = R.string.ordered_status), Icons.Default.ShoppingBag)
        "SHIPPING" -> Triple(MaterialTheme.colorScheme.tertiaryContainer, stringResource(id = R.string.shipping_status), Icons.Default.LocalShipping)
        "DELIVERED" -> Triple(MaterialTheme.colorScheme.secondaryContainer, stringResource(id = R.string.delivered_status), Icons.Default.CheckCircle)
        "CANCELLED" -> Triple(MaterialTheme.colorScheme.errorContainer, stringResource(id = R.string.cancelled_status), Icons.Default.Cancel)
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, stringResource(id = R.string.no_active_orders), Icons.Default.HelpOutline)
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
    val context = LocalContext.current
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val productNames = remember { mutableStateMapOf<String, String>() }

    // Fetch product names for labels
    LaunchedEffect(order.items) {
        order.items.keys.forEach { productId ->
            Firebase.firestore.collection("data")
                .document("stock").collection("products")
                .document(productId).get()
                .addOnSuccessListener { doc ->
                    productNames[productId] = doc.getString("title") ?: context.getString(R.string.unknown_product)
                }
        }
    }

    AlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(id = R.string.order_details),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                DetailRow(stringResource(id = R.string.order_id_label), "#${order.id.take(8)}")
                DetailRow(stringResource(id = R.string.date), order.date?.toDate()?.let { dateFormat.format(it) } ?: stringResource(id = R.string.no_active_orders))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${stringResource(id = R.string.order_status)}: ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    OrderStatusBadge(order.status)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = stringResource(id = R.string.customer_info_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                DetailRow(stringResource(id = R.string.full_name), order.userName.ifBlank { stringResource(id = R.string.no_active_orders) })
                DetailRow(stringResource(id = R.string.email), order.userEmail.ifBlank { stringResource(id = R.string.no_active_orders) })
                DetailRow(stringResource(id = R.string.address), order.address.ifBlank { stringResource(id = R.string.no_active_orders) })
                DetailRow(stringResource(id = R.string.payment_method_label), order.paymentMethod.ifBlank { "COD" })

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "${stringResource(id = R.string.ordered_items)} (${order.items.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                order.items.forEach { (productId, quantity) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val name = productNames[productId] ?: stringResource(id = R.string.loading_label)
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "x$quantity",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.total_amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currencyFormat.format(order.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Action Buttons for Admin
                if (order.status != "DELIVERED" && order.status != "CANCELLED") {
                    Text(
                        text = stringResource(id = R.string.update_status_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (order.status == "ORDERED") {
                            Button(
                                onClick = { onUpdateStatus("SHIPPING") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(id = R.string.ship_btn), fontSize = 12.sp)
                            }
                        }
                        if (order.status == "SHIPPING" || order.status == "ORDERED") {
                            Button(
                                onClick = { onUpdateStatus("DELIVERED") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(id = R.string.deliver_btn), fontSize = 12.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = { onUpdateStatus("CANCELLED") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(id = R.string.cancel_btn), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(id = R.string.close_btn))
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