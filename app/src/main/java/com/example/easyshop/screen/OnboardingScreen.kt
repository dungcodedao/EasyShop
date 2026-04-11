package com.example.easyshop.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

data class OnboardingPage(val title: String, val description: String, val emoji: String)

@Composable
fun OnboardingScreen(navController: NavController) {
    val pages = listOf(
        OnboardingPage("Chào mừng đến EasyShop", "Khám phá hàng ngàn sản phẩm chất lượng với giá tốt nhất.", "🛍️"),
        OnboardingPage("Mua sắm dễ dàng", "Thêm vào giỏ hàng, thanh toán nhanh chóng chỉ trong vài bước.", "🛒"),
        OnboardingPage("Giao hàng nhanh", "Nhận hàng tại nhà với dịch vụ giao hàng uy tín và nhanh chóng.", "🚀")
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = pages[page].emoji, fontSize = 80.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = pages[page].title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = pages[page].description,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF6B7280)
                )
            }
        }

        // Dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val primaryIndigo = Color(0xFF4F46E5)
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(if (isSelected) 24.dp else 8.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) primaryIndigo else Color(0xFFD1D5DB))
                )
            }
        }

        // Button
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            val primaryIndigo = Color(0xFF4F46E5)
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        navController.navigate("auth") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryIndigo)
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.size - 1) "Tiếp theo" else "Bắt đầu ngay",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
