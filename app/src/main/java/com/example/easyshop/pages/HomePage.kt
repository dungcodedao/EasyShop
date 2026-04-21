package com.example.easyshop.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import com.example.easyshop.ScreenState
import com.example.easyshop.components.BannerView
import com.example.easyshop.components.CategoriesView
import com.example.easyshop.components.ErrorStateView
import com.example.easyshop.components.HeaderView
import com.example.easyshop.components.SearchView
import com.example.easyshop.viewmodel.HomeViewModel

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    isSearching: Boolean = false,
    onSearchToggle: (Boolean) -> Unit = {},
    viewModel: HomeViewModel // ViewModel injected from HomeScreen
) {
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
            SearchView(
                onBackClick = { onSearchToggle(false) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val isRefreshing by viewModel.isRefreshing.collectAsState()
            val screenState by viewModel.screenState.collectAsState()
            val banners by viewModel.banners.collectAsState()

            if (screenState == ScreenState.ERROR && banners.isEmpty()) {
                ErrorStateView(
                    modifier = Modifier.fillMaxSize(),
                    onRetry = { viewModel.refreshData() }
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshData() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))

                        HeaderView(
                            onSearchClick = { onSearchToggle(true) },
                            onAvatarClick = onNavigateToProfile,
                            onNotificationClick = onNotificationClick
                        )

                        Spacer(Modifier.height(20.dp))

                        // Banner
                        BannerView(viewModel = viewModel, modifier = Modifier.height(160.dp))

                        Spacer(Modifier.height(24.dp))

                        // Section Title - Categories
                        Text(
                            text = stringResource(R.string.categories),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(Modifier.height(12.dp))

                        CategoriesView(viewModel = viewModel)

                        // Tăng khoảng trống để tránh bị che bởi thanh điều hướng và nút AI (Spark button)
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}