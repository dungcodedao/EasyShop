package com.example.easyshop.admin.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.easyshop.R
import com.example.easyshop.admin.viewmodel.AdminChatViewModel
import com.example.easyshop.model.ChatSession
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatListScreen(
    navController: NavController,
    viewModel: AdminChatViewModel = viewModel()
) {
    val sessions by viewModel.chatSessions.collectAsState()
    val noSessionsText = stringResource(R.string.admin_chat_no_sessions)
    val defaultCustomerText = stringResource(R.string.admin_chat_default_customer)
    val backCd = stringResource(R.string.cd_back)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.admin_chat_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, backCd)
                    }
                }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(noSessionsText, color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(sessions) { session ->
                    ChatSessionItem(session, defaultCustomerName = defaultCustomerText) {
                        val encodedName = java.net.URLEncoder.encode(
                            session.userName.ifBlank { defaultCustomerText }, "UTF-8"
                        )
                        val encodedAvatar = session.userProfileImage
                            ?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: "null"
                        navController.navigate(
                            "admin-chat-detail/${session.userId}?userName=$encodedName&userAvatar=$encodedAvatar"
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
fun ChatSessionItem(
    session: ChatSession,
    defaultCustomerName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (!session.userProfileImage.isNullOrEmpty()) {
                AsyncImage(
                    model = session.userProfileImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.userName.ifBlank { defaultCustomerName },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (session.unreadCountByAdmin > 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                val locale = LocalConfiguration.current.locales[0]
                val time = remember(locale) {
                    SimpleDateFormat("HH:mm", locale).format(session.lastTimestamp.toDate())
                }
                Text(time, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Text(
                text = session.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (session.unreadCountByAdmin > 0) Color.Black else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (session.unreadCountByAdmin > 0) FontWeight.Bold else FontWeight.Normal
            )
        }

        if (session.unreadCountByAdmin > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = session.unreadCountByAdmin.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
