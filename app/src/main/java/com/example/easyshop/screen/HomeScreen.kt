package com.example.easyshop.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.easyshop.model.NavItemModel
import com.example.easyshop.pages.*

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navController: NavController) {
    val navItemList = listOf(
        NavItemModel("Trang chủ", Icons.Filled.Home, Icons.Outlined.Home, "home"),
        NavItemModel("Yêu thích", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "favorite"),
        NavItemModel("Giỏ hàng", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "cart"),
        NavItemModel("Hồ sơ", Icons.Filled.Person, Icons.Outlined.Person, "profile"),
    )

    var selectedIndex by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.height(80.dp)
            ) {
                navItemList.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("ai-chat") },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "AI Assistant",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        ContentScreen(modifier = Modifier.padding(innerPadding), selectedIndex)
    }
}

@Composable
fun ContentScreen(modifier: Modifier = Modifier, selectedIndex: Int) {
    when (selectedIndex) {
        0 -> HomePage(modifier)
        1 -> FavoritePage(modifier)
        2 -> CartPage(modifier)
        3 -> ProfilePage(modifier)
    }
}