package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.GlobalNavigation.navController
import com.example.easyshop.components.OrderView
import com.example.easyshop.model.OrderModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun OrdersPage(modifier: Modifier = Modifier) {

    val orderList = remember {
        mutableStateOf<List<OrderModel>>(emptyList())
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Processing", "History")

    val filteredList = remember(selectedTab, orderList.value) {
        when (selectedTab) {
            0 -> orderList.value.filter { it.status == "ORDERED" || it.status == "SHIPPING" }
            else -> orderList.value.filter { it.status == "DELIVERED" || it.status == "CANCELLED" }
        }
    }


    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("orders")
            .whereEqualTo("userId", FirebaseAuth.getInstance().currentUser?.uid!!)
            .get().addOnCompleteListener() {
                if (it.isSuccessful) {
                    val resultList = it.result.documents.mapNotNull { doc ->
                        doc.toObject(OrderModel::class.java)
                    }
                    orderList.value = resultList
                }
            }

    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Your orders",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = TextStyle(
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        )
                    }
                )
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedTab == 0) "No active orders" else "No order history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) {
                    OrderView(it)
                }
            }
        }
    }
}