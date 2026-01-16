package com.example.easyshop.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    modifier: Modifier = Modifier,
    navController: NavController  // ✅ Chỉ giữ 2 parameters này
) {
    var products by remember { mutableStateOf<List<ProductModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductModel?>(null) }

    val scope = rememberCoroutineScope()
    val firestore = Firebase.firestore

    // Load products
    fun loadProducts() {
        isLoading = true
        firestore.collection("data").document("stock")
            .collection("products")
            .get()
            .addOnSuccessListener { result ->
                products = result.documents.mapNotNull { doc ->
                    doc.toObject(ProductModel::class.java)?.apply { id = doc.id }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    LaunchedEffect(Unit) {
        loadProducts()
    }

    // Delete product
    fun deleteProduct(product: ProductModel) {
        scope.launch {
            try {
                firestore.collection("data").document("stock")
                    .collection("products")
                    .document(product.id)
                    .delete()
                    .await()
                loadProducts()
                showDeleteDialog = false
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin - Manage Products") },
                actions = {
                    IconButton(onClick = { loadProducts() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = {
                        Firebase.auth.signOut()
                        navController.navigate("auth") {
                            popUpTo("admin-home") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, "Logout")
                    }
                }
            )

        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("add-product") },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Product") }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                products.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No products yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(products) { product ->
                            AdminProductItem(
                                product = product,
                                navController = navController,
                                onEdit = {
                                    // ✅ Navigate đến EDIT PAGE khi click icon bút
                                    navController.navigate("edit_product/${product.id}")
                                },
                                onViewDetails = {
                                    // ✅ Navigate đến DETAILS PAGE khi click vào Card
                                    navController.navigate("product-details/${product.id}")
                                },
                                onDelete = {
                                    productToDelete = product
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && productToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Product?") },
            text = { Text("Are you sure you want to delete \"${productToDelete?.title}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { productToDelete?.let { deleteProduct(it) } },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
private fun AdminProductItem(
    product: ProductModel,
    navController: NavController,
    onEdit: () -> Unit,           // ✅ Cho icon Edit
    onViewDetails: () -> Unit,    // ✅ Thêm callback này cho Card
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onViewDetails  // ✅ Click Card -> xem chi tiết
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product Image
            Card(
                modifier = Modifier.size(80.dp)
            ) {
                if (product.images.isNotEmpty()) {
                    AsyncImage(
                        model = product.images.first(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingCart, null)
                    }
                }
            }

            // Product Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )

                Text(
                    text = product.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$${product.actualPrice}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (product.actualPrice != product.price) {
                        Text(
                            text = "$${product.price}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stock status
                Surface(
                    color = if (product.inStock)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (product.inStock) "In Stock" else "Out of Stock",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.inStock)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ✅ Icon Edit - Navigate đến EditProductPage
                IconButton(
                    onClick = onEdit  // ✅ Gọi onEdit khi click icon bút
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }

                // ✅ Icon Delete
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}