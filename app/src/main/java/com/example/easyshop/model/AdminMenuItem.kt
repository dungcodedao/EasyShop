package com.example.easyshop.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AdminMenuItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color,
    val count: Int? = null
)
