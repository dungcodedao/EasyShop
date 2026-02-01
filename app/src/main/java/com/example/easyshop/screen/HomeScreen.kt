package com.example.easyshop.screen

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.easyshop.model.NavItemModel // Import model
import com.example.easyshop.pages.*

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navController: NavController) {
    val navItemList = listOf(
        NavItemModel("Home", Icons.Filled.Home, Icons.Filled.Home, "home"),
        NavItemModel("Favorite", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "favorite"),
        NavItemModel("Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "cart"),
        NavItemModel("Profile", Icons.Filled.Person, Icons.Outlined.Person, "profile"),
    )

    var selectedIndex by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 12.dp,
                modifier = Modifier.height(110.dp)
            ) {
                navItemList.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (selectedIndex == index) Color(0xFF1976D2) else Color.Gray,
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedIndex == index) Color(0xFF1976D2) else Color.Gray
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFFE3F2FD)
                        )
                    )
                }
            }
        }
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