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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.example.easyshop.model.AdminMenuItem

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

    LaunchedEffect(key1 = Unit) {
        currentUser?.let { user ->
            firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc -> adminName = doc.getString("name") ?: "Admin" }
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Welcome Header
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            stringResource(id = R.string.welcome_back),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            adminName,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(16.dp))
                        // Stat row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatChip("📦", "$totalOrders", stringResource(id = R.string.orders))
                            StatChip("⏳", "$pendingOrders", "Pending")
                            StatChip("👥", "$totalUsers", stringResource(id = R.string.users))
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

            // Menu Grid
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(800.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    items(menuItems) { item ->
                        AdminMenuCard(item = item, onClick = { navController.navigate(item.route) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun AdminMenuCard(item: AdminMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.count != null && item.count > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
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
                modifier = Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(item.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, null, tint = item.color, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}