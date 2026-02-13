package com.example.easyshop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun HeaderView(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }

    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
            .get().addOnCompleteListener {
                name = it.result.getString("name") ?: ""
            }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (name.isNotEmpty()) name.first().uppercase() else "?",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(14.dp))

        // Welcome text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.welcome_back),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Search button
        FilledTonalIconButton(
            onClick = onSearchClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}