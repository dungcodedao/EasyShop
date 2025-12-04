package com.example.easyshop.componmets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
fun BannerView(modifier: Modifier = Modifier){

    var bannerList by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    //lay du lieu dât banner từ firebase
    LaunchedEffect(Unit) {
        Firebase.firestore.collection("data")
            .document("banners")
            .get().addOnCompleteListener(){
                bannerList = it.result.get("urls") as List<String>
            }
    }

    Column (
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally

    ){
        val pagerState = rememberPagerState (0) {
            bannerList.size
        }
//chuyen dong banner
        LaunchedEffect(pagerState.currentPage) {
            if (bannerList.isNotEmpty()) {
                delay(3000) // 3 giây
                val nextPage = (pagerState.currentPage + 1) % bannerList.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
        HorizontalPager(
            state = pagerState,
            pageSpacing =24.dp
        ) {
            AsyncImage(model = bannerList.get(it),
                contentDescription = "Banner image",
               modifier = Modifier.fillMaxWidth()
                   .clip(RoundedCornerShape(16.dp))
                .height(140.dp) ,
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


