package com.example.easyshop.productdetails


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.easyshop.*
import com.tbuonomo.viewpagerdotsindicator.compose.DotsIndicator
import com.tbuonomo.viewpagerdotsindicator.compose.model.DotGraphic
import com.tbuonomo.viewpagerdotsindicator.compose.type.ShiftIndicatorType

@Composable
fun ProductImageSlider(
    images: List<String>,
    inStock: Boolean,
    productId: String
) {
    val context = LocalContext.current
    var isFav by remember { mutableStateOf(AppUtil.checkFavorite(context, productId)) }

    if (images.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            val pagerState = rememberPagerState(pageCount = { images.size })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = images[page],
                    contentDescription = "Product image",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Dots Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp)
            ) {
                DotsIndicator(
                    dotCount = images.size,
                    type = ShiftIndicatorType(
                        DotGraphic(color = MaterialTheme.colorScheme.primary, size = 10.dp)
                    ),
                    pagerState = pagerState
                )
            }

            // Favorite Button
            IconButton(
                onClick = {
                    AppUtil.addOrRemoveFromFavorite(context, productId)
                    isFav = AppUtil.checkFavorite(context, productId)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    tint = if (isFav) Color(0xFFE91E63) else Color.Gray,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(28.dp)
                )
            }

            // Out of Stock Badge
            if (!inStock) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Red, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "OUT OF STOCK",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
