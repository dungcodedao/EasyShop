package com.example.easyshop.productdetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddToCartButton(inStock: Boolean, onAddToCart: () -> Unit) {
    var buttonScale by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            buttonScale = 0.95f
            onAddToCart()
            scope.launch { delay(100); buttonScale = 1f }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(buttonScale),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (inStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (inStock) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        ),
        enabled = inStock,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (inStock) 4.dp else 0.dp)
    ) {
        Icon(
            imageVector = if (inStock) Icons.Default.ShoppingCart else Icons.Default.Clear,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (inStock) stringResource(id = R.string.add_to_cart_btn) else stringResource(id = R.string.out_of_stock_btn),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}