package com.example.easyshop.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.easyshop.viewmodel.OnboardingViewModel
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

@Composable
fun SplashScreen(navController: NavController, viewModel: OnboardingViewModel = viewModel()) {
    val auth = FirebaseAuth.getInstance()
    val primaryIndigo = Color(0xFF4F46E5)
    val skyBlue = Color(0xFF0EA5E9)
    val context = LocalContext.current

    val urls by viewModel.onboardingUrls.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val logoUrl = urls.firstOrNull()

    // Animation States
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alpha"
    )

    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        startAnimation = true
        
        // Start role fetch concurrently
        val roleDeferred = async {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                try {
                    val doc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()
                    doc.getString("role") ?: "user"
                } catch (e: Exception) {
                    "user"
                }
            } else null
        }

        // Animate progress over 2 seconds
        val startTime = System.currentTimeMillis()
        val duration = 2000L
        while (System.currentTimeMillis() - startTime < duration) {
            progress = (System.currentTimeMillis() - startTime).toFloat() / duration
            delay(16) // ~60fps
        }
        progress = 1f
        
        // Chờ thêm nếu Firebase đang tải URL ảnh
        while (viewModel.screenState.value == com.example.easyshop.ScreenState.LOADING) {
            delay(100)
        }

        val role = roleDeferred.await()
        
        if (role != null) {
            val destination = if (role == "admin") "admin-dashboard" else "home"
            navController.navigate(destination) {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            if (com.example.easyshop.AppUtil.isOnboardingCompleted(context)) {
                navController.navigate("auth") {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                navController.navigate("onboarding") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4F46E5)), // Fallback background
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "Splash Screen Flow",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alphaAnim)
                    .scale(scale)
            )

            // Progress Bar overlay
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .padding(horizontal = 64.dp)
                    .height(6.dp)
                    .fillMaxWidth(),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        } else {
            if (screenState == com.example.easyshop.ScreenState.LOADING) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}
