package com.example.easyshop.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.tbuonomo.viewpagerdotsindicator.compose.DotsIndicator
import com.tbuonomo.viewpagerdotsindicator.compose.model.DotGraphic
import com.tbuonomo.viewpagerdotsindicator.compose.type.ShiftIndicatorType
import kotlinx.coroutines.delay

@Composable
fun BannerView(modifier: Modifier = Modifier) {

    var bannerList by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    // Lấy dữ liệu banner từ Firebase
    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("data")
            .document("banners")
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val urls = task.result?.get("urls") as? List<String>
                    if (urls != null) {
                        bannerList = urls
                    }
                }
            }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val pagerState = rememberPagerState(0) {
            bannerList.size
        }

        // Tự động chuyển banner mượt mà hơn
        LaunchedEffect(key1 = bannerList) {
            if (bannerList.isNotEmpty()) {
                while (true) {
                    delay(3000) // Đợi 4 giây cho mỗi banner
                    if (!pagerState.isScrollInProgress) {
                        val nextPage = (pagerState.currentPage + 1) % bannerList.size
                        pagerState.animateScrollToPage(
                            nextPage,
                            animationSpec = tween(durationMillis = 1000) // Animation kéo dài 1s
                        )
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 0.dp), // Bỏ hiệu ứng peek
            pageSpacing = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp) // Padding ngoài để banner không dính sát mép máy
        ) { pageIndex ->
            AsyncImage(
                model = bannerList.get(pageIndex),
                contentDescription = "Banner image",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .height(150.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        DotsIndicator(
            dotCount = bannerList.size,
            type = ShiftIndicatorType(
                DotGraphic(
                    color = MaterialTheme.colorScheme.primary,
                    size = 8.dp
                )
            ),
            pagerState = pagerState
        )
    }
}
