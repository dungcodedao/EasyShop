package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.easyshop.R
import com.example.easyshop.AppUtil
import com.example.easyshop.components.ProductItemView
import com.example.easyshop.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun FavoritePage(modifier: Modifier = Modifier) {
    val productsList = remember { mutableStateOf<List<ProductModel>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        val favoriteList = AppUtil.getFavoriteList(context)
        if (favoriteList.isEmpty()) {
            productsList.value = emptyList()
        } else {
            Firebase.firestore.collection("data").document("stock")
                .collection("products")
                .whereIn("id", favoriteList.toList())
                .get().addOnCompleteListener {
                    if (it.isSuccessful) {
                        productsList.value = it.result.documents.mapNotNull { doc -> doc.toObject(ProductModel::class.java) }
                    }
                }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.your_favorites),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        if (productsList.value.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = productsList.value.chunked(2),
                    key = { row -> row.joinToString("-") { it.id } }
                ) { rowItems ->
                    Row {
                        rowItems.forEach {
                            ProductItemView(product = it, modifier = Modifier.weight(1f))
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.no_favorites_yet),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}