package com.example.easyshop.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.easyshop.util.shimmerEffect
import com.example.easyshop.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun BannerView(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val bannerList by viewModel.banners.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val isLoading = screenState == com.example.easyshop.ScreenState.LOADING

    if (isLoading) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shimmerEffect()
            )
        }
        return
    }

    if (bannerList.isEmpty()) return

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val pagerState = rememberPagerState(0) { bannerList.size }

        // Auto-scroll
        LaunchedEffect(key1 = bannerList) {
            while (true) {
                delay(4000)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = if (bannerList.isNotEmpty()) (pagerState.currentPage + 1) % bannerList.size else 0
                    pagerState.animateScrollToPage(nextPage, animationSpec = tween(800))
                }
            }
        }

        // Banner Pager wrapped in Card for shadow
        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                SubcomposeAsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(bannerList.getOrNull(pageIndex))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Banner",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.FillBounds, // Ép dãn hình ảnh đầy khung 160.dp, không bị lề trống cũng không bị cắt chữ
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shimmerEffect()
                        )
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "EasyShop Promo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                )
            }
        }
    }
}
