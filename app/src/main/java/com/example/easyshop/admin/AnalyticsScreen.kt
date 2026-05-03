package com.example.easyshop.admin

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.R
import com.example.easyshop.model.OrderModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor

enum class TimePeriod(val label: String, val days: Int) {
    DAY("Hôm nay", 1),
    WEEK("7 ngày", 7),
    MONTH("30 ngày", 30),
    TWO_MONTHS("60 ngày", 60)
}

data class ProductStat(val id: String, val count: Int, val revenue: Double, val imageUrl: String? = null, val title: String? = null)

// Design tokens
private val GreenSuccess  = Color(0xFF00C896)
private val AmberWarn     = Color(0xFFFFB300)
private val RedCancel     = Color(0xFFFF5252)
private val PurpleAccent  = Color(0xFF7C4DFF)
private val BlueCard      = Color(0xFF1565C0)

@Composable
fun AnalyticsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var orders by remember { mutableStateOf<List<OrderModel>>(emptyList()) }
    var allOrders by remember { mutableStateOf<List<OrderModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var selectedPeriod by remember { mutableStateOf(TimePeriod.WEEK) }
    var totalRevenue by remember { mutableDoubleStateOf(0.0) }
    var deliveredOrdersCount by remember { mutableIntStateOf(0) }
    var cancelledOrdersCount by remember { mutableIntStateOf(0) }
    var topProducts by remember { mutableStateOf<List<ProductStat>>(emptyList()) }
    var dailyRevenue by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    var lastUpdatedAt by remember { mutableStateOf<Date?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            com.example.easyshop.AppUtil.showError("Cấp quyền thất bại", "Ứng dụng cần quyền bộ nhớ để lưu báo cáo.")
        }
    }

    val firestore = Firebase.firestore

    // Load tất cả orders 1 lần
    LaunchedEffect(Unit) {
        try {
            val result = firestore.collection("orders").get().await()
            allOrders = result.documents.mapNotNull { it.toObject(OrderModel::class.java) }
            orders = allOrders
            lastUpdatedAt = Date()
            isLoading = false
        } catch (e: Exception) { isLoading = false }
    }

    // Tính toán lại dữ liệu khi selectedPeriod hoặc allOrders thay đổi
    LaunchedEffect(selectedPeriod, allOrders) {
        if (allOrders.isEmpty()) return@LaunchedEffect

        val now = System.currentTimeMillis()
        val cutoff = now - selectedPeriod.days * 24 * 60 * 60 * 1000L
        val filteredOrders = allOrders.filter { it.date.toDate().time >= cutoff }
        orders = filteredOrders

        totalRevenue = filteredOrders.filter { it.status == "DELIVERED" }.sumOf { it.total }
        deliveredOrdersCount = filteredOrders.count { it.status == "DELIVERED" }
        cancelledOrdersCount = filteredOrders.count { it.status == "CANCELLED" }

        // Top products
        val productStats = mutableMapOf<String, ProductStat>()
        val productCache = mutableMapOf<String, com.example.easyshop.model.ProductModel?>()
        filteredOrders.filter { it.status == "DELIVERED" }.forEach { order ->
            order.items.forEach { (id, qty) ->
                val product = if (productCache.containsKey(id)) productCache[id]
                else {
                    val p = firestore.collection("data").document("stock").collection("products")
                        .document(id).get().await()
                        .toObject(com.example.easyshop.model.ProductModel::class.java)
                    productCache[id] = p; p
                }
                val price = product?.actualPrice?.toDoubleOrNull() ?: 0.0
                val imageUrl = product?.images?.firstOrNull()
                val title = product?.title
                val cur = productStats[id] ?: ProductStat(id, 0, 0.0, imageUrl, title)
                productStats[id] = ProductStat(id, cur.count + qty.toInt(), cur.revenue + price * qty, imageUrl, title)
            }
        }
        topProducts = productStats.values.sortedByDescending { it.revenue }.take(5)

        // Revenue chart data — gộp theo ngày/tuần tùy period
        val sdf = when (selectedPeriod) {
            TimePeriod.DAY -> SimpleDateFormat("HH:00", Locale.getDefault())
            TimePeriod.WEEK -> SimpleDateFormat("dd/MM", Locale.getDefault())
            TimePeriod.MONTH -> SimpleDateFormat("dd/MM", Locale.getDefault())
            TimePeriod.TWO_MONTHS -> SimpleDateFormat("'T'w", Locale.getDefault()) // Tuần
        }
        val dailyMap = mutableMapOf<String, Double>()
        filteredOrders.filter { it.status == "DELIVERED" }.forEach { order ->
            val d = sdf.format(order.date.toDate())
            dailyMap[d] = (dailyMap[d] ?: 0.0) + order.total
        }

        val cal = Calendar.getInstance()
        val dataPoints: List<Pair<String, Double>> = when (selectedPeriod) {
            TimePeriod.DAY -> {
                // 24 giờ, nhóm lại thành 8 khung 3h
                val hourFmt = SimpleDateFormat("HH:00", Locale.getDefault())
                val hourMap = mutableMapOf<String, Double>()
                filteredOrders.filter { it.status == "DELIVERED" }.forEach { order ->
                    val h = hourFmt.format(order.date.toDate())
                    hourMap[h] = (hourMap[h] ?: 0.0) + order.total
                }
                (0..7).map { slot ->
                    val hour = slot * 3
                    val label = "${hour}h"
                    val total = (0..2).sumOf { h ->
                        val key = String.format("%02d:00", hour + h)
                        hourMap[key] ?: 0.0
                    }
                    label to total
                }
            }
            TimePeriod.WEEK -> {
                val dayFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
                (6 downTo 0).map { offset ->
                    cal.time = Date(now - offset * 24 * 60 * 60 * 1000L)
                    val label = dayFmt.format(cal.time)
                    label to (dailyMap[label] ?: 0.0)
                }
            }
            TimePeriod.MONTH -> {
                // Nhóm thành 10 mốc, mỗi mốc 3 ngày
                val dayFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
                val dayFullMap = mutableMapOf<String, Double>()
                filteredOrders.filter { it.status == "DELIVERED" }.forEach { order ->
                    val d = dayFmt.format(order.date.toDate())
                    dayFullMap[d] = (dayFullMap[d] ?: 0.0) + order.total
                }
                (9 downTo 0).map { slot ->
                    val endOffset = slot * 3
                    val startOffset = endOffset + 2
                    var total = 0.0
                    var label = ""
                    for (d in startOffset downTo endOffset) {
                        cal.time = Date(now - d * 24 * 60 * 60 * 1000L)
                        val key = dayFmt.format(cal.time)
                        total += dayFullMap[key] ?: 0.0
                        if (d == endOffset) label = key
                    }
                    label to total
                }
            }
            TimePeriod.TWO_MONTHS -> {
                // Nhóm thành 12 mốc, mỗi mốc 5 ngày
                val weekFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
                val weeklyMap = mutableMapOf<String, Double>()
                filteredOrders.filter { it.status == "DELIVERED" }.forEach { order ->
                    val d = weekFmt.format(order.date.toDate())
                    weeklyMap[d] = (weeklyMap[d] ?: 0.0) + order.total
                }
                (11 downTo 0).map { slot ->
                    val endOffset = slot * 5
                    val startOffset = endOffset + 4
                    var total = 0.0
                    var labelEnd = ""
                    for (d in startOffset downTo endOffset) {
                        cal.time = Date(now - d * 24 * 60 * 60 * 1000L)
                        val key = weekFmt.format(cal.time)
                        total += weeklyMap[key] ?: 0.0
                        if (d == endOffset) labelEnd = key
                    }
                    labelEnd to total
                }
            }
        }
        dailyRevenue = dataPoints
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.business_analytics_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val currencyFmt = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
            val pendingCount = orders.size - deliveredOrdersCount - cancelledOrdersCount

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Hero Revenue Card ──────────────────────────────────────
                item {
                    HeroRevenueCard(
                        totalRevenue = totalRevenue,
                        totalOrders = orders.size,
                        currencyFmt = currencyFmt
                    )
                }


                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lastUpdatedAt?.let {
                                "Cập nhật lúc ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)}"
                            } ?: "Cập nhật lúc --",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Styled Export Button
                        Surface(
                            onClick = {
                                if (orders.isNotEmpty()) {
                                    showPreviewDialog = true
                                } else {
                                    com.example.easyshop.AppUtil.showError("Không có dữ liệu", "Danh sách đơn hàng đang trống")
                                }
                            },
                            color = GreenSuccess.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = GreenSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Xuất báo cáo",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = GreenSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ── Stat Chips Row ─────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatChipCard(
                            modifier = Modifier.weight(1f),
                            label = stringResource(id = R.string.delivered_status),
                            value = deliveredOrdersCount.toString(),
                            icon = Icons.Default.CheckCircle,
                            iconColor = GreenSuccess,
                            bgColor = GreenSuccess.copy(alpha = 0.08f)
                        )
                        StatChipCard(
                            modifier = Modifier.weight(1f),
                            label = stringResource(id = R.string.pending_status_legend),
                            value = pendingCount.toString(),
                            icon = Icons.Default.Pending,
                            iconColor = AmberWarn,
                            bgColor = AmberWarn.copy(alpha = 0.08f)
                        )
                        StatChipCard(
                            modifier = Modifier.weight(1f),
                            label = stringResource(id = R.string.cancelled_status),
                            value = cancelledOrdersCount.toString(),
                            icon = Icons.Default.Cancel,
                            iconColor = RedCancel,
                            bgColor = RedCancel.copy(alpha = 0.08f)
                        )
                    }
                }

                // ── Revenue Trend Bar Chart ────────────────────────────────
                item {
                    SectionTitle(
                        title = "${stringResource(id = R.string.revenue_trend_title)} (${selectedPeriod.label})",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        tint = PurpleAccent
                    )
                    Spacer(Modifier.height(10.dp))

                    // ── Time Period Filter Chips ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimePeriod.entries.forEach { period ->
                            val isSelected = selectedPeriod == period
                            Surface(
                                onClick = { selectedPeriod = period },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PurpleAccent else PurpleAccent.copy(alpha = 0.08f),
                                border = if (isSelected) null else BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = period.label,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else PurpleAccent,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    PremiumBarChart(data = dailyRevenue, currencyFmt = currencyFmt)
                }

                // ── Order Distribution ──────────────────────────────────────
                item {
                    SectionTitle(
                        title = stringResource(id = R.string.order_distribution_title),
                        icon = Icons.Default.PieChart,
                        tint = BlueCard
                    )
                    Spacer(Modifier.height(10.dp))
                    PremiumOrderDistribution(
                        delivered = deliveredOrdersCount,
                        cancelled = cancelledOrdersCount,
                        other = pendingCount
                    )
                }

                // ── Top Sellers ─────────────────────────────────────────────
                item {
                    SectionTitle(
                        title = stringResource(id = R.string.top_selling_products_title),
                        icon = Icons.Default.Leaderboard,
                        tint = AmberWarn
                    )
                    Spacer(Modifier.height(4.dp))
                }

                if (topProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(id = R.string.no_sales_data),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(topProducts) { index, stat ->
                        PremiumTopProductItem(rank = index + 1, stat = stat, currencyFmt = currencyFmt)
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    // ── Preview Dialog ───────────────────────────────────────────────────
    if (showPreviewDialog && orders.isNotEmpty()) {
        PreviewReportDialog(
            orders = orders,
            isExporting = isExporting,
            onDismiss = { if (!isExporting) showPreviewDialog = false },
            onConfirm = {
                val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                
                if (isGranted || android.os.Build.VERSION.SDK_INT >= 29) {
                    scope.launch {
                        isExporting = true
                        try {
                            val allProductIds = orders.flatMap { it.items.keys }.distinct()
                            val productNames = mutableMapOf<String, String>()
                            val productPrices = mutableMapOf<String, Double>()
                            
                            allProductIds.forEach { id ->
                                val doc = firestore.collection("data").document("stock").collection("products")
                                    .document(id).get().await()
                                productNames[id] = doc.getString("title") ?: id
                                // Xử lý giá: xóa "đ", xóa dấu phẩy và chuyển sang Double
                                val priceRaw = doc.get("actualPrice")?.toString() ?: "0"
                                val priceCleaned = priceRaw.replace("đ", "").replace(",", "").trim()
                                productPrices[id] = priceCleaned.toDoubleOrNull() ?: 0.0
                            }
                            
                            com.example.easyshop.AppUtil.exportOrdersToCSV(context, orders, productNames, productPrices)
                            showPreviewDialog = false
                        } catch (e: Exception) {
                            com.example.easyshop.AppUtil.showError("Lỗi xuất file", e.message)
                        } finally {
                            isExporting = false
                        }
                    }
                } else {
                    permissionLauncher.launch(permission)
                }
            }
        )
    }
}

@Composable
fun PreviewReportDialog(
    orders: List<OrderModel>,
    isExporting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var expandedOrderId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Xem trước dữ liệu xuất",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Đóng")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Button(
                            onClick = onConfirm,
                            enabled = !isExporting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("Đang xử lý dữ liệu...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(Icons.Default.FileDownload, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Xác nhận & Tải file CSV", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(16.dp))
                    
                    // Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = GreenSuccess)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Báo cáo kinh doanh", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Tổng cộng: ${orders.size} đơn hàng đã sẵn sàng", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Table Header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.width(65.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Receipt, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(2.dp))
                                Text("Mã", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            Row(modifier = Modifier.width(85.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(2.dp))
                                Text("Ngày", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(2.dp))
                                Text("Khách", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            Row(modifier = Modifier.width(90.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                Icon(Icons.Default.MonetizationOn, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(2.dp))
                                Text("Tiền", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    ) {
                        items(orders) { order ->
                            val isExpanded = expandedOrderId == order.id
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedOrderId = if (isExpanded) null else order.id }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val statusIcon = when(order.status) {
                                        "DELIVERED" -> Icons.Default.CheckCircle
                                        "CANCELLED" -> Icons.Default.Cancel
                                        else -> Icons.Default.Pending
                                    }
                                    val statusColor = when(order.status) {
                                        "DELIVERED" -> GreenSuccess
                                        "CANCELLED" -> RedCancel
                                        else -> AmberWarn
                                    }
                                    
                                    Box(modifier = Modifier.width(65.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(statusIcon, null, modifier = Modifier.size(14.dp), tint = statusColor)
                                            Spacer(Modifier.width(4.dp))
                                            Text(order.id.takeLast(4), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    Text(
                                        com.example.easyshop.AppUtil.formatDate(order.date).substringBefore(" "),
                                        modifier = Modifier.width(85.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        order.userName, 
                                        modifier = Modifier.weight(1f), 
                                        style = MaterialTheme.typography.bodySmall, 
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        com.example.easyshop.AppUtil.formatPrice(order.total),
                                        modifier = Modifier.width(90.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.End,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(start = 73.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                                    ) {
                                        Text(
                                            "Chi tiết sản phẩm:", 
                                            style = MaterialTheme.typography.labelSmall, 
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        
                                        order.items.forEach { (productId, qty) ->
                                            ProductItemDetailRow(productId, qty)
                                        }
                                        
                                        if (order.promoCode.isNotEmpty()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "Khuyến mãi: ${order.promoCode} (-${com.example.easyshop.AppUtil.formatPrice(order.discount)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GreenSuccess
                                            )
                                        }
                                    }
                                }
                                
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Lưu ý: File CSV sẽ bao gồm đầy đủ chi tiết các sản phẩm trong mỗi đơn hàng.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}



// ── Hero Revenue Card ────────────────────────────────────────────────────────
@Composable
fun HeroRevenueCard(totalRevenue: Double, totalOrders: Int, currencyFmt: NumberFormat) {
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
        start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .padding(24.dp)
    ) {
        // Decorative circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MonetizationOn, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(id = R.string.total_revenue_label),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = currencyFmt.format(totalRevenue),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 28.sp
            )
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroStatItem(
                    label = stringResource(id = R.string.orders),
                    value = totalOrders.toString(),
                    icon = Icons.Default.Inventory2
                )
                HeroStatItem(
                    label = stringResource(id = R.string.avg_value_label),
                    value = if (totalOrders > 0) currencyFmt.format(totalRevenue / totalOrders) else "0đ",
                    icon = Icons.AutoMirrored.Filled.TrendingUp
                )
            }
        }
    }
}

@Composable
fun HeroStatItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ── Stat Chip Card ────────────────────────────────────────────────────────────
@Composable
fun StatChipCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = iconColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = iconColor.copy(alpha = 0.75f), maxLines = 1)
        }
    }
}

// ── Section Title ─────────────────────────────────────────────────────────────
@Composable
fun SectionTitle(title: String, icon: ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// ── Premium Bar Chart ─────────────────────────────────────────────────────────
@Composable
fun PremiumBarChart(data: List<Pair<String, Double>>, currencyFmt: NumberFormat) {
    val maxRevenue = data.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    val hasAnyData = data.any { it.second > 0 }
    val midRevenue = maxRevenue / 2
    val scrollable = data.size > 7
    val scrollState = rememberScrollState(Int.MAX_VALUE)
    val barSlotWidth = if (scrollable) 56.dp else 0.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            if (hasAnyData) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mốc: ${compactCurrency(midRevenue)} - ${currencyFmt.format(maxRevenue)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )


                }
                Spacer(Modifier.height(8.dp))
            }

            // Bars row
            Row(
                modifier = Modifier
                    .then(if (scrollable) Modifier.horizontalScroll(scrollState) else Modifier)
                    .then(if (scrollable) Modifier else Modifier.fillMaxWidth())
                    .height(120.dp),
                horizontalArrangement = if (scrollable) Arrangement.spacedBy(0.dp) else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { (date, value) ->
                    val fraction = if (hasAnyData && value > 0) (value / maxRevenue).toFloat().coerceIn(0.06f, 1f) else 0f
                    val animFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(900),
                        label = "bar_$date"
                    )
                    val isMaxBar = value == data.maxOfOrNull { it.second } && value > 0

                    Column(
                        modifier = if (scrollable) Modifier.width(barSlotWidth) else Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Tooltip chỉ hiện ở cột cao nhất
                        if (isMaxBar) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = compactCurrency(value),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        } else {
                            Spacer(Modifier.height(20.dp))
                        }

                        // Bar
                        if (value > 0) {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .fillMaxHeight(animFraction)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(
                                        if (isMaxBar)
                                            Brush.verticalGradient(listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                            ))
                                        else
                                            Brush.verticalGradient(listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ))
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            // Date labels
            Row(
                modifier = Modifier
                    .then(if (scrollable) Modifier.horizontalScroll(scrollState) else Modifier)
                    .then(if (scrollable) Modifier else Modifier.fillMaxWidth()),
                horizontalArrangement = if (scrollable) Arrangement.spacedBy(0.dp) else Arrangement.SpaceBetween
            ) {
                data.forEach { (date, _) ->
                    Text(
                        text = date,
                        modifier = if (scrollable) Modifier.width(barSlotWidth) else Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Premium Order Distribution ────────────────────────────────────────────────
private fun compactCurrency(value: Double): String {
    return when {
        value >= 1_000_000_000 -> "${(value / 1_000_000_000).toInt()}B"
        value >= 1_000_000 -> "${(value / 1_000_000).toInt()}M"
        value >= 1_000 -> "${(value / 1_000).toInt()}K"
        else -> value.toInt().toString()
    }
}

private fun normalizeToHundred(counts: List<Int>): List<Int> {
    val total = counts.sum()
    if (total <= 0) return List(counts.size) { 0 }

    val raw = counts.map { it * 100.0 / total }
    val base = raw.map { floor(it).toInt() }.toMutableList()
    var remainder = 100 - base.sum()

    if (remainder > 0) {
        val fractions = raw.mapIndexed { idx, v -> idx to (v - floor(v)) }
            .sortedByDescending { it.second }
        var cursor = 0
        while (remainder > 0 && fractions.isNotEmpty()) {
            val idx = fractions[cursor % fractions.size].first
            base[idx] += 1
            remainder--
            cursor++
        }
    }
    return base
}

@Composable
fun PremiumOrderDistribution(delivered: Int, cancelled: Int, other: Int) {
    val total = (delivered + cancelled + other).toFloat().coerceAtLeast(1f)
    val deliveredW = delivered / total
    val otherW = other / total
    val cancelledW = cancelled / total
    val percents = normalizeToHundred(listOf(delivered, other, cancelled))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            // Stacked bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
            ) {
                if (deliveredW > 0) Box(Modifier.fillMaxHeight().weight(deliveredW).background(GreenSuccess))
                if (otherW > 0) Box(Modifier.fillMaxHeight().weight(otherW).background(AmberWarn))
                if (cancelledW > 0) Box(Modifier.fillMaxHeight().weight(cancelledW).background(RedCancel))
            }

            Spacer(Modifier.height(20.dp))

            // Legend with counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DistributionLegend(stringResource(id = R.string.delivered_status), delivered, percents[0], GreenSuccess)
                DistributionLegend(stringResource(id = R.string.pending_status_legend), other, percents[1], AmberWarn)
                DistributionLegend(stringResource(id = R.string.cancelled_status), cancelled, percents[2], RedCancel)
            }
        }
    }
}

@Composable
fun DistributionLegend(label: String, count: Int, percent: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$percent%",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

// ── Premium Top Product Item ───────────────────────────────────────────────────
private val rankColors = listOf(
    Color(0xFFFFD700), // 🥇 Gold
    Color(0xFFC0C0C0), // 🥈 Silver
    Color(0xFFCD7F32), // 🥉 Bronze
    Color(0xFF90A4AE),
    Color(0xFF90A4AE)
)

@Composable
fun PremiumTopProductItem(rank: Int, stat: ProductStat, currencyFmt: NumberFormat) {
    val productName = stat.title ?: stringResource(id = R.string.unknown_product)
    val rankColor = rankColors.getOrElse(rank - 1) { Color.Gray }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp), // Loai bo shadow xam
        colors = CardDefaults.cardColors(
            containerColor = if (rank == 1)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = rankColor
                )
            }

            Spacer(Modifier.width(12.dp))

            // Product icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (stat.imageUrl != null) {
                    AsyncImage(
                        model = stat.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.ShoppingBag, null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(id = R.string.items_sold_label, stat.count),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.width(120.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    currencyFmt.format(stat.revenue),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
//                if (rank == 1) {
//                    Text("🏆 Best", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
//                }
            }
        }
    }
}

@Composable
fun ProductItemDetailRow(productId: String, quantity: Long) {
    var productName by remember { mutableStateOf("Đang tải...") }
    val db = com.google.firebase.Firebase.firestore
    
    LaunchedEffect(productId) {
        db.collection("data")
            .document("stock").collection("products")
            .document(productId).get()
            .addOnSuccessListener { doc ->
                productName = doc.getString("title") ?: "Sản phẩm không tên"
            }
            .addOnFailureListener {
                productName = "Lỗi tải tên"
            }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Inventory2, 
                contentDescription = null, 
                modifier = Modifier.size(14.dp), 
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = productName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "x$quantity",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
