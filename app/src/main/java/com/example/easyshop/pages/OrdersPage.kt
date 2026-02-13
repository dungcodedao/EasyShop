package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.R
import com.example.easyshop.GlobalNavigation.navController
import com.example.easyshop.components.OrderView
import com.example.easyshop.model.OrderModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersPage(modifier: Modifier = Modifier) {
    val orderList = remember { mutableStateOf<List<OrderModel>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val tabs = listOf(stringResource(id = R.string.processing), stringResource(id = R.string.history))

    val filteredList = remember(selectedTab, orderList.value) {
        when (selectedTab) {
            0 -> orderList.value.filter { it.status == "ORDERED" || it.status == "SHIPPING" }
            else -> orderList.value.filter { it.status == "DELIVERED" || it.status == "CANCELLED" }
        }
    }

    LaunchedEffect(key1 = Unit) {
        isLoading = true
        Firebase.firestore.collection("orders")
            .whereEqualTo("userId", FirebaseAuth.getInstance().currentUser?.uid!!)
            .get().addOnCompleteListener {
                if (it.isSuccessful) {
                    orderList.value = it.result.documents.mapNotNull { doc -> doc.toObject(OrderModel::class.java) }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.your_orders), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 3.dp,
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
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "📦", style = androidx.compose.ui.text.TextStyle(fontSize = 40.sp))
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = if (selectedTab == 0) stringResource(id = R.string.no_active_orders) else stringResource(id = R.string.no_order_history),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = filteredList, key = { it.id }) {
                        OrderView(it)
                    }
                }
            }
        }
    }
}