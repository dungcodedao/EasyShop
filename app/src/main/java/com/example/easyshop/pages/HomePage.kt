package com.example.easyshop.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import com.example.easyshop.components.BannerView
import com.example.easyshop.components.CategoriesView
import com.example.easyshop.components.HeaderView
import com.example.easyshop.components.SearchView

@Composable
fun HomePage(modifier: Modifier = Modifier) {
    var isSearching by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        if (isSearching) {
            SearchView(onBackClick = { isSearching = false })
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                HeaderView(onSearchClick = { isSearching = true })

                Spacer(Modifier.height(20.dp))

                // Banner
                BannerView(modifier = Modifier.height(160.dp))

                Spacer(Modifier.height(24.dp))

                // Section Title
                Text(
                    text = stringResource(R.string.categories),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(12.dp))

                CategoriesView()

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}