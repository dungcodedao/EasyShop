package com.example.easyshop.pages

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.R
import com.example.easyshop.model.CategoryModel
import com.example.easyshop.repository.ProductRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class ProductMediaItem {
    data class LocalUri(val uri: Uri) : ProductMediaItem()
    data class RemoteUrl(val url: String) : ProductMediaItem()
    
    fun getPreviewModel(): Any = when (this) {
        is LocalUri -> uri
        is RemoteUrl -> url
    }
}

@Composable
fun AddProductPage(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var actualPrice by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var inStock by remember { mutableStateOf(true) }

    // Dynamic specifications
    var specifications by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var showAddSpecDialog by remember { mutableStateOf(false) }

    // Unified Media List (Max 5)
    var mediaItems by remember { mutableStateOf(listOf<ProductMediaItem>()) }
    var showAddOptions by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    var isLoading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<CategoryModel>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val firestore = Firebase.firestore
    val productRepository = remember { ProductRepository.getInstance(context) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = (5 - mediaItems.size).coerceAtLeast(0)
            if (remaining > 0) {
                mediaItems = mediaItems + uris.take(remaining).map { ProductMediaItem.LocalUri(it) }
            }
        }
    }

    // Load categories
    LaunchedEffect(key1 = Unit) {
        firestore.collection("data").document("stock")
            .collection("categories")
            .get()
            .addOnSuccessListener { result ->
                categories = result.documents.mapNotNull { it.toObject(CategoryModel::class.java) }
            }
    }

    // Save product to Firestore (with Cloudinary upload support)
    fun saveProduct() {
        if (title.isBlank() || category.isBlank() || price.isBlank()) {
            errorMessage = context.getString(R.string.please_fill_all_fields)
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val finalImageUrls = mutableListOf<String>()

                // 1. Phân loại media
                val localUris = mediaItems.filterIsInstance<ProductMediaItem.LocalUri>().map { it.uri }
                val remoteUrls = mediaItems.filterIsInstance<ProductMediaItem.RemoteUrl>().map { it.url }

                // 2. Upload selected images from device to Cloudinary
                if (localUris.isNotEmpty()) {
                    localUris.forEachIndexed { index, uri ->
                        uploadProgress = "Đang upload ảnh ${index + 1}/${localUris.size}..."
                        productRepository.uploadProductImage(uri).collectLatest { result ->
                            result.fold(
                                onSuccess = { url -> finalImageUrls.add(url) },
                                onFailure = { e -> throw e }
                            )
                        }
                    }
                }

                // 3. Khôi phục Manual URLs
                finalImageUrls.addAll(remoteUrls)

                val distinctUrls = finalImageUrls.distinct().take(5)

                if (distinctUrls.isEmpty()) {
                    errorMessage = "Vui lòng chọn ảnh hoặc nhập URL"
                    isLoading = false
                    uploadProgress = ""
                    return@launch
                }

                uploadProgress = "Đang lưu sản phẩm..."

                val otherDetails = specifications
                    .filter { it.first.isNotBlank() && it.second.isNotBlank() }
                    .toMap()

                val productId = UUID.randomUUID().toString()

                val productData = hashMapOf(
                    "id" to productId,
                    "title" to title,
                    "description" to description,
                    "price" to price,
                    "actualPrice" to actualPrice.ifBlank { price },
                    "category" to category,
                    "inStock" to inStock,
                    "images" to distinctUrls,
                    "otherDetails" to otherDetails
                )

                firestore.collection("data").document("stock")
                    .collection("products")
                    .document(productId)
                    .set(productData)
                    .await()

                showSuccessDialog = true

                // Reset form
                title = ""
                description = ""
                price = ""
                actualPrice = ""
                category = ""
                specifications = emptyList()
                mediaItems = emptyList()

            } catch (e: Exception) {
                Log.e("AddProductPage", "Upload error: ${e.message}", e)
                errorMessage = "Lỗi upload: ${e.message}"
            } finally {
                isLoading = false
                uploadProgress = ""
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
                    .statusBarsPadding()
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
                    text = stringResource(id = R.string.add_new_product),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.size(40.dp))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== Product Images Section =====
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
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(id = R.string.product_images),
                            style = MaterialTheme.typography.titleMedium
                        )
                        val totalImages = mediaItems.size
                        Text(
                            "$totalImages/5",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // ===== Unified Media Grid =====
                    val columns = 3
                    val rows = (mediaItems.size + 1 + columns - 1) / columns // Number of rows required, +1 for the Add button
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0 until columns) {
                                    val index = row * columns + col
                                    if (index < mediaItems.size) {
                                        // Display Media Item
                                        MediaItemCard(
                                            item = mediaItems[index],
                                            onRemove = {
                                                mediaItems = mediaItems.filterIndexed { i, _ -> i != index }
                                            },
                                            modifier = Modifier.weight(1f).aspectRatio(1f)
                                        )
                                    } else if (index == mediaItems.size && mediaItems.size < 5) {
                                        // "Add" slot
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .clickable { showAddOptions = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Thêm ảnh",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "Thêm",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        // Invisible spacer to balance the row
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
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
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                        Icon(Icons.Default.Add, contentDescription = null)
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
                    .clickable { inStock = !inStock }
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

            // Save Button
            Button(
                onClick = { saveProduct() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(uploadProgress.ifEmpty { stringResource(id = R.string.uploading) })
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.save_product))
                }
            }
        }
    }

    // Add Specification Dialog
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
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(id = R.string.success)) },
            text = { Text(stringResource(id = R.string.product_added_msg)) },
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

    // Modal Bottom Sheet for Image Options
    if (showAddOptions) {
        ModalBottomSheet(
            onDismissRequest = { showAddOptions = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Thêm hình ảnh",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                ListItem(
                    headlineContent = { Text("Thư viện ảnh") },
                    supportingContent = { Text("Chọn ảnh có sẵn trong máy") },
                    leadingContent = { 
                        Icon(
                            Icons.Default.PhotoLibrary, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    modifier = Modifier.clickable {
                        showAddOptions = false
                        imagePickerLauncher.launch("image/*")
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Nhập link URL") },
                    supportingContent = { Text("Sử dụng ảnh từ internet") },
                    leadingContent = { 
                        Icon(
                            Icons.Default.Link, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        ) 
                    },
                    modifier = Modifier.clickable {
                        showAddOptions = false
                        showUrlDialog = true
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // URL Input Dialog
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Nhập Link Ảnh") },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("URL (https://...)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            mediaItems = (mediaItems + ProductMediaItem.RemoteUrl(urlInput)).take(5)
                            urlInput = ""
                            showUrlDialog = false
                        }
                    }
                ) { Text("Thêm") }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun MediaItemCard(
    item: ProductMediaItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray.copy(alpha = 0.3f))
    ) {
        AsyncImage(
            model = item.getPreviewModel(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Source Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item is ProductMediaItem.RemoteUrl) Icons.Default.Link else Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        
        // Remove Button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Xóa",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
