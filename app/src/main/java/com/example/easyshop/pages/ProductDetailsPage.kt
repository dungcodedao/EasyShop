package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easyshop.GlobalNavigation.navController
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.model.ProductModel
import com.example.easyshop.productdetails.AddToCartButton
import com.example.easyshop.productdetails.ProductHeader
import com.example.easyshop.productdetails.ProductImageSlider
import com.example.easyshop.productdetails.ProductPriceCard
import com.example.easyshop.productdetails.ProductTabContent
import com.example.easyshop.productdetails.ProductTabs
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
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
                    it.result.toObject(ProductModel::class.java)?.let { p -> product = p }
                }
            }
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
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
                        rating = 4.5f,
                        reviewCount = 120
                    )

                    Spacer(Modifier.height(16.dp))

                    ProductPriceCard(
                        price = product.price,
                        actualPrice = product.actualPrice
                    )

                    Spacer(Modifier.height(24.dp))

                    AddToCartButton(
                        inStock = product.inStock,
                        onAddToCart = { AppUtil.addItemToCart(context, productId) }
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
                        specifications = product.otherDetails
                    )

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}