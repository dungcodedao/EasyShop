package com.example.easyshop.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
    var bannerList by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("data")
            .document("banners")
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    @Suppress("UNCHECKED_CAST")
                    val urls = task.result?.get("urls") as? List<String>
                    if (urls != null) bannerList = urls
                }
            }
    }

    if (bannerList.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val pagerState = rememberPagerState(0) { bannerList.size }

        // Auto-scroll
        LaunchedEffect(key1 = bannerList) {
            while (true) {
                delay(4000)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % bannerList.size
                    pagerState.animateScrollToPage(nextPage, animationSpec = tween(800))
                }
            }
        }

        // Banner Pager wrapped in Card for shadow
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                AsyncImage(
                    model = bannerList[pageIndex],
                    contentDescription = "Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        DotsIndicator(
            dotCount = bannerList.size,
            type = ShiftIndicatorType(
                DotGraphic(
                    color = MaterialTheme.colorScheme.primary,
                    size = 7.dp
                )
            ),
            pagerState = pagerState
        )
    }
}
