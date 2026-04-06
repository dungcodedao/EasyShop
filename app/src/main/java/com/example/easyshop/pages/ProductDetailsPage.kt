package com.example.easyshop.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.GlobalNavigation.navController
import com.example.easyshop.AppUtil
import com.example.easyshop.ScreenState
import com.example.easyshop.R
import com.example.easyshop.model.ProductModel
import com.example.easyshop.productdetails.*
import com.example.easyshop.components.ErrorStateView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsPage(modifier: Modifier = Modifier, productId: String) {
    var product by remember { mutableStateOf(ProductModel()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var screenState by remember { mutableStateOf(ScreenState.LOADING) }
    var selectedQuantity by remember { mutableIntStateOf(1) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun fetchProduct() {
        screenState = ScreenState.LOADING
        Firebase.firestore.collection("data").document("stock")
            .collection("products")
            .document(productId).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result?.exists() == true) {
                    task.result.toObject(ProductModel::class.java)?.let { p ->
                        product = p
                        screenState = ScreenState.SUCCESS
                    } ?: run {
                        screenState = ScreenState.ERROR
                    }
                } else {
                    screenState = ScreenState.ERROR
                }
            }
    }

    LaunchedEffect(key1 = productId) {
        fetchProduct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.product_details)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (screenState) {
                ScreenState.LOADING -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Đang tải sản phẩm...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ScreenState.ERROR -> {
                    ErrorStateView(
                        onRetry = { fetchProduct() }
                    )
                }

                ScreenState.SUCCESS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Image Slider
                        ProductImageSlider(
                            images = product.images,
                            inStock = product.inStock,
                            productId = productId
                        )

                        // Product Info Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-20).dp),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                ProductHeader(
                                    title = product.title,
                                    inStock = product.inStock,
                                    stockCount = product.stockCount,
                                    rating = product.rating,
                                    reviewCount = product.reviewCount
                                )

                                Spacer(Modifier.height(16.dp))

                                ProductPriceCard(
                                    price = product.price,
                                    actualPrice = product.actualPrice
                                )

                                if (product.inStock) {
                                    Spacer(Modifier.height(24.dp))
                                    
                                    // Quantity Selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Số lượng",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.weight(1f))
                                        
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { if (selectedQuantity > 1) selectedQuantity-- },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, null, Modifier.size(18.dp))
                                                }
                                                
                                                Text(
                                                    text = selectedQuantity.toString(),
                                                    modifier = Modifier.widthIn(min = 36.dp),
                                                    textAlign = TextAlign.Center,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                
                                                IconButton(
                                                    onClick = { if (selectedQuantity < product.stockCount) selectedQuantity++ },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(24.dp))

                                AddToCartButton(
                                    inStock = product.inStock,
                                    onAddToCart = { AppUtil.addItemToCart(context, productId, selectedQuantity) }
                                )

                                Spacer(Modifier.height(24.dp))

                                ProductTabs(
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it }
                                )

                                Spacer(Modifier.height(16.dp))

                                ProductTabContent(
                                    selectedTab = selectedTab,
                                    description = product.description,
                                    specifications = product.otherDetails,
                                    productId = productId
                                )

                                Spacer(Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}