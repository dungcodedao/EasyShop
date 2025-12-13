package com.example.easyshop.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("data").document("stock")
            .collection("categories")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { it.toObject(CategoryModel::class.java) }
                categoryList.value = list
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(16.dp),   // giảm từ 18 → 16dp cho vừa tay
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(16.dp),                 // SIÊU QUAN TRỌNG: lề ngoài đẹp đều
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp)                              // khoảng cách từ banner xuống đây
    ) {
        items(
            items = categoryList.value,
            key = { it.id }                                    // tốt cho performance + recomposition
        ) { item ->
            CategoryItem(category = item)
        }
    }
}

@Composable
fun CategoryItem(category: CategoryModel) {
    Card(
        modifier = Modifier
            .size(110.dp)                                      // tăng từ 100 → 110dp cho dễ bấm
            .clickable {
                GlobalNavigation.navController.navigate("category-products/${category.id}")
            },
        shape = RoundedCornerShape(16.dp),                     // bo góc mềm hơn
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)                                 // THÊM DÒNG NÀY = FIX 90% VẤN ĐỀ SÁT NHAU
        ) {
            AsyncImage(
                model = category.imageUrl,
                contentDescription = category.name,
                modifier = Modifier
                    .size(64.dp)                               // icon to hơn → đẹp hơn, dễ nhìn hơn
                    .clip(RoundedCornerShape(12.dp))           // bo góc ảnh nhẹ (đẹp bonus)
            )

            Spacer(modifier = Modifier.height(10.dp))          // từ 8 → 10dp cho thoáng

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,   // dùng theme chuẩn hơn
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}