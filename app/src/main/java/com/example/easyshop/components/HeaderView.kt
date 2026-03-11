package com.example.easyshop.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun HeaderView(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }

    LaunchedEffect(key1 = Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        Firebase.firestore.collection("users").document(uid)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val doc = task.result
                    name = doc.getString("name") ?: ""
                    avatarUrl = doc.getString("profileImg") ?: ""
                }
            }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar — click để vào Profile
        if (avatarUrl.isNotEmpty()) {
            coil.compose.AsyncImage(
                model = avatarUrl,
                contentDescription = "Ảnh đại diện",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAvatarClick),
                contentScale = ContentScale.Crop
            )
        } else {
            // Default placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
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