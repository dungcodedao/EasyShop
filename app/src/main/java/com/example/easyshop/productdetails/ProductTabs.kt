package com.example.easyshop.productdetails

import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easyshop.R

@Composable
fun ProductTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val titles = listOf(
        stringResource(id = R.string.product_description),
        stringResource(id = R.string.specifications),
        stringResource(id = R.string.write_a_review)
    )
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = MaterialTheme.colorScheme.primary,
                height = 3.dp
            )
        }
    ) {
        titles.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        title,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            )
        }
    }
}