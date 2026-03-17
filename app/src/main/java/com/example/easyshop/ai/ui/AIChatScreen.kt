package com.example.easyshop.ai.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.easyshop.ai.viewmodel.AIChatViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    navController: NavController,
    viewModel: AIChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Trợ lý AI Pro",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(8.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Đang trực tuyến",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Xoá cuộc trò chuyện",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column {
                    if (error != null) {
                        ErrorMessage(error!!) { viewModel.clearError() }
                    }

                    if (messages.isEmpty() && !isLoading) {
                        SuggestionChips(onSuggestionClick = { suggestion ->
                            viewModel.sendMessage(suggestion)
                        })
                    }

                    ChatInput(
                        value = inputText,
                        onValueChange = { inputText = it },
                        isLoading = isLoading,
                        onSend = {
                            if (inputText.isNotBlank() && !isLoading) {
                                viewModel.sendMessage(inputText.trim())
                                inputText = ""
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(
                        content = message.content,
                        isUser = message.isUser,
                        timestamp = message.timestamp
                    )
                }
                if (isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(content: String, isUser: Boolean, timestamp: Timestamp?) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val bubbleBrush = remember(isUser, primaryColor, secondaryColor, surfaceVariantColor) {
        if (isUser) {
            Brush.linearGradient(colors = listOf(primaryColor, secondaryColor))
        } else {
            Brush.linearGradient(colors = listOf(surfaceVariantColor, surfaceVariantColor))
        }
    }

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 18.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .shadow(2.dp, shape)
                .background(bubbleBrush, shape)
                .clip(shape)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            MarkdownText(
                text = content,
                textColor = if (isUser) Color.White else onSurfaceVariantColor
            )
        }
        
        timestamp?.let {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate())
            Text(
                text = time,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariantColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(28.dp)),
            placeholder = { Text("Nhắn tin cho Trợ lý AI...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(28.dp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        FloatingActionButton(
            onClick = onSend,
            containerColor = if (value.isNotBlank() && !isLoading) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (value.isNotBlank() && !isLoading) 
                Color.White 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                "Đang trả lời...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Text("✕", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, textColor: Color) {
    val lines = text.split("\n")
    Column {
        lines.forEach { line ->
            val annotatedString = buildAnnotatedString {
                val trimmedLine = line.trimStart()
                var processedLine = trimmedLine
                
                // 1. Chuyển đổi dấu (*) hoặc (-) đầu dòng thành bullet bullet (•)
                if (processedLine.startsWith("* ") || processedLine.startsWith("- ")) {
                    append("•  ")
                    processedLine = processedLine.substring(2)
                }

                // 2. Tìm ID Sản phẩm trong ngoặc vuông và loại bỏ đi để text không bị xấu
                val productIdRegex = Regex("\\[(.*?)\\]")
                processedLine = processedLine.replace(productIdRegex, "")

                // 3. Tìm và bôi đậm những chữ bị kẹp trong **chữ**
                val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
                var currentIndex = 0
                var matchResult = boldRegex.find(processedLine, currentIndex)
                
                while (matchResult != null) {
                    // Thêm đoạn text thường trước chỗ in đậm
                    append(processedLine.substring(currentIndex, matchResult.range.first))
                    
                    // Thêm đoạn chữ in đậm (bỏ 2 dấu ** đi)
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(matchResult.groupValues[1])
                    }
                    
                    currentIndex = matchResult.range.last + 1
                    matchResult = boldRegex.find(processedLine, currentIndex)
                }
                
                // Thêm nốt phần text còn lại
                if (currentIndex < processedLine.length) {
                    append(processedLine.substring(currentIndex))
                }
            }

            Text(
                text = annotatedString,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                modifier = Modifier.padding(vertical = 1.dp)
            )
            
            // 👉 Render nút xem sản phẩm (nếu tìm thấy ID bị kẹp)
            val idMatch = Regex("\\[(.*?)\\]").find(line)
            if (idMatch != null) {
                var productId = idMatch.groupValues[1].trim()
                productId = productId.replace("PID_", "")
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { com.example.easyshop.GlobalNavigation.navController.navigate("product-details/$productId") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xem sản phẩm này", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun SuggestionChips(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf(
        "Bạn có thể giúp gì cho tôi?",
        "Giá iPhone hiện tại bao nhiêu?",
        "Sản phẩm nào đang giảm giá?",
        "Gợi ý laptop văn phòng mỏng nhẹ",
        "Chính sách bảo hành tại Shop"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { suggestion ->
            AssistChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion, style = MaterialTheme.typography.labelMedium) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    labelColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
