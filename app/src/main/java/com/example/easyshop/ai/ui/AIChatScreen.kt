package com.example.easyshop.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.ai.viewmodel.AIChatViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private val ChatGradientTop = Color(0xFFF4F7FF)
private val ChatGradientBottom = Color(0xFFE9F1FF)
private val ChatAccentStart = Color(0xFF4F46E5)
private val ChatAccentEnd = Color(0xFF0EA5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    navController: NavController,
    viewModel: AIChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val error by viewModel.error.collectAsState()
    val typingMessage by viewModel.typingMessage.collectAsState()
    val speechText by viewModel.speechText.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    val listState = rememberLazyListState()

    // Anti-spam click state
    var lastClickTime by remember { mutableStateOf(0L) }
    val clickThrottleMs = 600L

    fun navigateSafely(route: String, singleTop: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > clickThrottleMs) {
            lastClickTime = currentTime
            navController.navigate(route) {
                if (singleTop) {
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(speechText) {
        if (speechText.isNotBlank()) {
            inputText = if (inputText.isBlank()) speechText else "$inputText $speechText"
            viewModel.clearSpeechText()
        }
    }

    LaunchedEffect(messages.size, typingMessage) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, it)
                        ImageDecoder.decodeBitmap(source)
                    }
                    selectedImage = bitmap
                } catch (e: Exception) {
                    Log.e("AIChat", "Error loading image", e)
                }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening(context)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            ChatTopBar(
                onBack = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime > clickThrottleMs) {
                        lastClickTime = currentTime
                        navController.popBackStack()
                    }
                },
                onClear = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime > clickThrottleMs) {
                        lastClickTime = currentTime
                        viewModel.clearChat()
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 10.dp,
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column {
                    if (error != null) {
                        ErrorMessage(error!!) { viewModel.clearError() }
                    }

                    AnimatedVisibility(visible = selectedImage != null) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            selectedImage?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Selected",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(2.dp, ChatAccentStart, RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { selectedImage = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-8).dp)
                                        .size(24.dp)
                                        .background(Color.Red, CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    ChatInput(
                        value = inputText,
                        onValueChange = { inputText = it },
                        isLoading = isLoading,
                        isListening = isListening,
                        selectedImageAttached = selectedImage != null,
                        onMicClick = {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClickTime > clickThrottleMs) {
                                lastClickTime = currentTime
                                if (isListening) {
                                    viewModel.stopListening()
                                } else {
                                    val permission = Manifest.permission.RECORD_AUDIO
                                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                        viewModel.startListening(context)
                                    } else {
                                        permissionLauncher.launch(permission)
                                    }
                                }
                            }
                        },
                        onImageClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onImagePaste = { bitmap ->
                            selectedImage = bitmap
                        },
                        onSend = {
                            val textInput = inputText.trim()
                            if (selectedImage != null) {
                                viewModel.sendImageMessage(textInput, selectedImage!!)
                                inputText = ""
                                selectedImage = null
                            } else if (textInput.isNotBlank() && !isLoading) {
                                viewModel.sendMessage(textInput)
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
                .background(Brush.verticalGradient(colors = listOf(ChatGradientTop, ChatGradientBottom)))
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 14.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty() && !isLoading) {
                    item { WelcomeHintCard() }
                }

                // Hiển thị gợi ý nhanh nếu chưa có tin nhắn nào từ phía USER
                val hasUserMessage = messages.any { it.isUser }
                if (!hasUserMessage && !isLoading) {
                    item {
                        SuggestionChips(onSuggestionClick = { suggestion ->
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClickTime > clickThrottleMs) {
                                lastClickTime = currentTime
                                inputText = "" // Đảm bảo xóa text khi click gợi ý
                                viewModel.sendMessage(suggestion)
                            }
                        })
                    }
                }

                items(messages) { message ->
                    ChatBubble(
                        content = message.content,
                        isUser = message.isUser,
                        timestamp = message.timestamp,
                        imageUrl = message.imageUrl,
                        onProductClick = { navigateSafely("product-details/$it", true) }
                    )
                }

                if (typingMessage != null) {
                    item {
                        if (typingMessage!!.isEmpty()) {
                            TypingIndicator()
                        } else {
                            ChatBubble(
                                content = typingMessage!! + " █",
                                isUser = false,
                                timestamp = null,
                                onProductClick = { navigateSafely("product-details/$it", true) }
                            )
                        }
                    }
                } else if (isLoading) {
                    item { TypingIndicator() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(onBack: () -> Unit, onClear: () -> Unit) {
    Surface(
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ChatAccentStart, ChatAccentEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Trợ lý AI", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FiberManualRecord, null, tint = Color(0xFF22C55E), modifier = Modifier.size(8.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Đang online", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            },
            actions = {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteSweep, "Clear chat", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun WelcomeHintCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Tư vấn nhanh theo nhu cầu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Hãy gửi tin nhắn, giọng nói hoặc hình ảnh sản phẩm để Shop tư vấn nhanh nhất cho bạn nhé!",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ChatBubble(
    content: String,
    isUser: Boolean,
    timestamp: Timestamp?,
    imageUrl: String? = null,
    onProductClick: (String) -> Unit
) {
    val userBrush = Brush.linearGradient(colors = listOf(ChatAccentStart, ChatAccentEnd))
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = RoundedCornerShape(
        topStart = 18.dp, topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 8.dp,
        bottomEnd = if (isUser) 8.dp else 18.dp
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        val baseMod = Modifier
            .widthIn(max = 300.dp)
            .shadow(if (isUser) 4.dp else 1.dp, shape)
            .clip(shape)

        val backgroundMod = if (isUser) {
            baseMod.background(userBrush)
        } else {
            baseMod.background(MaterialTheme.colorScheme.surface)
        }

        val finalModifier = if (!isUser) {
            backgroundMod.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), shape)
        } else {
            backgroundMod
        }

        Box(
            modifier = finalModifier.padding(horizontal = 0.dp, vertical = 0.dp)
        ) {
            Column {
                imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Chat Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                if (content.isNotBlank()) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        MarkdownText(
                            text = content,
                            textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                            onProductClick = onProductClick
                        )
                    }
                }
            }
        }

        timestamp?.let {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(it.toDate())
            Text(time, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    isListening: Boolean,
    selectedImageAttached: Boolean = false,
    onMicClick: () -> Unit,
    onImageClick: () -> Unit,
    onImagePaste: (Bitmap) -> Unit,
    onSend: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
    }

    fun tryReadImageFromClipboard(): Boolean {
        val clip = clipboardManager.primaryClip ?: run {
            Log.d("AIChatPaste", "Clipboard trống")
            return false
        }
        Log.d("AIChatPaste", "Clipboard có ${clip.itemCount} items, mimeTypes: ${
            (0 until clip.description.mimeTypeCount).map { clip.description.getMimeType(it) }
        }")

        for (i in 0 until clip.itemCount) {
            val uri = clip.getItemAt(i).uri
            Log.d("AIChatPaste", "Item[$i] uri=$uri")
            uri ?: continue
            return try {
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                }
                Log.d("AIChatPaste", "✅ Bitmap decoded thành công: ${bitmap.width}x${bitmap.height}")
                onImagePaste(bitmap)
                true
            } catch (e: Exception) {
                Log.e("AIChatPaste", "❌ Lỗi decode: ${e.message}")
                false
            }
        }
        return false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onImageClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.AddAPhoto, "Vision", tint = ChatAccentStart)
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown
                        && keyEvent.key == Key.V
                        && keyEvent.isCtrlPressed
                    ) {
                        Log.d("AIChatPaste", "Ctrl+V detected!")
                        val handled = tryReadImageFromClipboard()
                        Log.d("AIChatPaste", "Ctrl+V handled as image: $handled")
                        handled // true = consume, false = pass xuống TextField (dán text)
                    } else false
                }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .contentReceiver { contentInfo ->
                        Log.d("AIChatPaste", "contentReceiver triggered, hasImage: ${
                            contentInfo.hasMediaType(MediaType.Image)
                        }")
                        if (contentInfo.hasMediaType(MediaType.Image)) {
                            val clip = contentInfo.clipEntry.clipData
                            for (i in 0 until clip.itemCount) {
                                val uri = clip.getItemAt(i).uri ?: continue
                                try {
                                    val bitmap: Bitmap = if (Build.VERSION.SDK_INT < 28) {
                                        @Suppress("DEPRECATION")
                                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                                    } else {
                                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                        }
                                    }
                                    Log.d("AIChatPaste", "✅ contentReceiver: bitmap OK")
                                    onImagePaste(bitmap)
                                } catch (e: Exception) {
                                    Log.e("AIChatPaste", "contentReceiver lỗi: ${e.message}")
                                }
                            }
                            null // consume
                        } else {
                            contentInfo
                        }
                    },
                placeholder = { Text(if (isListening) "Đang nghe..." else "Nhập tin nhắn...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = if (isListening) Color.Red else ChatAccentStart.copy(0.5f)
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isListening) Color.Red.copy(0.1f) else Color.Transparent,
                    CircleShape
                )
        ) {
            Icon(
                if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                "STT",
                tint = if (isListening) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onSend,
            enabled = (value.isNotBlank() || selectedImageAttached) && !isLoading && !isListening,
            shape = CircleShape,
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ChatAccentStart)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(start = 4.dp).border(1.dp, MaterialTheme.colorScheme.outline.copy(0.1f), RoundedCornerShape(16.dp))) {
        Text("AI đang nghĩ...", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Close, null) }
        }
    }
}

