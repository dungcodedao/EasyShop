package com.example.easyshop.admin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.R
import com.example.easyshop.model.AdminMenuItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var adminName by remember { mutableStateOf("Admin") }
    var adminAvatar by remember { mutableStateOf("") } // <-- Thêm biến lưu link ảnh
    var totalProducts by remember { mutableStateOf(0) }
    var totalOrders by remember { mutableStateOf(0) }
    var pendingOrders by remember { mutableStateOf(0) }
    var totalUsers by remember { mutableStateOf(0) }
    var totalCategories by remember { mutableStateOf(0) }
    var unreadNotifCount by remember { mutableIntStateOf(0) }
    var unreadChatCount by remember { mutableIntStateOf(0) }

    var showExitDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.activity.compose.BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        com.example.easyshop.screen.ExitConfirmationDialog(
            onConfirm = { (context as? android.app.Activity)?.finish() },
            onDismiss = { showExitDialog = false }
        )
    }

    val firestore = Firebase.firestore
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Lắng nghe dữ liệu realtime (Thoát màn hình thì ngắt kết nối để tránh memory leak)
    DisposableEffect(key1 = Unit) {
        val notifListener = firestore.collection("notifications")
            .whereEqualTo("recipientRole", "admin")
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snap, _ ->
                unreadNotifCount = snap?.size() ?: 0
            }

        val chatListener = firestore.collection("chats")
            .whereGreaterThan("unreadCountByAdmin", 0)
            .addSnapshotListener { snap, _ ->
                unreadChatCount = snap?.size() ?: 0
            }

        onDispose {
            notifListener.remove()
            chatListener.remove()
        }
    }

    LaunchedEffect(key1 = Unit) {
        currentUser?.let { user ->
            firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc -> 
                    val role = doc.getString("role") ?: "user"
                    if (role != "admin") {
                        // ❌ Không phải Admin! Đẩy ra ngoài ngay lập tức
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        adminName = doc.getString("name") ?: "Admin" 
                        adminAvatar = doc.getString("profileImg") ?: ""
                    }
                }
                .addOnFailureListener {
                    navController.navigate("auth") { popUpTo(0) { inclusive = true } }
                }
        } ?: run {
            navController.navigate("auth") { popUpTo(0) { inclusive = true } }
        }
        firestore.collection("data").document("stock").collection("products").get()
            .addOnSuccessListener { totalProducts = it.size() }
        firestore.collection("orders").get()
            .addOnSuccessListener { result ->
                totalOrders = result.size()
                pendingOrders = result.documents.count { it.getString("status") == "ORDERED" }
            }
        firestore.collection("users").get()
            .addOnSuccessListener { result ->
                totalUsers = result.documents.count { (it.getString("role") ?: "user") == "user" }
            }
        firestore.collection("data").document("stock").collection("categories").get()
            .addOnSuccessListener { totalCategories = it.size() }
    }

    val menuItems = listOf(
        AdminMenuItem(stringResource(id = R.string.manage_products), Icons.Default.Inventory, "admin-products", Color(0xFF2196F3), totalProducts),
        AdminMenuItem(stringResource(id = R.string.add_product_action), Icons.Default.Add, "add-product", Color(0xFF4CAF50)),
        AdminMenuItem(stringResource(id = R.string.orders), Icons.Default.ShoppingBag, "orders-management", Color(0xFFFF9800), pendingOrders),
        AdminMenuItem(stringResource(id = R.string.categories), Icons.Default.Category, "manage-categories", Color(0xFF9C27B0), totalCategories),
        AdminMenuItem(stringResource(id = R.string.analytics), Icons.Default.Analytics, "analytics", Color(0xFF00BCD4)),
        AdminMenuItem(stringResource(id = R.string.admin_chat_title), Icons.AutoMirrored.Filled.Message, "admin-chat-list", Color(0xFF2E7D32), unreadChatCount),
        AdminMenuItem(stringResource(id = R.string.users), Icons.Default.People, "manage-users", Color(0xFFE91E63), totalUsers),
        AdminMenuItem(stringResource(id = R.string.promo_codes), Icons.Default.ConfirmationNumber, "manage-promo-codes", Color(0xFF673AB7))
    )

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.admin_dashboard_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 🔔 Nút chuông thông báo
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge { Text(if (unreadNotifCount > 99) "99+" else unreadNotifCount.toString()) }
                                }
                            }
                        ) {
                            FilledTonalIconButton(
                                onClick = { navController.navigate("admin-notifications") },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Thông báo")
                            }
                        }
                        FilledTonalIconButton(
                            onClick = {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("auth") { popUpTo("admin-dashboard") { inclusive = true } }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, stringResource(id = R.string.logout))
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Welcome Header — Premium Gradient Design
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6),
                                        Color(0xFFA855F7)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        stringResource(id = R.string.welcome_back),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        adminName.replace("Admin", "Quản trị viên"),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                // Admin avatar circle
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable { navController.navigate("admin_profile") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (adminAvatar.isNotEmpty()) {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                .data(adminAvatar)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Admin Avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // Stat cards row — glassmorphism style
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.ShoppingBag,
                                    value = "$totalOrders",
                                    label = stringResource(id = R.string.orders),
                                    onClick = { navController.navigate("orders-management") }
                                )
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Schedule,
                                    value = "$pendingOrders",
                                    label = "Chờ xử lý",
                                    onClick = { navController.navigate("orders-management") }
                                )
                                StatCard(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.People,
                                    value = "$totalUsers",
                                    label = stringResource(id = R.string.users),
                                    onClick = { navController.navigate("manage-users") }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Title
            item {
                Text(
                    text = stringResource(id = R.string.quick_actions),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Menu Grid — Redesigned to be flexible and scrollable
            menuItems.chunked(2).forEach { rowItems ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            AdminMenuCard(
                                item = item,
                                onClick = { navController.navigate(item.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Add spacer if only 1 item in row to keep alignment
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp)) // <--- Đổi từ hình tròn sang bo góc cho chuẩn dàn ở dưới
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AdminMenuCard(
    item: AdminMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth() // Dùng fillMaxWidth thay vì cứng aspectRatio để linh hoạt
            .height(130.dp) // Set chiều cao tối thiểu đủ chỗ cho icon và chữ 2 dòng
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp) // Tăng độ bo góc thẻ
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.count != null && item.count > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape
                ) {
                    Text(
                        text = item.count.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp) // Giảm kích thước nền xuống tỉ lệ an toàn
                        .clip(RoundedCornerShape(16.dp))
                        .background(item.color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        null,
                        tint = item.color,
                        modifier = Modifier.size(32.dp) // Kích thước Icon phải NHỎ HƠN kích thước nền (56dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge, // Đổi thành labelLarge để nhỏ hơn chút, dễ xếp dòng
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    minLines = 2, // Đảm bảo luôn chiếm 2 dòng để các thẻ bằng nhau
                    maxLines = 2  // Cho phép xuống dòng tránh bị cắt chữ
                )
            }
        }
    }
}
