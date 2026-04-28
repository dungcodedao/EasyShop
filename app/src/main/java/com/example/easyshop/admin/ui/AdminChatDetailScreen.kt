package com.example.easyshop.admin.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.R
import com.example.easyshop.admin.viewmodel.AdminChatViewModel
import com.example.easyshop.model.ShopChatMessage
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatDetailScreen(
    navController: NavController,
    userId: String,
    userName: String? = null,
    userAvatar: String? = null,
    viewModel: AdminChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val auth = FirebaseAuth.getInstance()
    val currentAdminId = remember { auth.currentUser?.uid }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.uploadAndSendImage(context, it) }
        }
    )

    // Load messages for selected user
    LaunchedEffect(userId) {
        viewModel.selectUser(userId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val chatSessions by viewModel.chatSessions.collectAsState()
    val chatSession = chatSessions.find { it.userId == userId }
    val lastTimestamp = chatSession?.lastTimestamp

    val displayUserName = chatSession?.userName?.takeIf { it.isNotBlank() } ?: userName ?: "Khách hàng"
    val displayUserAvatar = chatSession?.userProfileImage?.takeIf { it.isNotBlank() } ?: userAvatar

    var statusText by remember { mutableStateOf("Đang tải...") }
    LaunchedEffect(lastTimestamp) {
        while (true) {
            if (lastTimestamp != null) {
                val now = System.currentTimeMillis() / 1000
                val diff = now - lastTimestamp.seconds
                statusText = when {
                    diff < 60 -> "Đang hoạt động"
                    diff < 3600 -> "Hoạt động ${diff / 60} phút trước"
                    diff < 86400 -> "Hoạt động ${diff / 3600} giờ trước"
                    else -> "Hoạt động ${diff / 86400} ngày trước"
                }
            } else {
                statusText = "Đang tải thông tin..."
            }
            kotlinx.coroutines.delay(60000) // update every minute
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!displayUserAvatar.isNullOrEmpty()) {
                            AsyncImage(
                                model = displayUserAvatar,
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4F46E5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayUserName.takeIf { it.isNotBlank() }?.take(1)?.uppercase() ?: "K",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = displayUserName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (statusText == "Đang hoạt động") Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete Chat", tint = MaterialTheme.colorScheme.error)
                    }
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text(stringResource(R.string.delete_chat_confirm_title)) },
                            text = { Text(stringResource(R.string.delete_chat_confirm_msg)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteDialog = false
                                    viewModel.deleteChatSession(userId) {
                                        navController.popBackStack()
                                    }
                                }) {
                                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Hủy")
                                }
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp, 
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding().imePadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút gửi ảnh
                    IconButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploadingImage
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Gửi ảnh",
                                tint = Color(0xFF4F46E5)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Nhập phản hồi...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (inputText.isNotBlank()) Color(0xFF4F46E5) else Color.Gray)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    val isMe = message.senderId == currentAdminId
                    AdminSimpleChatBubble(message, isMe) 
                }
            }
        }
    }
}

@Composable
fun AdminSimpleChatBubble(message: ShopChatMessage, isMe: Boolean) {
    val hasContent = message.text.isNotBlank() || !message.imageUrl.isNullOrEmpty()
    if (!hasContent) return

    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor = Color.White
    val adminAccent = Color(0xFF4F46E5)
    val textColor = if (isMe) adminAccent else Color.Black
    val borderColor = if (isMe) adminAccent.copy(alpha = 0.5f) else Color(0xFFE0E0E0)
    
    val shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isMe) 16.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            shadowElevation = 0.5.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(if (message.imageUrl.isNullOrEmpty()) 12.dp else 4.dp)) {
                if (!message.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                    if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = if (!message.imageUrl.isNullOrEmpty()) Modifier.padding(horizontal = 8.dp, vertical = 4.dp) else Modifier
                    )
                }
            }
        }
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp.toDate())
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}
