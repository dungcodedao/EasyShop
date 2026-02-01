package com.example.easyshop.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Dùng dấu * để lấy tất cả: Check, Person, Search...
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.easyshop.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
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

    fun loadUsers() {
        isLoading = true
        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                users = result.documents.mapNotNull { it.toObject(UserModel::class.java) }
                isLoading = false
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
                title = { Text("Manage Users", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                placeholder = { Text("Search by name or email") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredUsers) { user ->
                        UserListItem(user, onClick = {
                            selectedUser = user
                            showBottomSheet = true
                        })
                        Divider(
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
    onRoleChange: (String) -> Unit
) {
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
            text = user.name.ifEmpty { "No Name" },
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
        DetailInfoRow("User ID", user.uid)
        DetailInfoRow("Address", user.address.ifEmpty { "No address set" })
        DetailInfoRow("Cart Items", "${user.cartItems.size} items")
        DetailInfoRow("Current Role", user.role.uppercase())

        Spacer(Modifier.height(32.dp))

        // Action Buttons
        Text(
            text = "Change Role",
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
                Text("Make User")
            }

            Button(
                onClick = { onRoleChange("admin") },
                modifier = Modifier.weight(1f),
                enabled = user.role != "admin",
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE3F2FD),
                    contentColor = Color(0xFF1976D2)
                )
            ) {
                Text("Make Admin")
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
        Divider(Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun UserListItem(user: UserModel, onClick: () -> Unit) {
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
                text = user.name.ifEmpty { "No Name" },
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
                    maxLines = 1
                )
            }
        }

        // Role Badge
        Surface(
            color = if (user.role == "admin") Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = user.role.uppercase(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (user.role == "admin") Color(0xFF1976D2) else Color(0xFF757575),
                letterSpacing = 0.5.sp
            )
        }
    }
}