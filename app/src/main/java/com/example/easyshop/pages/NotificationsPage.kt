package com.example.easyshop.pages

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.easyshop.AppUtil
import com.example.easyshop.components.PromoSection
import com.example.easyshop.model.NotificationModel
import com.example.easyshop.model.PromoCodeModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsPage(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    var notifications by remember { mutableStateOf<List<NotificationModel>>(emptyList()) }
    var promos by remember { mutableStateOf<List<PromoCodeModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var unreadCount by remember { mutableIntStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Load data from Firestore
    LaunchedEffect(uid) {
        if (uid != null) {
            val listener = db.collection("notifications")
                .whereEqualTo("recipientRole", "user")
                .whereIn("userId", listOf(uid, "broadcast"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    isLoading = false
                    if (error != null) {
                        android.util.Log.e("NotificationsPage", "Query error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull {
                            val notif = it.toObject(NotificationModel::class.java)
                            notif?.copy(id = it.id)
                        }
                        notifications = list
                        unreadCount = notifications.count { !it.isRead }
                    }
                }
        } else {
            isLoading = false
        }

        // Load promos
        db.collection("promoCodes")
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { snapshot ->
                promos = snapshot.documents.mapNotNull { it.toObject(PromoCodeModel::class.java) }
            }
    }

    // Function to save promo code
    fun savePromoCode(code: String) {
        if (uid == null) {
            AppUtil.showToast(context, "Vui lòng đăng nhập để lưu mã")
            return
        }
        db.collection("users").document(uid)
            .update("savedPromoCodes", FieldValue.arrayUnion(code))
            .addOnSuccessListener {
                AppUtil.showSuccess("Đã lưu mã giảm giá vào ví!")
            }
            .addOnFailureListener {
                AppUtil.showError("Lỗi", "Không thể lưu mã giảm giá")
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
        val unread = notifications.filter { !it.isRead }
        if (unread.isEmpty()) return
        val batch = db.batch()
        unread.forEach { notif ->
            batch.update(db.collection("notifications").document(notif.id), "isRead", true)
        }
        batch.commit()
    }

    // Auto-clear: khi danh sách load xong và có tin chưa đọc → tự đánh dấu sau 1.5s
    LaunchedEffect(notifications) {
        val hasUnread = notifications.any { !it.isRead }
        if (hasUnread && !isLoading) {
            delay(1500)
            markAllAsRead()
        }
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
                                val batch = db.batch()
                                notifications.forEach {
                                    batch.delete(db.collection("notifications").document(it.id))
                                }
                                batch.commit().addOnSuccessListener {
                                    notifications = emptyList()
                                    com.example.easyshop.AppUtil.showSuccess("Đã xóa tất cả thông báo")
                                }
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
                        item {
                            UserNotifSummaryHeader(
                                totalCount = notifications.size,
                                unreadCount = unreadCount
                            )
                            Spacer(Modifier.height(16.dp))
                        }

                        // Promo Section in Notifications
                        item {
                            if (promos.isNotEmpty()) {
                                PromoSection(
                                    promos = promos,
                                    onCollect = { code -> savePromoCode(code) }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        items(notifications, key = { it.id }) { notif ->
                            NotificationItem(
                                notif = notif,
                                onClick = {
                                    markAsRead(notif)
                                // Chuyển hướng tới chi tiết đơn nếu có orderId
                                    if (!notif.orderId.isNullOrEmpty()) {
                                        navController.navigate("order-details/${notif.orderId}")
                                    }
                                },
                                onDelete = {
                                    db.collection("notifications").document(notif.id).delete().addOnSuccessListener {
                                        com.example.easyshop.AppUtil.showSuccess("Đã xóa thông báo thành công")
                                    }
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
    }
}

// ─── Header thống kê nhanh ───────────────────────────────────────────────────
@Composable
private fun UserNotifSummaryHeader(totalCount: Int, unreadCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.6f)
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
            // Thời gian + Badge type + Nút xoá
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
        "ORDER_STATUS" -> "Cập nhật đơn"
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
            "Khi có sự lựa chọn từ cửa hàng hoặc đơn hàng,\nthông báo sẽ hiển thị ở đây.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}
