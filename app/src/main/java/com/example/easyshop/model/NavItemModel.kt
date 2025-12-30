package com.example.easyshop.model

import androidx.compose.ui.graphics.vector.ImageVector


data class NavItemModel(

    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)