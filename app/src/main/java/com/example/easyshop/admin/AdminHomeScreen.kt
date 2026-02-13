package com.example.easyshop.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.easyshop.R
import coil.compose.AsyncImage
import com.example.easyshop.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.easyshop.AppUtil
import com.example.easyshop.model.CategoryModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    var products by remember { mutableStateOf<List<ProductModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductModel?>(null) }
    var categories by remember { mutableStateOf<List<CategoryModel>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val firestore = Firebase.firestore

    fun loadProducts() {
        isLoading = true
        firestore.collection("data").document("stock").collection("products").get()
            .addOnSuccessListener { result ->
                products = result.documents.mapNotNull { doc ->
                    doc.toObject(ProductModel::class.java)?.apply { id = doc.id }
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    LaunchedEffect(key1 = Unit) {
        loadProducts()
        firestore.collection("data").document("stock").collection("categories").get()
            .addOnSuccessListener { result ->
                categories = result.documents.mapNotNull { it.toObject(CategoryModel::class.java) }.sortedBy { it.name }
            }
    }

    fun deleteProduct(product: ProductModel) {
        scope.launch {
            try {
                firestore.collection("data").document("stock").collection("products").document(product.id).delete().await()
                loadProducts()
                showDeleteDialog = false
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.manage_products_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back_to_home))
                    }
                },
                actions = {
                    IconButton(onClick = { loadProducts() }) {
                        Icon(Icons.Default.Refresh, stringResource(id = R.string.reset))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                products.isEmpty() -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShoppingCart, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(id = R.string.no_products_yet), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    val filteredProducts = remember(products, selectedCategory) {
                        if (selectedCategory == null) products else products.filter { it.category == selectedCategory }
                    }

                    Column(Modifier.fillMaxSize()) {
                        // Category Chips
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text(stringResource(id = R.string.all_filter)) },
                                    leadingIcon = if (selectedCategory == null) { { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) } } else null,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            items(items = categories, key = { it.id }) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat.id,
                                    onClick = { selectedCategory = cat.id },
                                    label = { Text(cat.name) },
                                    leadingIcon = if (selectedCategory == cat.id) { { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) } } else null,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        if (filteredProducts.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(id = R.string.no_products_in_category), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items = filteredProducts, key = { it.id }) { product ->
                                    AdminProductItem(
                                        product = product,
                                        onEdit = { navController.navigate("edit_product/${product.id}") },
                                        onViewDetails = { navController.navigate("product-details/${product.id}") },
                                        onDelete = { productToDelete = product; showDeleteDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && productToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(id = R.string.delete_product_title)) },
            text = { Text(stringResource(id = R.string.delete_product_confirm_msg, productToDelete?.title ?: "")) },
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                TextButton(
                    onClick = { productToDelete?.let { deleteProduct(it) } },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(id = R.string.delete_product_title)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun AdminProductItem(
    product: ProductModel,
    onEdit: () -> Unit,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onViewDetails
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.size(80.dp), shape = RoundedCornerShape(12.dp)) {
                if (product.images.isNotEmpty()) {
                    AsyncImage(model = product.images.first(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingCart, null) }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(product.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(AppUtil.formatPrice(product.actualPrice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    if (product.actualPrice != product.price) {
                        Text(AppUtil.formatPrice(product.price), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    color = if (product.inStock) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (product.inStock) stringResource(id = R.string.in_stock) else stringResource(id = R.string.out_of_stock),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.inStock) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(id = R.string.edit_product)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}