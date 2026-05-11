package com.example.easyshop.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.easyshop.model.CategoryModel
import com.example.easyshop.util.CategoryTranslations
import com.example.easyshop.util.GlobalNavigation
import com.example.easyshop.util.LanguageManager
import com.example.easyshop.viewmodel.HomeViewModel


@Composable
fun CategoriesView(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val categoryList by viewModel.categories.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val isLoading = screenState == com.example.easyshop.ScreenState.LOADING

    if (isLoading) {
        // Placeholder: lưới 2 hàng x 3 cột
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {}
                    }
                }
            }
        }
    } else {
        // Lưới 2 hàng x 3 cột, lọc bỏ danh mục "Test"
        val filteredList = remember(categoryList) { 
            categoryList.filter { !it.name.equals("Test", ignoreCase = true) } 
        }
        val rows = remember(filteredList) { filteredList.chunked(3) }
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        CategoryItem(
                            category = item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Nếu hàng cuối chưa đủ 3, thêm Spacer để giữ bố cục đều
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(category: CategoryModel, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .heightIn(min = 110.dp)
            .clickable {
                GlobalNavigation.navController.navigate("category-products/${category.id}")
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val isEnglish = LanguageManager.getCurrentLang(context) == LanguageManager.LANG_EN
            val displayName = CategoryTranslations.localize(category.name, isEnglish = isEnglish)

            val imageRequest = remember(category.imageUrl) {
                coil.request.ImageRequest.Builder(context)
                    .data(category.imageUrl)
                    .crossfade(true)
                    .size(200)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = displayName,
                modifier = Modifier.size(60.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
