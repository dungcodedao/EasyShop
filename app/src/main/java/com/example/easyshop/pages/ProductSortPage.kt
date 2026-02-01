package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        title = { Text("Filter & Sort Products") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Brand Filter
                FilterDropdown(
                    label = "Brand",
                    value = selectedBrand,
                    options = brandList,
                    onSelected = onBrandSelected
                )

                // Price Sort
                FilterDropdown(
                    label = "Sort by Price",
                    value = selectedPriceSort,
                    options = listOf("Default", "Price: Low to High", "Price: High to Low"),
                    onSelected = onPriceSortSelected
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}