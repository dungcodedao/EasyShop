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
import com.example.easyshop.ScreenState
import com.example.easyshop.AppUtil
import com.example.easyshop.components.ErrorStateView
import com.example.easyshop.model.ProductModel
import com.example.easyshop.util.GlobalNavigation.navController
import com.example.easyshop.components.ProductItemView
import com.example.easyshop.components.ProductFilterDialog
import com.example.easyshop.R
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
    var screenState by remember { mutableStateOf(ScreenState.LOADING) }
    var showFilterDialog by remember { mutableStateOf(false) }

    fun fetchProducts() {
        screenState = ScreenState.LOADING
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
                        .mapNotNull { it.otherDetails["Thương hiệu"]?.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                    screenState = ScreenState.SUCCESS
                } else {
                    screenState = ScreenState.ERROR
                }
            }
    }

    LaunchedEffect(categoryId) {
        fetchProducts()
    }

    val filteredProducts = remember(allProducts, selectedBrand, selectedPriceSort) {
        allProducts
            .filter { product ->
                selectedBrand == allBrandsLabel ||
                        product.otherDetails["Thương hiệu"]?.trim().equals(selectedBrand, ignoreCase = true)
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
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.products_count, filteredProducts.size),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        ProductListContent(
            modifier = Modifier.padding(padding),
            screenState = screenState,
            products = filteredProducts,
            onResetFilters = {
                selectedBrand = allBrandsLabel
                selectedPriceSort = defaultSortLabel
            },
            onRetry = { fetchProducts() }
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
    screenState: ScreenState,
    products: List<ProductModel>,
    onResetFilters: () -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (screenState) {
            ScreenState.LOADING -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            ScreenState.ERROR -> {
                ErrorStateView(
                    onRetry = onRetry
                )
            }
            ScreenState.EMPTY -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.no_products_found))
                }
            }
            ScreenState.SUCCESS -> {
                if (products.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
    }
}

private fun String?.parsePrice(): Double {
    return this?.replace(",", "")?.toDoubleOrNull() ?: 0.0
}
