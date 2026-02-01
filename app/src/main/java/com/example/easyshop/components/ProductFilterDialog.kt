package com.example.easyshop.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProductFilterDialog(
    brandList: List<String>,
    selectedBrand: String,
    onBrandSelected: (String) -> Unit,
    selectedPriceSort: String,
    onPriceSortSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter & Sort") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Brand Section
                item {
                    Text(
                        "Brand",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(brandList) { brand ->
                    FilterItem(
                        text = brand,
                        isSelected = brand == selectedBrand,
                        onClick = { onBrandSelected(brand) }
                    )
                }

                // Divider
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Price Sort Section
                item {
                    Text(
                        "Sort by Price",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(listOf("Default", "Price: Low to High", "Price: High to Low")) { sortOption ->
                    FilterItem(
                        text = sortOption,
                        isSelected = sortOption == selectedPriceSort,
                        onClick = { onPriceSortSelected(sortOption) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onReset()
                onDismiss()
            }) {
                Text("Reset")
            }
        }
    )
}

@Composable
private fun FilterItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text)

        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}