package com.example.easyshop.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.easyshop.AppUtil
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

import coil.compose.AsyncImage
import com.example.easyshop.R
import com.example.easyshop.model.CategoryModel

@Composable
fun ManageCategoriesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var categories by remember { mutableStateOf<List<CategoryModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryModel?>(null) }

    // Form state
    var catId by remember { mutableStateOf("") }
    var catName by remember { mutableStateOf("") }
    var catImageUrl by remember { mutableStateOf("") }

    val firestore = Firebase.firestore
    val categoriesCollection = firestore.collection("data").document("stock").collection("categories")

    var productCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    fun loadCategories() {
        isLoading = true
        categoriesCollection.get().addOnSuccessListener { result ->
            val cats = result.documents.mapNotNull { it.toObject(CategoryModel::class.java) }
                .sortedBy { it.name }
            categories = cats

            // Fetch product counts for each category
            firestore.collection("data").document("stock")
                .collection("products")
                .get()
                .addOnSuccessListener { productResult ->
                    val counts = productResult.documents
                        .mapNotNull { it.getString("category") }
                        .groupingBy { it }
                        .eachCount()
                    productCounts = counts
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        }.addOnFailureListener {
            isLoading = false
        }
    }

    LaunchedEffect(key1 = Unit) {
        loadCategories()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.product_categories_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(id = R.string.back_to_home))
                    }
                },
                actions = {
                    IconButton(onClick = { loadCategories() }) {
                        Icon(Icons.Default.Refresh, stringResource(id = R.string.reset))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    catId = ""
                    catName = ""
                    catImageUrl = ""
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, stringResource(id = R.string.add_new_category_title))
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        stringResource(id = R.string.categories_overview_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (categories.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 100.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Category, null, Modifier.size(64.dp), Color.LightGray)
                            Text(stringResource(id = R.string.no_categories_found), color = Color.Gray)
                        }
                    }
                } else {
                    items(
                        items = categories,
                        key = { it.id }
                    ) { category ->
                        CategoryItem(
                            category = category,
                            productCount = productCounts[category.id] ?: 0,
                            onEdit = {
                                catId = category.id
                                catName = category.name
                                catImageUrl = category.imageUrl
                                editingCategory = category
                            },
                            onDelete = {
                                categoriesCollection.document(category.id).delete()
                                    .addOnSuccessListener { loadCategories() }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        CategoryFormDialog(
            title = stringResource(id = R.string.add_new_category_title),
            id = catId,
            name = catName,
            imageUrl = catImageUrl,
            isEdit = false,
            onIdChange = { catId = it },
            onNameChange = { catName = it },
            onImageUrlChange = { catImageUrl = it },
            onDismiss = { showAddDialog = false },
            onConfirm = {
                if (catId.isNotBlank() && catName.isNotBlank()) {
                    val category = CategoryModel(catId.trim().lowercase(), catImageUrl.trim(), catName.trim())
                    categoriesCollection.document(category.id).set(category)
                        .addOnSuccessListener {
                            showAddDialog = false
                            loadCategories()
                        }
                }
            }
        )
    }

    // Edit Dialog
    if (editingCategory != null) {
        CategoryFormDialog(
            title = stringResource(id = R.string.edit_category_title),
            id = catId,
            name = catName,
            imageUrl = catImageUrl,
            isEdit = true,
            onIdChange = { catId = it },
            onNameChange = { catName = it },
            onImageUrlChange = { catImageUrl = it },
            onDismiss = { editingCategory = null },
            onConfirm = {
                if (catName.isNotBlank()) {
                    val category = CategoryModel(catId, catImageUrl.trim(), catName.trim())
                    categoriesCollection.document(catId).set(category)
                        .addOnSuccessListener {
                            editingCategory = null
                            loadCategories()
                        }
                }
            }
        )
    }
}

@Composable
fun CategoryFormDialog(
    title: String,
    id: String,
    name: String,
    imageUrl: String,
    isEdit: Boolean,
    onIdChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = id,
                    onValueChange = onIdChange,
                    label = { Text(stringResource(id = R.string.category_id_label)) },
                    placeholder = { Text(stringResource(id = R.string.category_id_hint)) },
                    singleLine = true,
                    enabled = !isEdit, // Don't allow changing ID when editing
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(id = R.string.display_name_label)) },
                    placeholder = { Text(stringResource(id = R.string.display_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = onImageUrlChange,
                    label = { Text(stringResource(id = R.string.category_image_url_label)) },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(if (isEdit) stringResource(id = R.string.update_btn) else stringResource(id = R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun CategoryItem(
    category: CategoryModel,
    productCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = category.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                placeholder = painterResource(id = com.example.easyshop.R.drawable.login_banner)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ID: ${category.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(stringResource(id = R.string.products_count_label, productCount), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, stringResource(id = R.string.edit_product), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, stringResource(id = R.string.cancel_order), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(id = R.string.delete_category_title)) },
            text = { Text(stringResource(id = R.string.delete_category_confirm_msg, category.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.delete_category_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

