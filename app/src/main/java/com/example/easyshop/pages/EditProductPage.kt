package com.example.easyshop.pages
import com.example.easyshop.model.CategoryModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import com.example.easyshop.AppUtil
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun EditProductPage(
    productId: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var actualPrice by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var inStock by remember { mutableStateOf(true) }
    var specifications by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var showAddSpecDialog by remember { mutableStateOf(false) }
    var imageUrls by remember { mutableStateOf(listOf("")) }

    var isLoading by remember { mutableStateOf(false) }
    var isLoadingData by remember { mutableStateOf(true) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<CategoryModel>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val firestore = Firebase.firestore

    // Load categories
    LaunchedEffect(key1 = Unit) {
        firestore.collection("data").document("stock")
            .collection("categories")
            .get()
            .addOnSuccessListener { result ->
                categories = result.documents.mapNotNull { it.toObject(CategoryModel::class.java) }
            }
    }

    // Load existing product data
    LaunchedEffect(productId) {
        isLoadingData = true
        try {
            val doc = firestore.collection("data").document("stock")
                .collection("products")
                .document(productId)
                .get()
                .await()

            title = doc.getString("title") ?: ""
            description = doc.getString("description") ?: ""
            price = doc.getString("price") ?: ""
            actualPrice = doc.getString("actualPrice") ?: ""
            category = doc.getString("category") ?: ""
            inStock = doc.getBoolean("inStock") ?: true

            // Load images
            @Suppress("UNCHECKED_CAST")
            val imgs = doc.get("images") as? List<String> ?: emptyList()
            imageUrls = if (imgs.isEmpty()) listOf("") else imgs

            // Load specifications
            @Suppress("UNCHECKED_CAST")
            val specs = doc.get("otherDetails") as? Map<String, String> ?: emptyMap()
            specifications = specs.toList()

        } catch (e: Exception) {
            errorMessage = "${context.getString(R.string.failed_load_product)}: ${e.message}"
        } finally {
            isLoadingData = false
        }
    }

    // Update product
    fun updateProduct() {
        if (title.isBlank() || category.isBlank() || price.isBlank()) {
            errorMessage = context.getString(R.string.please_fill_all_fields)
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                // 1. Filter manual URLs
                val finalImageUrls = imageUrls.filter { it.isNotBlank() }

                if (finalImageUrls.isEmpty()) {
                    errorMessage = "Vui lòng thêm ít nhất 1 hình ảnh"
                    isLoading = false
                    return@launch
                }

                val otherDetails = specifications
                    .filter { it.first.isNotBlank() && it.second.isNotBlank() }
                    .toMap()

                val productData = hashMapOf(
                    "id" to productId,
                    "title" to title,
                    "description" to description,
                    "price" to price,
                    "actualPrice" to actualPrice.ifBlank { price },
                    "category" to category,
                    "inStock" to inStock,
                    "images" to finalImageUrls,
                    "otherDetails" to otherDetails
                )

                firestore.collection("data").document("stock")
                    .collection("products")
                    .document(productId)
                    .set(productData) // Hoặc dùng .update() nếu muốn chỉ update fields thay đổi
                    .await()

                showSuccessDialog = true

            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
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
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = stringResource(id = R.string.edit_product),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.size(40.dp))
            }
        }
        if (isLoadingData) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(id = R.string.loading_product))
                }
            }
        } else {
            // Main content - GIỐNG AddProductPage
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product Images Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = R.string.product_images),
                            style = MaterialTheme.typography.titleMedium
                        )
                        val totalImages = imageUrls.count { it.isNotBlank() }
                        Text(
                            "$totalImages/5",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Dán URL hình ảnh",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    imageUrls.forEachIndexed { index, url ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = url,
                                onValueChange = { newUrl ->
                                    imageUrls = imageUrls.toMutableList().apply {
                                        this[index] = newUrl
                                    }
                                },
                                label = { Text("URL hình ảnh ${index + 1}") },
                                placeholder = { Text("https://example.com/image.jpg") },
                                modifier = Modifier.weight(1f),
                                leadingIcon = {
                                    Icon(Icons.Default.Link, null,
                                        modifier = Modifier.size(20.dp))
                                }
                            )

                            if (imageUrls.size > 1) {
                                IconButton(
                                    onClick = {
                                        imageUrls = imageUrls.filterIndexed { i, _ -> i != index }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    if (imageUrls.size < 5) {
                        OutlinedButton(
                            onClick = { imageUrls = imageUrls + "" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Thêm URL")
                        }
                    }
                }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("${stringResource(id = R.string.product_title)} *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.product_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == category }?.name ?: category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("${stringResource(id = R.string.category)} *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    category = cat.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Specifications Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(id = R.string.specifications),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(id = R.string.items_count, specifications.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (specifications.isNotEmpty()) {
                            specifications.forEachIndexed { index, (key, value) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                key,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                value,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                specifications = specifications.filterIndexed { i, _ -> i != index }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showAddSpecDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.add_specification))
                        }
                    }
                }

                // Prices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("${stringResource(id = R.string.original_price)} *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text("đ") }
                    )

                    OutlinedTextField(
                        value = actualPrice,
                        onValueChange = { actualPrice = it },
                        label = { Text(stringResource(id = R.string.sale_price)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text("đ") }
                    )
                }

                // In Stock Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(id = R.string.in_stock), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = inStock,
                        onCheckedChange = { inStock = it }
                    )
                }

                // Error message
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Update Button (thay vì Save)
                Button(
                    onClick = { updateProduct() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.updating))
                    } else {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.update_product))
                    }
                }
            }
        }
    }

    // Add Specification Dialog (giống hệt AddProductPage)
    if (showAddSpecDialog) {
        var specKey by remember { mutableStateOf("") }
        var specValue by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSpecDialog = false },
            title = { Text(stringResource(id = R.string.add_specification)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = specKey,
                        onValueChange = { specKey = it },
                        label = { Text(stringResource(id = R.string.specification_name)) },
                        placeholder = { Text("e.g. Screen Size, RAM, CPU") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = specValue,
                        onValueChange = { specValue = it },
                        label = { Text(stringResource(id = R.string.value)) },
                        placeholder = { Text("e.g. 6.67 Inches, 8 GB") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (specKey.isNotBlank() && specValue.isNotBlank()) {
                            specifications = specifications + (specKey to specValue)
                            showAddSpecDialog = false
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSpecDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(id = R.string.success)) },
            text = { Text(stringResource(id = R.string.product_updated_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    navController.navigateUp()
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            }
        )
    }
}