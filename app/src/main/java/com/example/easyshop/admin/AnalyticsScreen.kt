package com.example.easyshop.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.easyshop.R
import com.example.easyshop.model.OrderModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext

data class ProductStat(val id: String, val count: Int, val revenue: Double)

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
    var topProducts by remember { mutableStateOf<List<ProductStat>>(emptyList()) }
    var dailyRevenue by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }


    val firestore = Firebase.firestore

    LaunchedEffect(key1 = Unit) {
        try {
            val result = firestore.collection("orders").get().await()
            orders = result.documents.mapNotNull { it.toObject(OrderModel::class.java) }

            // Calculate Metrics
            totalRevenue = orders.filter { it.status == "DELIVERED" }.sumOf { it.total }
            delivedOrdersCount = orders.count { it.status == "DELIVERED" }
            cancelledOrdersCount = orders.count { it.status == "CANCELLED" }

            // Top Products with Revenue
            val productStats = mutableMapOf<String, ProductStat>()
            val productCache = mutableMapOf<String, com.example.easyshop.model.ProductModel?>()
            
            orders.filter { it.status == "DELIVERED" }.forEach { order ->
                order.items.forEach { (id, qty) ->
                    val product = if (productCache.containsKey(id)) {
                        productCache[id]
                    } else {
                        val p = firestore.collection("data").document("stock").collection("products")
                            .document(id).get().await().toObject(com.example.easyshop.model.ProductModel::class.java)
                        productCache[id] = p
                        p
                    }
                    
                    val price = product?.actualPrice?.toDoubleOrNull() ?: 0.0
                    val current = productStats[id] ?: ProductStat(id, 0, 0.0)
                    productStats[id] = ProductStat(
                        id = id,
                        count = current.count + qty.toInt(),
                        revenue = current.revenue + (price * qty)
                    )
                }
            }
            topProducts = productStats.values.sortedByDescending { it.revenue }.take(5)

            // Daily Revenue (Last 7 days)
            val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            val dailyMap = mutableMapOf<String, Double>()
            orders.filter { it.status == "DELIVERED" }.forEach { order ->
                val dateStr = sdf.format(order.date.toDate())
                dailyMap[dateStr] = (dailyMap[dateStr] ?: 0.0) + order.total
            }
            dailyRevenue = dailyMap.toList().sortedByDescending { it.first }.reversed().takeLast(7)

            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.business_analytics_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_to_home))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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

                // Revenue Trend
                item {
                    Text(
                        stringResource(id = R.string.revenue_trend_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    RevenueBarChart(dailyRevenue)
                }

                // Order Distribution
                item {
                    Text(
                        stringResource(id = R.string.order_distribution_title),
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
                        stringResource(id = R.string.top_selling_products_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (topProducts.isEmpty()) {
                    item {
                        Text(stringResource(id = R.string.no_sales_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(
                        items = topProducts,
                        key = { it.id }
                    ) { stat: ProductStat ->
                        TopProductItem(stat)
                    }
                }
            }
        }
    }
}

@Composable
fun RevenueCard(total: Double, totalOrders: Int) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))

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
                    stringResource(id = R.string.total_revenue_label),
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
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatSubItem(stringResource(id = R.string.orders), totalOrders.toString(), Icons.Default.Inventory2)
                StatSubItem(stringResource(id = R.string.avg_value_label), if (totalOrders > 0) currencyFormat.format(total / totalOrders) else "$0", Icons.AutoMirrored.Filled.TrendingUp)
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
fun RevenueBarChart(data: List<Pair<String, Double>>) {
    val maxRevenue = data.maxOfOrNull { it.second } ?: 1.0
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { (date, value) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((value / maxRevenue).toFloat().coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(date, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
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
                LegendItem(stringResource(id = R.string.delivered_status), Color(0xFF4CAF50))
                LegendItem(stringResource(id = R.string.pending_status_legend), Color(0xFFFFB300))
                LegendItem(stringResource(id = R.string.cancelled_status), Color(0xFFF44336))
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
fun TopProductItem(stat: ProductStat) {
    val context = LocalContext.current
    var productName by remember { mutableStateOf("") }
    if (productName.isEmpty()) productName = stringResource(id = R.string.loading_label)
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))

    LaunchedEffect(stat.id) {
        Firebase.firestore.collection("data").document("stock").collection("products")
            .document(stat.id).get().addOnSuccessListener {
                productName = it.getString("title") ?: context.getString(R.string.unknown_product)
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
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ShoppingBag, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(productName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(id = R.string.items_sold_label, stat.count), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                currencyFormat.format(stat.revenue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}