package com.example.easyshop.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.easyshop.R
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import coil.compose.AsyncImage
import com.example.easyshop.model.CategoryModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var categories by remember { mutableStateOf<List<CategoryModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryModel?>(null) }

    var catId by remember { mutableStateOf("") }
    var catName by remember { mutableStateOf("") }
    var catImageUrl by remember { mutableStateOf("") }

    val firestore = Firebase.firestore
    val categoriesCollection = firestore.collection("data").document("stock").collection("categories")
    var productCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    fun loadCategories() {
        isLoading = true
        categoriesCollection.get().addOnSuccessListener { result ->
            categories = result.documents.mapNotNull { it.toObject(CategoryModel::class.java) }.sortedBy { it.name }
            firestore.collection("data").document("stock").collection("products").get()
                .addOnSuccessListener { productResult ->
                    productCounts = productResult.documents.mapNotNull { it.getString("category") }.groupingBy { it }.eachCount()
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        }.addOnFailureListener { isLoading = false }
    }

    LaunchedEffect(key1 = Unit) { loadCategories() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.product_categories_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back_to_home))
                    }
                },
                actions = {
                    IconButton(onClick = { loadCategories() }) { Icon(Icons.Default.Refresh, stringResource(id = R.string.reset)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { catId = ""; catName = ""; catImageUrl = ""; showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, stringResource(id = R.string.add_new_category_title))
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(padding),
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
                            Icon(Icons.Default.Category, null, Modifier.size(64.dp), MaterialTheme.colorScheme.outlineVariant)
                            Text(stringResource(id = R.string.no_categories_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(items = categories, key = { it.id }) { category ->
                        CategoryItem(
                            category = category,
                            productCount = productCounts[category.id] ?: 0,
                            onEdit = { catId = category.id; catName = category.name; catImageUrl = category.imageUrl; editingCategory = category },
                            onDelete = { categoriesCollection.document(category.id).delete().addOnSuccessListener { loadCategories() } }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryFormDialog(
            title = stringResource(id = R.string.add_new_category_title), id = catId, name = catName, imageUrl = catImageUrl, isEdit = false,
            onIdChange = { catId = it }, onNameChange = { catName = it }, onImageUrlChange = { catImageUrl = it },
            onDismiss = { showAddDialog = false },
            onConfirm = {
                if (catId.isNotBlank() && catName.isNotBlank()) {
                    val category = CategoryModel(catId.trim().lowercase(), catImageUrl.trim(), catName.trim())
                    categoriesCollection.document(category.id).set(category).addOnSuccessListener { showAddDialog = false; loadCategories() }
                }
            }
        )
    }

    if (editingCategory != null) {
        CategoryFormDialog(
            title = stringResource(id = R.string.edit_category_title), id = catId, name = catName, imageUrl = catImageUrl, isEdit = true,
            onIdChange = { catId = it }, onNameChange = { catName = it }, onImageUrlChange = { catImageUrl = it },
            onDismiss = { editingCategory = null },
            onConfirm = {
                if (catName.isNotBlank()) {
                    val category = CategoryModel(catId, catImageUrl.trim(), catName.trim())
                    categoriesCollection.document(catId).set(category).addOnSuccessListener { editingCategory = null; loadCategories() }
                }
            }
        )
    }
}

@Composable
fun CategoryFormDialog(
    title: String, id: String, name: String, imageUrl: String, isEdit: Boolean,
    onIdChange: (String) -> Unit, onNameChange: (String) -> Unit, onImageUrlChange: (String) -> Unit,
    onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = id, onValueChange = onIdChange, label = { Text(stringResource(id = R.string.category_id_label)) },
                    placeholder = { Text(stringResource(id = R.string.category_id_hint)) }, singleLine = true, enabled = !isEdit,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text(stringResource(id = R.string.display_name_label)) },
                    placeholder = { Text(stringResource(id = R.string.display_name_hint)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = imageUrl, onValueChange = onImageUrlChange, label = { Text(stringResource(id = R.string.category_image_url_label)) },
                    placeholder = { Text("https://...") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            }
        },
        confirmButton = { Button(onClick = onConfirm, shape = RoundedCornerShape(12.dp)) { Text(if (isEdit) stringResource(id = R.string.update_btn) else stringResource(id = R.string.add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } }
    )
}

@Composable
fun CategoryItem(category: CategoryModel, productCount: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = category.imageUrl, contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ID: ${category.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                        Text(stringResource(id = R.string.products_count_label, productCount), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(id = R.string.delete_category_title)) },
            text = { Text(stringResource(id = R.string.delete_category_confirm_msg, category.name)) },
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(id = R.string.delete_category_title)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(id = R.string.cancel)) } }
        )
    }
}
