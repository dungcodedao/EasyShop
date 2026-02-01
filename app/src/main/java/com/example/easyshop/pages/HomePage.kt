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
import com.example.easyshop.components.BannerView
import com.example.easyshop.components.CategoriesView
import com.example.easyshop.components.HeaderView
import com.example.easyshop.components.SearchView

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold

@Composable
fun HomePage(modifier: Modifier = Modifier) {
    var isSearching by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        if (isSearching) {
            SearchView(
                onBackClick = { isSearching = false }
            )
        } else {
            HeaderView(onSearchClick = { isSearching = true })
            Spacer(modifier = Modifier.height(10.dp))
            BannerView(modifier = Modifier.height(150.dp))

            Spacer(modifier = Modifier.height(10.dp))
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