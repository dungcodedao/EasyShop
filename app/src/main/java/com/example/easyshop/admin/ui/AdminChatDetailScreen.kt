package com.example.easyshop.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.easyshop.admin.viewmodel.AdminChatViewModel
import com.example.easyshop.chat.ui.SimpleChatBubble
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.res.stringResource
import com.example.easyshop.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatDetailScreen(
    navController: NavController,
    userId: String,
    viewModel: AdminChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val auth = FirebaseAuth.getInstance()
    val currentAdminId = remember { auth.currentUser?.uid }

    // Load messages for selected user
    LaunchedEffect(userId) {
        viewModel.selectUser(userId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đang trả lời khách hàng", style = MaterialTheme.typography.titleMedium) },
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
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F3F4))
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    val isAdmin = message.senderId == currentAdminId
                    SimpleChatBubble(message, isUser = isAdmin) // Admin is the 'user' of this bubble style here
                }
            }
        }
    }
}
