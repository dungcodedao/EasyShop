package com.example.easyshop.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    // Drawable resource ID của avatar, mặc định profile_nam
    var avatarRes by remember { mutableStateOf(R.drawable.profile_nam) }

    LaunchedEffect(key1 = Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        Firebase.firestore.collection("users").document(uid)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val doc = task.result
                    name = doc.getString("name") ?: ""
                    // Map tên drawable string → resource ID
                    avatarRes = when (doc.getString("avatar") ?: "profile_nam") {
                        "profile_nam2" -> R.drawable.profile_nam2
                        "profile_nu"   -> R.drawable.profile_nu
                        "profile_nu2"  -> R.drawable.profile_nu2
                        else           -> R.drawable.profile_nam
                    }
                }
            }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar từ drawable (giống trang Profile)
        Image(
            painter = painterResource(avatarRes),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

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