package com.example.easyshop.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.FiberNew
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.easyshop.model.NotificationModel
import com.example.easyshop.model.OrderModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// ─── Hàm tiện ích: format thời gian tương đối ────────────────────────────────
private fun formatRelativeTime(timestamp: Timestamp): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp.toDate().time
    val diffMin = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val diffH = TimeUnit.MILLISECONDS.toHours(diffMs)
    val diffD = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        diffMin < 1  -> "Vừa xong"
        diffMin < 60 -> "${diffMin} phút trước"
        diffH < 24   -> "${diffH} giờ trước"
        diffD < 7    -> "${diffD} ngày trước"
        else         -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(timestamp.toDate())
    }
}

// ─── Kiểu thông báo → màu + icon ────────────────────────────────────────────
private data class NotifStyle(val color: Color, val icon: ImageVector, val bgColor: Color)

private fun styleFor(type: String): NotifStyle = when (type) {
    "NEW_ORDER"    -> NotifStyle(Color(0xFF7C4DFF), Icons.Rounded.ShoppingCart,     Color(0xFFEDE7F6))
    "SHIPPING"     -> NotifStyle(Color(0xFF0288D1), Icons.Rounded.LocalShipping,    Color(0xFFE1F5FE))
    "DELIVERED"    -> NotifStyle(Color(0xFF388E3C), Icons.Rounded.CheckCircle,      Color(0xFFE8F5E9))
    "CANCELLED"    -> NotifStyle(Color(0xFFD32F2F), Icons.Rounded.Cancel,           Color(0xFFFFEBEE))
    "ORDER_STATUS" -> NotifStyle(Color(0xFFF57C00), Icons.Rounded.Autorenew,        Color(0xFFFFF3E0))
    "PROMO"        -> NotifStyle(Color(0xFFC2185B), Icons.Rounded.ConfirmationNumber, Color(0xFFFCE4EC))
    else           -> NotifStyle(Color(0xFF546E7A), Icons.Rounded.Notifications,    Color(0xFFECEFF1))
}

