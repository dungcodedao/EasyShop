package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import com.example.easyshop.components.ProductItemView
import com.example.easyshop.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritePage(
    modifier: Modifier = Modifier
) {
    var favoriteProducts by remember { mutableStateOf<List<ProductModel>>(emptyList()) }
    var screenState by remember { mutableStateOf(ScreenState.LOADING) }
    val userId = Firebase.auth.currentUser?.uid

    fun fetchFavorites() {
        if (userId == null) {
            screenState = ScreenState.ERROR
            return
        }
        screenState = ScreenState.LOADING
        Firebase.firestore.collection("users").document(userId)
            .collection("favorites")
            .get()
            .addOnSuccessListener { result ->
                favoriteProducts = result.documents.mapNotNull { doc ->
                    doc.toObject(ProductModel::class.java)?.apply { id = doc.id }
                }
                screenState = ScreenState.SUCCESS
            }
            .addOnFailureListener {
                screenState = ScreenState.ERROR
            }
    }

    LaunchedEffect(userId) {
        fetchFavorites()
    }

    Scaffold(
        topBar = {
            // Chỉ hiện TopBar nếu không phải trạng thái lỗi hoặc load để ErrorStateView được căn giữa tốt hơn
            if (screenState == ScreenState.SUCCESS) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(id = R.string.favorites),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (screenState) {
                ScreenState.LOADING -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                ScreenState.ERROR -> {
                    ErrorStateView(
                        onRetry = { fetchFavorites() }
                    )
                }
                ScreenState.SUCCESS -> {
                    if (favoriteProducts.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(id = R.string.no_favorites),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(favoriteProducts) { product ->
                                ProductItemView(product = product)
                            }
                        }
                    }
                }
            }
        }
    }
}