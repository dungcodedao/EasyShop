
package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import com.example.easyshop.GlobalNavigation.navController
import com.example.easyshop.components.ProductFilterDialog
import com.example.easyshop.components.ProductItemView
import com.example.easyshop.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryProductsPage(
    modifier: Modifier = Modifier,
    categoryId: String
) {
    val allBrandsLabel = stringResource(id = R.string.all_brands)
    val defaultSortLabel = stringResource(id = R.string.sort_default)
    val priceLowHighLabel = stringResource(id = R.string.sort_price_low_high)
    val priceHighLowLabel = stringResource(id = R.string.sort_price_high_low)

    var allProducts by remember { mutableStateOf<List<ProductModel>>(emptyList()) }
    var brandList by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedBrand by remember { mutableStateOf(allBrandsLabel) }
    var selectedPriceSort by remember { mutableStateOf(defaultSortLabel) }
    var isLoading by remember { mutableStateOf(true) }
    var showFilterDialog by remember { mutableStateOf(false) }

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
                    brandList = listOf(allBrandsLabel) + allProducts
                        .mapNotNull { it.otherDetails["Brand"]?.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                }
                isLoading = false
            }
    }

    val filteredProducts = remember(allProducts, selectedBrand, selectedPriceSort) {
        allProducts
            .filter { product ->
                selectedBrand == allBrandsLabel ||
                        product.otherDetails["Brand"]?.trim().equals(selectedBrand, ignoreCase = true)
            }
            .let { products ->
                when (selectedPriceSort) {
                    priceLowHighLabel -> products.sortedBy { it.actualPrice.parsePrice() }
                    priceHighLowLabel -> products.sortedByDescending { it.actualPrice.parsePrice() }
                    else -> products
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.products_count, filteredProducts.size),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = { showFilterDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        ProductListContent(
            modifier = Modifier.padding(padding),
            isLoading = isLoading,
            products = filteredProducts,
            onResetFilters = {
                selectedBrand = allBrandsLabel
                selectedPriceSort = defaultSortLabel
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
                selectedBrand = allBrandsLabel
                selectedPriceSort = defaultSortLabel
            }
        )
    }
}

@Composable
private fun ProductListContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    products: List<ProductModel>,
    onResetFilters: () -> Unit
) {
    when {
        isLoading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        products.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(id = R.string.no_products_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onResetFilters) {
                        Text(stringResource(id = R.string.reset_filters))
                    }
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = products.chunked(2),
                    key = { row -> row.joinToString("-") { it.id } }
                ) { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { product ->
                            ProductItemView(product = product, modifier = Modifier.weight(1f))
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun String?.parsePrice(): Double {
    return this?.replace(",", "")?.toDoubleOrNull() ?: 0.0
}