// ─── Màn hình Thông báo Admin ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()

    var notifications by remember { mutableStateOf<List<NotificationModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedOrder by remember { mutableStateOf<OrderModel?>(null) }

    val unreadCount = notifications.count { !it.isRead }

    // Lắng nghe realtime từ Firestore
    LaunchedEffect(Unit) {
        db.collection("notifications")
            .whereEqualTo("recipientRole", "admin")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    notifications = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(NotificationModel::class.java)?.copy(id = doc.id)
                    }
                }
                isLoading = false
            }
    }

    // Hàm đánh dấu 1 thông báo đã đọc
    fun markAsRead(notif: NotificationModel) {
        if (!notif.isRead) {
            db.collection("notifications").document(notif.id)
                .update("isRead", true)
        }
    }

    // Hàm đánh dấu tất cả đã đọc
    fun markAllAsRead() {
        notifications.filter { !it.isRead }.forEach { notif ->
            db.collection("notifications").document(notif.id)
                .update("isRead", true)
        }
    }

    // Gửi thông báo cho user khi Admin cập nhật trạng thái đơn hàng từ popup
    fun notifyUserOrderStatus(order: OrderModel, newStatus: String) {
        val (title, body, type) = when (newStatus) {
            "SHIPPING"  -> Triple("Đơn hàng đang vận chuyển 🚚", "Đơn #${order.id.take(8).uppercase()} đang trên giao đến bạn.", "SHIPPING")
            "DELIVERED" -> Triple("Đơn hàng đã giao thành công ✅", "Đơn #${order.id.take(8).uppercase()} đã giao thành công. Cảm ơn bạn!", "DELIVERED")
            "CANCELLED" -> Triple("Đơn hàng đã bị hủy ❌", "Đơn #${order.id.take(8).uppercase()} đã bị hủy.", "CANCELLED")
            else -> Triple("Cập nhật đơn hàng", "Trạng thái mới: $newStatus", "ORDER_STATUS")
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
        db.collection("notifications").add(notif)
    }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                    Text(
                        text = "Thông báo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (unreadCount > 0) {
                            TextButton(onClick = { markAllAsRead() }) {
                                Text("Đọc tất cả", fontSize = 13.sp)
                            }
                        }
                        if (notifications.isNotEmpty()) {
                            IconButton(onClick = {
                                notifications.forEach {
                                    db.collection("notifications").document(it.id).delete()
                                        .addOnFailureListener { e -> com.example.easyshop.AppUtil.showError("Lỗi xóa: ${e.message}") }
                                }
                                notifications = emptyList() // Update UI ngay lập tức
                                com.example.easyshop.AppUtil.showSuccess("Đã xóa tất cả thông báo")
                            }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Xóa tất cả", tint = Color.Red.copy(alpha=0.7f))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                notifications.isEmpty() -> {
                    EmptyNotificationState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Header thống kê nhanh
                        item {
                            AdminNotifSummaryHeader(
                                totalCount = notifications.size,
                                unreadCount = unreadCount
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        items(notifications, key = { it.id }) { notif ->
                            NotificationItem(
                                notif = notif,
                                onClick = {
                                    markAsRead(notif)
                                    notif.orderId?.let { orderId ->
                                        db.collection("orders").document(orderId).get().addOnSuccessListener { doc ->
                                            val order = doc.toObject(OrderModel::class.java)?.copy(id = doc.id)
                                            if (order != null) {
                                                selectedOrder = order
                                            } else {
                                                com.example.easyshop.AppUtil.showError("Đơn hàng không tồn tại!")
                                            }
                                        }
                                    }
                                },
                                onDelete = {
                                    db.collection("notifications").document(notif.id).delete()
                                        .addOnSuccessListener {
                                            com.example.easyshop.AppUtil.showSuccess("Đã xóa thông báo thành công")
                                        }
                                        .addOnFailureListener { e -> com.example.easyshop.AppUtil.showError("Lỗi xóa: ${e.message}") }
                                    notifications = notifications.filter { it.id != notif.id } // Update UI ngay lập tức
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        if (selectedOrder != null) {
            com.example.easyshop.admin.OrderDetailsDialog(
                order = selectedOrder!!,
                onDismiss = { selectedOrder = null },
                onUpdateStatus = { newStatus ->
                    db.collection("orders").document(selectedOrder!!.id).update("status", newStatus)
                        .addOnSuccessListener {
                            selectedOrder = selectedOrder!!.copy(status = newStatus)
                            notifyUserOrderStatus(selectedOrder!!, newStatus)
                            com.example.easyshop.AppUtil.showSuccess("Đã cập nhật trạng thái đơn hàng!")
                        }
                        .addOnFailureListener {
                            com.example.easyshop.AppUtil.showError("Cập nhật thất bại: ${it.message}")
                        }
                }
            )
        }
    }
}

// ─── Header thống kê nhanh ───────────────────────────────────────────────────
@Composable
private fun AdminNotifSummaryHeader(totalCount: Int, unreadCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryChip(
                value = totalCount.toString(),
                label = "Tổng cộng",
                icon = Icons.Rounded.Notifications,
                color = MaterialTheme.colorScheme.primary
            )
            VerticalDivider(
                modifier = Modifier.height(40.dp).padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(0.3f)
            )
            SummaryChip(
                value = unreadCount.toString(),
                label = "Chưa đọc",
                icon = Icons.Rounded.FiberNew,
                color = if (unreadCount > 0) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
            )
            VerticalDivider(
                modifier = Modifier.height(40.dp).padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(0.3f)
            )
            SummaryChip(
                value = (totalCount - unreadCount).toString(),
                label = "Đã đọc",
                icon = Icons.Rounded.DoneAll,
                color = Color(0xFF388E3C)
            )
        }
    }
}

@Composable
private fun SummaryChip(value: String, label: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Item thông báo ─────────────────────────────────────────────────────────
@Composable
private fun NotificationItem(notif: NotificationModel, onClick: () -> Unit, onDelete: () -> Unit) {
    val style = styleFor(notif.type)
    val bgColor by animateColorAsState(
        targetValue = if (notif.isRead) Color.Transparent else style.bgColor.copy(alpha = 0.35f),
        animationSpec = tween(400),
        label = "bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon tròn có màu
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(style.bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(24.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notif.title,
                    fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Dot chưa đọc
                if (!notif.isRead) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = notif.body,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            // Thời gian + Badge type + Nút xóa
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatRelativeTime(notif.createdAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NotifTypeBadge(type = notif.type, style = style)
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Xóa",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotifTypeBadge(type: String, style: NotifStyle) {
    val label = when (type) {
        "NEW_ORDER"    -> "Đơn mới"
        "SHIPPING"     -> "Vận chuyển"
        "DELIVERED"    -> "Đã giao"
        "CANCELLED"    -> "Đã hủy"
        "ORDER_STATUS" -> "Cập nhật"
        "PROMO"        -> "Khuyến mãi"
        else           -> "Hệ thống"
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = style.bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = style.color
        )
    }
}

// ─── Empty state ─────────────────────────────────────────────────────────────
@Composable
private fun EmptyNotificationState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Chưa có thông báo nào",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Khi có đơn mới hoặc cập nhật trạng thái,\nthông báo sẽ hiện ở đây.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
