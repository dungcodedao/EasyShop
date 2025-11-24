package com.example.easyshop.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.componmets.BannerView
import com.example.easyshop.componmets.CategoriesView
import com.example.easyshop.componmets.HeaderView
import com.example.easyshop.componmets.SearchView

@Composable
fun HomePage(modifier: Modifier = Modifier) {
    var isSearching by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()
        .padding(16.dp)) {
        if (isSearching) {
            // Hiển thị màn hình tìm kiếm
            SearchView(
                onBackClick = { isSearching = false }
            )
        } else {
            HeaderView(onSearchClick = { isSearching = true })
            Spacer(modifier = Modifier.height(10.dp))
            BannerView(modifier = Modifier.height(150.dp))
            Text(
                "Categories",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            CategoriesView()
        }
    }
}