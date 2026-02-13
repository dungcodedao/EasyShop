package com.example.easyshop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.model.CategoryModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun CategoriesView(modifier: Modifier = Modifier) {
    val categoryList = remember { mutableStateOf<List<CategoryModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        Firebase.firestore.collection("data").document("stock")
            .collection("categories")
            .get()
            .addOnSuccessListener { result ->
                categoryList.value = result.documents.mapNotNull { it.toObject(CategoryModel::class.java) }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    if (isLoading) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            items(5) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    )
                }
            }
        }
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            items(
                items = categoryList.value,
                key = { it.id }
            ) { item ->
                CategoryItem(category = item)
            }
        }
    }
}

@Composable
fun CategoryItem(category: CategoryModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .clickable {
                GlobalNavigation.navController.navigate("category-products/${category.id}")
            }
    ) {
        // Circular image with subtle background
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = category.imageUrl,
                contentDescription = category.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}