@Composable
fun MarkdownText(text: String, textColor: Color, onProductClick: (String) -> Unit) {
    val productIds = remember(text) {
        // Chỉ match đúng pattern [PID_xxx], tránh bắt nhầm [Còn hàng], [...] trong Markdown
        Regex("\\[PID_([^\\]]+)]").findAll(text)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    val contentWithoutPids = remember(text) {
        text.replace(Regex("\\[PID_[^\\]]+]"), "").trim()
    }

    Column {
        Text(
            text = parseMarkdown(contentWithoutPids),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
        )

        if (productIds.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                items(productIds.take(3)) { productId ->
                    AssistChip(
                        onClick = { onProductClick(productId) },
                        label = { Text("Xem sản phẩm", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = ChatAccentStart,
                            leadingIconContentColor = ChatAccentStart
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun parseMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    val words = text.split(Regex("(?<=\\*\\*)|(?=\\*\\*)"))
    return androidx.compose.ui.text.buildAnnotatedString {
        var isBold = false
        words.forEach { word ->
            if (word == "**") {
                isBold = !isBold
            } else {
                if (isBold) {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                    append(word)
                    pop()
                } else {
                    append(word)
                }
            }
        }
    }
}

@Composable
fun SuggestionChips(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf("Laptop 15 triệu", "PC Gaming", "Deal tốt", "iPhone đời mới")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(suggestions) { s ->
            AssistChip(onClick = { onSuggestionClick(s) }, label = { Text(s) })
        }
    }
}