package com.example.easyshop.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.easyshop.model.OrderModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var orders by remember { mutableStateOf<List<OrderModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Metrics
    var totalRevenue by remember { mutableDoubleStateOf(0.0) }
    var delivedOrdersCount by remember { mutableIntStateOf(0) }
    var cancelledOrdersCount by remember { mutableIntStateOf(0) }
    var topProducts by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    val firestore = Firebase.firestore

    LaunchedEffect(key1 = Unit) {
        try {
            val result = firestore.collection("orders").get().await()
            orders = result.documents.mapNotNull { it.toObject(OrderModel::class.java) }

            // Calculate Metrics
            totalRevenue = orders.filter { it.status == "DELIVERED" }.sumOf { it.total }
            delivedOrdersCount = orders.count { it.status == "DELIVERED" }
            cancelledOrdersCount = orders.count { it.status == "CANCELLED" }

            // Calculate Top Products
            val productCounts = mutableMapOf<String, Int>()
            orders.forEach { order ->
                order.items.forEach { (id, qty) ->
                    productCounts[id] = (productCounts[id] ?: 0) + qty.toInt()
                }
            }
            topProducts = productCounts.toList().sortedByDescending { it.second }.take(5)

            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Main Stats
                item {
                    RevenueCard(totalRevenue, orders.size)
                }

                // Order Distribution
                item {
                    Text(
                        "Order Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    OrderDistributionChart(
                        delivered = delivedOrdersCount,
                        cancelled = cancelledOrdersCount,
                        other = orders.size - delivedOrdersCount - cancelledOrdersCount
                    )
                }

                // Best Sellers
                item {
                    Text(
                        "Top Selling Products",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (topProducts.isEmpty()) {
                    item {
                        Text("No sales data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(topProducts) { (productId, count) ->
                        TopProductItem(productId, count)
                    }
                }
            }
        }
    }
}

@Composable
fun RevenueCard(total: Double, totalOrders: Int) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Total Revenue",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Text(
                text = currencyFormat.format(total),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatSubItem("Orders", totalOrders.toString(), Icons.Default.Inventory2)
                StatSubItem("Avg. Value", if (totalOrders > 0) currencyFormat.format(total / totalOrders) else "$0", Icons.Default.TrendingUp)
            }
        }
    }
}

@Composable
fun StatSubItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun OrderDistributionChart(delivered: Int, cancelled: Int, other: Int) {
    val total = (delivered + cancelled + other).toFloat().coerceAtLeast(1f)
    val deliveredWeight = delivered / total
    val cancelledWeight = cancelled / total
    val otherWeight = other / total

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (deliveredWeight > 0) Box(Modifier.fillMaxHeight().weight(deliveredWeight).background(Color(0xFF4CAF50)))
                if (otherWeight > 0) Box(Modifier.fillMaxHeight().weight(otherWeight).background(Color(0xFFFFB300)))
                if (cancelledWeight > 0) Box(Modifier.fillMaxHeight().weight(cancelledWeight).background(Color(0xFFF44336)))
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem("Delivered", Color(0xFF4CAF50))
                LegendItem("Pending", Color(0xFFFFB300))
                LegendItem("Cancelled", Color(0xFFF44336))
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TopProductItem(productId: String, count: Int) {
    var productName by remember { mutableStateOf("Loading...") }

    LaunchedEffect(productId) {
        Firebase.firestore.collection("data").document("stock").collection("products")
            .document(productId).get().addOnSuccessListener {
                productName = it.getString("title") ?: "Unknown Product"
            }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ShoppingBag, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("ID: ${productId.take(8)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "$count sold",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}