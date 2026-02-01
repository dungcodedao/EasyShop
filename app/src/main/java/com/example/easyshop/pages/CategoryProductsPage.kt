
package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.easyshop.GlobalNavigation.navController
import com.example.easyshop.components.ProductFilterDialog
import com.example.easyshop.components.ProductItemView
import com.example.easyshop.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun CategoryProductsPage(
    modifier: Modifier = Modifier,
    categoryId: String
) {
    var allProducts by remember { mutableStateOf<List<ProductModel>>(emptyList()) }
    var brandList by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedBrand by remember { mutableStateOf("All Brands") }
    var selectedPriceSort by remember { mutableStateOf("Default") }
    var isLoading by remember { mutableStateOf(true) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Load dữ liệu từ Firebase
    LaunchedEffect(categoryId) {
        isLoading = true
        Firebase.firestore.collection("data").document("stock")
            .collection("products")
            .whereEqualTo("category", categoryId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    allProducts = task.result.documents.mapNotNull { doc ->
                        doc.toObject(ProductModel::class.java)?.apply { id = doc.id }
                    }

                    // Extract brands
                    brandList = listOf("All Brands") + allProducts
                        .mapNotNull { it.otherDetails["Brand"]?.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                }
                isLoading = false
            }
    }

    // Áp dụng filter & sort
    val filteredProducts = remember(allProducts, selectedBrand, selectedPriceSort) {
        allProducts
            .filter { product ->
                selectedBrand == "All Brands" ||
                        product.otherDetails["Brand"]?.trim().equals(selectedBrand, ignoreCase = true)
            }
            .let { products ->
                when (selectedPriceSort) {
                    "Price: Low to High" -> products.sortedBy { it.actualPrice.parsePrice() }
                    "Price: High to Low" -> products.sortedByDescending { it.actualPrice.parsePrice() }
                    else -> products
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header với nút filter
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),  // Padding nhỏ hơn
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Product Count
                Text(
                    text = "${filteredProducts.size} products",
                    style = MaterialTheme.typography.labelLarge
                )

                // Filter Button
                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filter",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

// Danh sách sản phẩm
        ProductListContent(
            isLoading = isLoading,
            products = filteredProducts,
            onResetFilters = {
                selectedBrand = "All Brands"
                selectedPriceSort = "Default"
            }
        )
    }
    if (showFilterDialog) {
        ProductFilterDialog(
            brandList = brandList,
            selectedBrand = selectedBrand,
            onBrandSelected = { selectedBrand = it },
            selectedPriceSort = selectedPriceSort,
            onPriceSortSelected = { selectedPriceSort = it },
            onDismiss = { showFilterDialog = false },
            onReset = {
                selectedBrand = "All Brands"
                selectedPriceSort = "Default"
            }
        )
    }
}

@Composable
private fun ProductListContent(
    isLoading: Boolean,
    products: List<ProductModel>,
    onResetFilters: () -> Unit
) {
    when {
        isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        products.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No products found")
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onResetFilters) {
                        Text("Reset filters")
                    }
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(products.chunked(2)) { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        rowItems.forEach { product ->
                            ProductItemView(
                                product = product,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// Extension function
private fun String?.parsePrice(): Double {
    return this?.replace(",", "")?.toDoubleOrNull() ?: 0.0
}