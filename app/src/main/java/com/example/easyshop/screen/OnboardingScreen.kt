package com.example.easyshop.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.AppUtil
import com.example.easyshop.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

data class OnboardingPage(val title: String, val description: String)

@Composable
fun OnboardingScreen(navController: NavController, viewModel: OnboardingViewModel = viewModel()) {
    val context = LocalContext.current
    val urls by viewModel.onboardingUrls.collectAsState()

    val pages = listOf(
        OnboardingPage(
            "Thế giới Công nghệ",
            "Sở hữu ngay những thiết bị công nghệ đỉnh cao từ các thương hiệu hàng đầu với mức giá ưu đãi đặc quyền chỉ có tại EasyShop."
        ),
        OnboardingPage(
            "Trợ lý AI Thông minh",
            "Trải nghiệm mua sắm cá nhân hóa hoàn toàn. AI của chúng tôi thấu hiểu phong cách và luôn sẵn sàng đề xuất những gì bạn cần."
        ),
        OnboardingPage(
            "An toàn & Thần tốc",
            "Giao dịch bảo mật tuyệt đối, hỗ trợ tận tình 24/7. Nhận đơn hàng ngay tại nhà chỉ trong chớp mắt với dịch vụ chuyển phát siêu tốc."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    // Dynamic Background Colors
    val bgColors = listOf(
        Color(0xFFEEF2FF), // Indigo tint
        Color(0xFFF0F9FF), // Sky tint
        Color(0xFFF5F3FF)  // Violet tint
    )
    
    val currentBgColor by animateColorAsState(
        targetValue = bgColors.getOrElse(pagerState.currentPage) { Color.White },
        animationSpec = tween(durationMillis = 500),
        label = "bgColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)) // Light gray background for whole screen
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Section: Image (55%)
                Box(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUrl = urls.getOrNull(page + 1)
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit, // Better for 3D objects with white backgrounds
                            modifier = Modifier
                                .fillMaxWidth(0.85f) // Don't let it touch edges too much
                                .fillMaxHeight()
                        )
                    }
                }

                // Bottom Section: Content Card (45%)
                Surface(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Text Content
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = pages[page].title,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF111827)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = pages[page].description,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF4B5563),
                                lineHeight = 24.sp
                            )
                        }

                        // Navigation UI (Dots & Button)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Dots
                            Row(
                                modifier = Modifier.padding(bottom = 24.dp),
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
                                            .background(if (isSelected) primaryIndigo else Color(0xFFE5E7EB))
                                    )
                                }
                            }

                            // Main Button
                            val primaryIndigo = Color(0xFF4F46E5)
                            Button(
                                onClick = {
                                    if (pagerState.currentPage < pages.size - 1) {
                                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    } else {
                                        AppUtil.setOnboardingCompleted(context)
                                        navController.navigate("auth") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
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
                    }
                }
            }
        }
    }
}
