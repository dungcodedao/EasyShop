package com.example.easyshop.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.easyshop.R

@Composable
fun ErrorStateView(
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    buttonText: String? = null,
    icon: ImageVector = Icons.Default.ErrorOutline,
    onRetry: () -> Unit
) {
    val resolvedTitle   = title      ?: androidx.compose.ui.res.stringResource(R.string.error_title)
    val resolvedMessage = message    ?: androidx.compose.ui.res.stringResource(R.string.error_message)
    val resolvedButton  = buttonText ?: androidx.compose.ui.res.stringResource(R.string.error_retry)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = resolvedTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = resolvedMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = onRetry,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.height(48.dp).padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(text = resolvedButton, fontWeight = FontWeight.Bold)
        }
    }
}
