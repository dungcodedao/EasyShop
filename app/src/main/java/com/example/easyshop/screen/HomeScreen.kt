package com.example.easyshop.screen

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.easyshop.model.NavItemModel
import com.example.easyshop.pages.CartPage
import com.example.easyshop.pages.FavoritePage
import com.example.easyshop.pages.HomePage
import com.example.easyshop.pages.ProfilePage
import com.example.easyshop.viewmodel.FavoriteViewModel
import com.example.easyshop.viewmodel.HomeViewModel

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navController: NavController) {
    val navItemList = listOf(
        NavItemModel("Trang chủ", Icons.Filled.Home, Icons.Outlined.Home, "home"),
        NavItemModel("Yêu thích", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "favorite"),
        NavItemModel("Giỏ hàng", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "cart"),
        NavItemModel("Hồ sơ", Icons.Filled.Person, Icons.Outlined.Person, "profile"),
    )

    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Chặn phím Back để hiện Dialog xác nhận thoát
    BackHandler(enabled = !isSearching) { // Chỉ hiện khi không ở chế độ tìm kiếm
        showExitDialog = true
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = { (context as? Activity)?.finish() },
            onDismiss = { showExitDialog = false }
        )
    }

    Scaffold(
        bottomBar = {
            if (!isSearching) {
                CustomFloatingNavigationBar(
                    items = navItemList,
                    selectedIndex = selectedIndex,
                    onItemSelected = { selectedIndex = it }
                )
            }
        },
        floatingActionButton = {
            if (selectedIndex == 0 && !isSearching) {
                FloatingActionButton(
                    onClick = { navController.navigate("ai-chat") },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(bottom = 16.dp) 
                        .shadow(8.dp, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "AI Assistant",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        val homeViewModel: HomeViewModel = viewModel()
        val favoriteViewModel: FavoriteViewModel = viewModel()

        ContentScreen(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            selectedIndex = selectedIndex,
            navController = navController,
            onNavigateToProfile = { selectedIndex = 3 },
            onNotificationClick = { navController.navigate("notifications") },
            isSearching = isSearching,
            onSearchToggle = { isSearching = it },
            homeViewModel = homeViewModel,
            favoriteViewModel = favoriteViewModel
        )
    }
}

@Composable
fun CustomFloatingNavigationBar(
    items: List<NavItemModel>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = activeColor.copy(alpha = 0.2f)
                ),
            shape = RoundedCornerShape(24.dp),
            color = containerColor,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = tween(durationMillis = 300),
                        label = "scale"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onItemSelected(index) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = if (isSelected) activeColor else inactiveColor,
                            modifier = Modifier
                                .size(26.dp)
                                .scale(scale)
                        )
                        
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = item.label,
                                color = activeColor,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        if (!isSelected) {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContentScreen(
    modifier: Modifier = Modifier, 
    selectedIndex: Int, 
    navController: NavController,
    onNavigateToProfile: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    isSearching: Boolean = false,
    onSearchToggle: (Boolean) -> Unit = {},
    homeViewModel: HomeViewModel,
    favoriteViewModel: FavoriteViewModel
) {
    Box(modifier = modifier) {
        when (selectedIndex) {
            0 -> HomePage(
                modifier = Modifier.fillMaxSize(), 
                onNavigateToProfile = onNavigateToProfile,
                onNotificationClick = onNotificationClick,
                isSearching = isSearching,
                onSearchToggle = onSearchToggle,
                viewModel = homeViewModel
            )
            1 -> FavoritePage(
                modifier = Modifier.fillMaxSize(),
                viewModel = favoriteViewModel
            )
            2 -> CartPage(Modifier.fillMaxSize(), navController = navController)
            3 -> ProfilePage(Modifier.fillMaxSize(), navController = navController)
        }
    }
}

@Composable
fun ExitConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                "Thoát EasyShop?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Bạn có chắc chắn muốn đóng ứng dụng không? Giỏ hàng của bạn vẫn sẽ được lưu lại.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Thoát", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    )
}
