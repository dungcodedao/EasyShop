package com.example.easyshop.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.model.OrderModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showOrderDetails by remember { mutableStateOf<OrderModel?>(null) }

    val scope = rememberCoroutineScope()

    val filteredOrders = orders.filter {
        val matchesFilter = if (selectedFilter == "ALL") true else it.status == selectedFilter
        val matchesSearch = it.id.contains(searchQuery, ignoreCase = true) || 
                           it.userName.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }.sortedByDescending { it.date }

    fun loadOrders() {
        isLoading = true
        Firebase.firestore.collection("orders")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    orders = task.result.documents.mapNotNull { doc ->
                        if (doc.getBoolean("archived") == true) {
                            null
                        } else {
                            doc.toObject(OrderModel::class.java)?.copy(id = doc.id)
                        }
                    }
                }
                isLoading = false
            }
    }

    LaunchedEffect(key1 = Unit) {
        loadOrders()
    }

    fun createNotificationForUser(order: OrderModel, newStatus: String) {
        val (title, body, type) = when (newStatus) {
            "SHIPPING"  -> Triple("Đơn hàng đang vận chuyển 🚚", "Đơn #${order.id.take(8).uppercase()} đang trên đường giao đến bạn.", "SHIPPING")
            "DELIVERED" -> Triple("Đơn hàng đã giao thành công ✅", "Đơn #${order.id.take(8).uppercase()} đã giao thành công.", "DELIVERED")
            "CANCELLED" -> Triple("Đơn hàng đã bị hủy ❌", "Đơn #${order.id.take(8).uppercase()} đã bị hủy.", "CANCELLED")
            else -> Triple("Cập nhật đơn hàng", "Đơn #${order.id.take(8).uppercase()} vừa cập nhật trạng thái: $newStatus", "ORDER_STATUS")
        }

        val notif = hashMapOf(
            "userId" to order.userId,
            "title" to title,
            "body" to body,
            "type" to type,
            "orderId" to order.id,
            "isRead" to false,
            "recipientRole" to "user",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        Firebase.firestore.collection("notifications").add(notif)
            .addOnSuccessListener {
                com.example.easyshop.services.FcmSender.sendToUser(order.userId, title, body, type)
            }
    }

    fun updateOrderStatus(order: OrderModel, newStatus: String) {
        scope.launch {
            try {
                Firebase.firestore.collection("orders").document(order.id).update("status", newStatus).await()
                
                // --- LOGIC HOÀN KHO KHI HỦY ĐƠN ---
                if (newStatus == "CANCELLED") {
                    android.util.Log.d("EasyShop_Admin", "Admin đã hủy đơn #${order.id.take(8)}. Tiến hành hoàn kho...")
                    AppUtil.restoreStock(order.items)
                }
                
                createNotificationForUser(order, newStatus)
                AppUtil.showSuccess(context.getString(R.string.order_updated_msg, order.id.take(8).uppercase()))
                loadOrders()
            } catch (e: Exception) {
                AppUtil.showError(context.getString(R.string.order_update_failed), e.message)
            }
        }
    }

    fun archiveOrder(order: OrderModel) {
        scope.launch {
            try {
                val adminUid = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
                Firebase.firestore.collection("orders").document(order.id).update(
                    mapOf("archived" to true, "archivedAt" to FieldValue.serverTimestamp(), "archivedBy" to adminUid)
                ).await()
                AppUtil.showSuccess("Đã lưu trữ đơn #${order.id.take(8).uppercase()}")
                loadOrders()
            } catch (e: Exception) {
                AppUtil.showError("Lưu trữ thất bại", e.message)
            }
        }
    }

    fun deleteOrderPermanently(order: OrderModel) {
        scope.launch {
            try {
                val adminUid = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
                val auditData = hashMapOf("order" to order, "deletedBy" to adminUid, "deletedAt" to FieldValue.serverTimestamp(), "source" to "admin_orders_management")
                Firebase.firestore.collection("deleted_orders").document(order.id).set(auditData).await()
                Firebase.firestore.collection("orders").document(order.id).delete().await()
                AppUtil.showSuccess("Đã xóa vĩnh viễn đơn #${order.id.take(8).uppercase()}")
                loadOrders()
            } catch (e: Exception) {
                AppUtil.showError("Xóa vĩnh viễn thất bại", e.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.orders_management_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { loadOrders() }) { Icon(Icons.Default.Refresh, "Refresh") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm theo mã đơn hoặc tên khách...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear") } }
                } else null,
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            val tabs = listOf("ALL", "ORDERED", "SHIPPING", "DELIVERED", "CANCELLED")
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedFilter),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedFilter)]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEach { filter ->
                    val label = when (filter) {
                        "ALL" -> "TẤT CẢ"
                        "ORDERED" -> "ĐÃ ĐẶT"
                        "SHIPPING" -> "ĐANG GIAO"
                        "DELIVERED" -> "ĐÃ GIAO"
                        "CANCELLED" -> "ĐÃ HỦY"
                        else -> filter
                    }
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Medium
                            )
                        }
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
                    // Premium Empty State
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(120.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = "Không tìm thấy đơn hàng",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Hãy thử thay đổi bộ lọc hoặc từ khóa tìm kiếm của bạn.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
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
                                onViewDetails = { showOrderDetails = order },
                                onArchive = { archiveOrder(order) },
                                onDeletePermanently = { deleteOrderPermanently(order) }
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
            navController = navController,
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
    onViewDetails: () -> Unit,
    onArchive: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("vi", "VN"))
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    var showMenu by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val canManageFinalized = order.status == "DELIVERED" || order.status == "CANCELLED"

    // Dynamic coloring based on status
    val statusColor = when (order.status) {
        "ORDERED" -> Color(0xFFFFF8E1) // Amber tint
        "SHIPPING" -> Color(0xFFE3F2FD) // Blue tint
        "DELIVERED" -> Color(0xFFE8F5E9) // Green tint
        "CANCELLED" -> Color(0xFFFFEBEE) // Red tint
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onViewDetails,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Order ID and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "#${order.id.take(8).uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = order.date?.toDate()?.let { dateFormat.format(it) } ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OrderStatusBadge(status = order.status)
                    if (canManageFinalized) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Lưu trữ (Ẩn)") },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                    onClick = { showMenu = false; showArchiveConfirm = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Xóa vĩnh viễn", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; showDeleteConfirm = true }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            // Body: Customer Info & Call Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text(
                            text = order.userName.ifBlank { "Khách hàng" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = order.userEmail.ifBlank { "Không có email" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.userPhone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("smsto:${order.userPhone}")
                                }
                                context.startActivity(intent)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Message, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Customer Note Preview in Card
            if (order.note.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Notes,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = order.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Products Thumbnails
            OrderProductThumbnails(productIds = order.items.keys.toList())

            // Footer: Items count and Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${order.items.values.sum()} sản phẩm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currencyFormat.format(order.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Status Actions (Ship, Cancel, Complete)
            if (order.status != "DELIVERED" && order.status != "CANCELLED") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (order.status) {
                        "ORDERED" -> {
                            Button(
                                onClick = { onUpdateStatus("SHIPPING") },
                                modifier = Modifier.weight(1f),
                                elevation = ButtonDefaults.buttonElevation(0.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Giao hàng", fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = { onUpdateStatus("CANCELLED") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Hủy đơn", fontSize = 13.sp)
                            }
                        }
                        "SHIPPING" -> {
                            Button(
                                onClick = { onUpdateStatus("DELIVERED") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Hoàn thành đơn hàng")
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialogs (Archive/Delete) unchanged...


    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Xóa và Ẩn đơn hàng?") },
            text = { Text("Đơn #${order.id.take(8).uppercase()} sẽ bị ẩn khỏi danh sách này nhưng VẪN ĐƯỢC tính vào doanh số trong phần Thống kê.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveConfirm = false
                        onArchive()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa (Vẫn giữ thống kê)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("⚠️ Xóa vĩnh viễn dữ liệu?") },
            text = {
                Text("Đơn #${order.id.take(8).uppercase()} sẽ bị xóa hoàn toàn. Dữ liệu này sẽ KHÔNG CÒN xuất hiện trong phần Thống kê doanh thu.\n\nBạn chắc chắn chứ?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeletePermanently()
                    }
                ) {
                    Text("Tôi hiểu, cứ xóa đi", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun OrderProductThumbnails(productIds: List<String>) {
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    val firestore = Firebase.firestore

    LaunchedEffect(productIds) {
        val urls = mutableListOf<String>()
        val idsToLoad = productIds.take(4)
        for (id in idsToLoad) {
            try {
                val doc = firestore.collection("data").document("stock").collection("products")
                    .document(id).get().await()
                val product = doc.toObject(com.example.easyshop.model.ProductModel::class.java)
                product?.images?.firstOrNull()?.let { urls.add(it) }
            } catch (e: Exception) { }
        }
        images = urls
    }

    if (images.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            images.forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            }
            if (productIds.size > 4) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${productIds.size - 4}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun OrderStatusBadge(status: String) {
    val (color, text, icon, contentColor) = when (status) {
        "ORDERED" -> Quad(Color(0xFFFFC107), "Đã đặt", Icons.Default.ShoppingBag, Color.White)
        "SHIPPING" -> Quad(Color(0xFF2196F3), "Đang giao", Icons.Default.LocalShipping, Color.White)
        "DELIVERED" -> Quad(Color(0xFF4CAF50), "Đã giao", Icons.Default.CheckCircle, Color.White)
        "CANCELLED" -> Quad(Color(0xFFF44336), "Đã hủy", Icons.Default.Cancel, Color.White)
        else -> Quad(MaterialTheme.colorScheme.outline, status, Icons.Default.Help, Color.White)
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = contentColor)
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsDialog(
    order: OrderModel,
    navController: NavController,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    val context = LocalContext.current
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
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

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
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

                if (order.note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Notes, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = stringResource(R.string.customer_note_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = order.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

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

                // Subtotal row
                val subtotalVal = if (order.subtotal > 0) order.subtotal else order.total
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.subtotal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(subtotalVal),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Discount row (chỉ hiện khi có giảm giá)
                if (order.discount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.discount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            if (order.promoCode.isNotBlank()) {
                                Text(
                                    text = "Mã: ${order.promoCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Text(
                            text = "- ${currencyFormat.format(order.discount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // Total row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.total_amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currencyFormat.format(order.total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Action Buttons for Admin
                if (order.status != "DELIVERED" && order.status != "CANCELLED") {
                    Text(
                        text = stringResource(id = R.string.update_status_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(id = R.string.ship_btn), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (order.status == "SHIPPING" || order.status == "ORDERED") {
                            Button(
                                onClick = { onUpdateStatus("DELIVERED") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(id = R.string.deliver_btn), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        OutlinedButton(
                            onClick = { onUpdateStatus("CANCELLED") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(id = R.string.cancel_btn), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        navController.navigate("receipt/${order.total}/${order.id}?fromAdmin=true")
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.view_receipt_btn))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
