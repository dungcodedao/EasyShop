package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easyshop.R

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
        title = { Text(stringResource(id = R.string.filter_sort_products)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Brand Filter
                FilterDropdown(
                    label = stringResource(id = R.string.brand),
                    value = selectedBrand,
                    options = brandList,
                    onSelected = onBrandSelected
                )

                // Price Sort
                FilterDropdown(
                    label = stringResource(id = R.string.sort_by_price),
                    value = selectedPriceSort,
                    options = listOf(
                        stringResource(id = R.string.sort_default),
                        stringResource(id = R.string.sort_price_low_high),
                        stringResource(id = R.string.sort_price_high_low)
                    ),
                    onSelected = onPriceSortSelected
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onReset()
                onDismiss()
            }) {
                Text(stringResource(id = R.string.reset))
            }
        }
    )
}

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