package com.example.easyshop.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.* // Dùng dấu * để lấy tất cả: Check, Person, Search...
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.easyshop.R
import com.example.easyshop.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.example.easyshop.model.OrderModel
import java.text.NumberFormat
import java.util.Locale

data class UserStats(val orderCount: Int, val totalSpent: Double)

@Composable
fun ManageUsersScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var users by remember { mutableStateOf<List<UserModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedUser by remember { mutableStateOf<UserModel?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val firestore = Firebase.firestore

    var userStats by remember { mutableStateOf<Map<String, UserStats>>(emptyMap()) }


    fun loadUsers() {
        isLoading = true
        firestore.collection("users")
            .get()
            .addOnSuccessListener { userResult ->
                val usersList = userResult.documents.mapNotNull { it.toObject(UserModel::class.java) }
                
                // Fetch all orders to calculate stats per user
                firestore.collection("orders").get().addOnSuccessListener { orderResult ->
                    val ordersList = orderResult.documents.mapNotNull { it.toObject(OrderModel::class.java) }
                    
                    val statsMap = mutableMapOf<String, UserStats>()
                    ordersList.forEach { order ->
                        val current = statsMap[order.userId] ?: UserStats(0, 0.0)
                        if (order.status != "CANCELLED") {
                            statsMap[order.userId] = UserStats(
                                orderCount = current.orderCount + 1,
                                totalSpent = current.totalSpent + order.total
                            )
                        } else {
                            // Still count canceled orders but clarify in logic if needed
                            // For now, let's only count non-canceled for Spent
                        }
                    }
                    
                    userStats = statsMap
                    users = usersList
                    isLoading = false
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    LaunchedEffect(key1 = Unit) {
        loadUsers()
    }

    val filteredUsers = users.filter { it.role == "user" }.let { list ->
        if (searchQuery.isEmpty()) {
            list
        } else {
            list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.email.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.manage_users_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_to_home))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(id = R.string.search_users_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.no_users_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = filteredUsers,
                        key = { it.uid }
                    ) { user: UserModel ->
                        val stats = userStats[user.uid] ?: UserStats(0, 0.0)
                        UserListItem(user, stats, onClick = {
                            selectedUser = user
                            showBottomSheet = true
                        })
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }

        // User Detail Bottom Sheet
        if (showBottomSheet && selectedUser != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                UserDetailContent(
                    user = selectedUser!!,
                    stats = userStats[selectedUser!!.uid] ?: UserStats(0, 0.0),
                    onRoleChange = { newRole ->
                        firestore.collection("users").document(selectedUser!!.uid)
                            .update("role", newRole)
                            .addOnSuccessListener {
                                loadUsers()
                                showBottomSheet = false
                            }
                    }
                )
            }
        }
    }
}

@Composable
fun UserDetailContent(
    user: UserModel,
    stats: UserStats,
    onRoleChange: (String) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = user.name.ifEmpty { stringResource(id = R.string.no_name) },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Info Grid
        DetailInfoRow(stringResource(id = R.string.user_id_label), user.uid)
        DetailInfoRow(stringResource(id = R.string.address), user.address.ifEmpty { stringResource(id = R.string.no_address_set) })
        DetailInfoRow(stringResource(id = R.string.total_items), stringResource(id = R.string.orders_count_label, stats.orderCount))
        DetailInfoRow(stringResource(id = R.string.total_spent_label), currencyFormat.format(stats.totalSpent))
        DetailInfoRow(stringResource(id = R.string.current_role_label), user.role.uppercase())

        Spacer(Modifier.height(32.dp))

        // Action Buttons
        Text(
            text = stringResource(id = R.string.change_role_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onRoleChange("user") },
                modifier = Modifier.weight(1f),
                enabled = user.role != "user",
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(stringResource(id = R.string.make_user_btn))
            }

            Button(
                onClick = { onRoleChange("admin") },
                modifier = Modifier.weight(1f),
                enabled = user.role != "admin",
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(stringResource(id = R.string.make_admin_btn))
            }
        }
    }
}

@Composable
fun DetailInfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun UserListItem(user: UserModel, stats: UserStats, onClick: () -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Placeholder
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name.ifEmpty { stringResource(id = R.string.no_name) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (user.address.isNotEmpty()) {
                Text(
                    text = user.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(id = R.string.orders_count_label, stats.orderCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = currencyFormat.format(stats.totalSpent),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Role Badge
        Surface(
            color = if (user.role == "admin") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = user.role.uppercase(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (user.role == "admin") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
        }
    }
}