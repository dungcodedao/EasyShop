package com.example.easyshop.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    Box(
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
                Scaffold(
                    topBar = {
                        Surface(
                            shadowElevation = 2.dp,
                            tonalElevation = 2.dp,
                            color = MaterialTheme.colorScheme.background
                        ) {
                            HeaderView(
                                onSearchClick = { onSearchToggle(true) },
                                onAvatarClick = onNavigateToProfile,
                                onNotificationClick = onNotificationClick,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    },
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshData() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Spacer(Modifier.height(12.dp))

                                // Banner
                                BannerView(viewModel = viewModel)

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

                                // Khoảng trống cuối trang
                                Spacer(Modifier.height(100.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}