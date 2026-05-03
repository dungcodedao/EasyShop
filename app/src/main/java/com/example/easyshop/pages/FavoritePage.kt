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
import com.example.easyshop.viewmodel.FavoriteViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritePage(
    modifier: Modifier = Modifier,
    viewModel: FavoriteViewModel
) {
    val favoriteProducts by viewModel.favorites.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val screenState by viewModel.screenState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchFavorites()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Favorites Header
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.favorites),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
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
