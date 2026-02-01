package com.example.easyshop.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.example.easyshop.model.AdminMenuItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var adminName by remember { mutableStateOf("Admin") }
    var totalProducts by remember { mutableStateOf(0) }
    var totalOrders by remember { mutableStateOf(0) }
    var pendingOrders by remember { mutableStateOf(0) }
    var totalUsers by remember { mutableStateOf(0) }
    var totalCategories by remember { mutableStateOf(0) }

    val firestore = Firebase.firestore
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Load stats
    LaunchedEffect(key1 = Unit) {
        // Get admin name
        currentUser?.let { user ->
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    adminName = doc.getString("name") ?: "Admin"
                }
        }

        // Count products
        firestore.collection("data").document("stock")
            .collection("products")
            .get()
            .addOnSuccessListener { result ->
                totalProducts = result.size()
            }

        // Count orders
        firestore.collection("orders")
            .get()
            .addOnSuccessListener { result ->
                totalOrders = result.size()
                pendingOrders = result.documents.count {
                    it.getString("status") == "ORDERED"
                }
            }

        // Count only customers (including those with default "user" role)
        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                totalUsers = result.documents.count { doc ->
                    val role = doc.getString("role") ?: "user"
                    role == "user"
                }
            }

        // Count categories
        firestore.collection("data").document("stock")
            .collection("categories")
            .get()
            .addOnSuccessListener { result ->
                totalCategories = result.size()
            }
    }

    val menuItems = listOf(
        AdminMenuItem(
            "Manage Products",
            Icons.Default.Inventory,
            "admin-products",
            Color(0xFF2196F3),
            totalProducts
        ),
        AdminMenuItem(
            "Add Product",
            Icons.Default.Add,
            "add-product",
            Color(0xFF4CAF50)
        ),
        AdminMenuItem(
            "Orders",
            Icons.Default.ShoppingBag,
            "orders-management",
            Color(0xFFFF9800),
            pendingOrders
        ),
        AdminMenuItem(
            "Categories",
            Icons.Default.Category,
            "manage-categories",
            Color(0xFF9C27B0),
            totalCategories
        ),
        AdminMenuItem(
            "Analytics",
            Icons.Default.Analytics,
            "analytics",
            Color(0xFF00BCD4)
        ),
        AdminMenuItem(
            "Users",
            Icons.Default.People,
            "manage-users",
            Color(0xFFE91E63),
            totalUsers
        )
    )

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
                // Spacer or Admin Title
                Text(
                    text = "Admin Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp)
                )

                IconButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("auth") {
                        popUpTo("admin-dashboard") { inclusive = true }
                    }
                }) {
                    Icon(Icons.Default.ExitToApp, "Logout")
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = adminName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            item { Spacer(Modifier.height(8.dp)) }

            item { Spacer(Modifier.height(32.dp)) }

            // Quick Actions Title
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Menu Grid
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    items(menuItems) { item ->
                        AdminMenuCard(
                            item = item,
                            onClick = {
                                navController.navigate(item.route)
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AdminMenuCard(
    item: AdminMenuItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Badge for count
            if (item.count != null && item.count > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    color = Color(0xFFFF5252),
                    shape = CircleShape
                ) {
                    Text(
                        text = item.count.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    item.color.copy(alpha = 0.2f),
                                    item.color.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = item.color,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
            }
        }
    }
}