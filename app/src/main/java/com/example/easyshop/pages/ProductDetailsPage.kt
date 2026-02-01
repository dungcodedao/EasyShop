package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.easyshop.GlobalNavigation.navController
import com.example.easyshop.AppUtil
import com.example.easyshop.model.ProductModel
import com.example.easyshop.productdetails.AddToCartButton
import com.example.easyshop.productdetails.ProductHeader
import com.example.easyshop.productdetails.ProductImageSlider
import com.example.easyshop.productdetails.ProductPriceCard
import com.example.easyshop.productdetails.ProductTabContent
import com.example.easyshop.productdetails.ProductTabs
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun ProductDetailsPage(modifier: Modifier = Modifier, productId: String) {
    var product by remember { mutableStateOf(ProductModel()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("data").document("stock")
            .collection("products")
            .document(productId).get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val result = it.result.toObject(ProductModel::class.java)
                    if (result != null) {
                        product = result
                    }
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
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Product Details",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.size(40.dp))
            }
        }

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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Title & Rating
                    ProductHeader(
                        title = product.title,
                        inStock = product.inStock,
                        rating = 4.5f,
                        reviewCount = 120

                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Price
                    ProductPriceCard(
                        price = product.price,
                        actualPrice = product.actualPrice
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Add to Cart Button
                    AddToCartButton(
                        inStock = product.inStock,
                        onAddToCart = {
                            AppUtil.addItemToCart(context, productId)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tabs
                    ProductTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab Content
                    ProductTabContent(
                        selectedTab = selectedTab,
                        description = product.description,
                        specifications = product.otherDetails
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}