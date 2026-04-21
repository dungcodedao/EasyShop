package com.example.easyshop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun HeaderView(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }

    var unreadCount by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        Firebase.firestore.collection("users").document(uid)
            .get().addOnSuccessListener { doc ->
                name = doc.getString("name") ?: ""
                avatarUrl = doc.getString("profileImg") ?: ""
            }

        // Theo dõi số lượng thông báo chưa đọc (cả cá nhân và broadcast)
        Firebase.firestore.collection("notifications")
            .whereEqualTo("recipientRole", "user")
            .whereIn("userId", listOf(uid, "broadcast"))
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    unreadCount = snapshot.documents.size
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
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
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

        Spacer(Modifier.width(8.dp))

        // Notification Bell
        Box(modifier = Modifier.size(44.dp)) {
            FilledTonalIconButton(
                onClick = onNotificationClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // Badge (Chỉ hiện khi có tin chưa đọc)
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                        .size(16.dp)
                        .background(androidx.compose.ui.graphics.Color.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
