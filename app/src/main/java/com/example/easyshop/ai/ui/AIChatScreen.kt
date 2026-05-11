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
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.R
import com.example.easyshop.ai.viewmodel.AIChatViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private val ChatGradientTop = Color(0xFFF4F7FF)
private val ChatGradientBottom = Color(0xFFE9F1FF)
private val ChatAccentStart = Color(0xFF4F46E5)
private val ChatAccentEnd = Color(0xFF0EA5E9)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val partialSpeechText by viewModel.partialSpeechText.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    val listState = rememberLazyListState()

    var lastClickTime by remember { mutableStateOf(0L) }
    val clickThrottleMs = 600L

    fun navigateSafely(route: String, singleTop: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > clickThrottleMs) {
            lastClickTime = currentTime
            navController.navigate(route) {
                if (singleTop) launchSingleTop = true
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkAndShowWelcomeMessage(context)
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
                        @Suppress("DEPRECATION")
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
        if (isGranted) viewModel.startListening(context)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            ChatTopBar(
                context = context,
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
                modifier = Modifier.navigationBarsPadding().imePadding()
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
                                    contentDescription = context.getString(R.string.cd_selected),
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
                        partialSpeechText = partialSpeechText,
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
                                viewModel.sendImageMessage(textInput, selectedImage!!, context)
                                inputText = ""
                                selectedImage = null
                            } else if (textInput.isNotBlank() && !isLoading) {
                                viewModel.sendMessage(textInput, context)
                                inputText = ""
                            }
                        },
                        context = context
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(ChatGradientTop, ChatGradientBottom)))
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty() && !isLoading) {
                    item { 
                        WelcomeHintCard(
                            title = context.getString(R.string.ai_welcome_card_title),
                            description = context.getString(R.string.ai_welcome_card_desc)
                        ) 
                    }
                }

                val hasUserMessage = messages.any { it.isUser }
                if (!hasUserMessage && !isLoading) {
                    item {
                        SuggestionChips(onSuggestionClick = { suggestion ->
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClickTime > clickThrottleMs) {
                                lastClickTime = currentTime
                                inputText = ""
                                viewModel.sendMessage(suggestion, context)
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
                        context = context,
                        onProductClick = { navigateSafely("product-details/$it", true) }
                    )
                }

                if (typingMessage != null) {
                    item {
                        if (typingMessage!!.isEmpty()) {
                            TypingIndicator(context)
                        } else {
                            ChatBubble(
                                content = typingMessage!! + " █",
                                isUser = false,
                                timestamp = null,
                                context = context,
                                onProductClick = { navigateSafely("product-details/$it", true) }
                            )
                        }
                    }
                } else if (isLoading) {
                    item { TypingIndicator(context) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(context: android.content.Context, onBack: () -> Unit, onClear: () -> Unit) {
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
                        Text(context.getString(R.string.ai_chat_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FiberManualRecord, null, tint = Color(0xFF22C55E), modifier = Modifier.size(8.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(context.getString(R.string.ai_status_online), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, context.getString(R.string.cd_back)) }
            },
            actions = {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteSweep, context.getString(R.string.cd_clear_chat), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun WelcomeHintCard(title: String, description: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ChatBubble(
    content: String,
    isUser: Boolean,
    timestamp: Timestamp?,
    imageUrl: String? = null,
    context: android.content.Context,
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

        Box(modifier = finalModifier) {
            Column {
                imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = context.getString(R.string.cd_chat_image),
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
            val locale = LocalConfiguration.current.locales[0]
            val time = remember(locale) { SimpleDateFormat("HH:mm", locale).format(it.toDate()) }
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
    partialSpeechText: String = "",
    onMicClick: () -> Unit,
    onImageClick: () -> Unit,
    onImagePaste: (Bitmap) -> Unit,
    onSend: () -> Unit,
    context: android.content.Context
) {
    val clipboardManager = remember {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    }

    fun tryReadImageFromClipboard(): Boolean {
        val clip = clipboardManager.primaryClip ?: return false
        for (i in 0 until clip.itemCount) {
            val uri = clip.getItemAt(i).uri ?: continue
            return try {
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
                }
                onImagePaste(bitmap)
                true
            } catch (e: Exception) { false }
        }
        return false
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onImageClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.AddAPhoto, null, tint = ChatAccentStart)
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier.weight(1f).onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.V && keyEvent.isCtrlPressed) {
                    tryReadImageFromClipboard()
                } else false
            }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().contentReceiver { contentInfo ->
                    if (contentInfo.hasMediaType(MediaType.Image)) {
                        val clip = contentInfo.clipEntry.clipData
                        for (i in 0 until clip.itemCount) {
                            val uri = clip.getItemAt(i).uri ?: continue
                            try {
                                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                                    @Suppress("DEPRECATION")
                                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                                } else {
                                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                                    ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
                                }
                                onImagePaste(bitmap)
                            } catch (e: Exception) {}
                        }
                        null
                    } else contentInfo
                },
                placeholder = {
                    Text(
                        when {
                            partialSpeechText.isNotBlank() -> partialSpeechText
                            isListening -> context.getString(R.string.ai_listening)
                            else -> context.getString(R.string.ai_input_placeholder)
                        }
                    )
                },
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
            modifier = Modifier.size(40.dp).background(if (isListening) Color.Red.copy(0.1f) else Color.Transparent, CircleShape)
        ) {
            Icon(
                if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                context.getString(R.string.stt_label),
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
                Icon(Icons.AutoMirrored.Filled.Send, context.getString(R.string.cd_send), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun TypingIndicator(context: android.content.Context) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(start = 4.dp).border(1.dp, MaterialTheme.colorScheme.outline.copy(0.1f), RoundedCornerShape(16.dp))
    ) {
        Text(context.getString(R.string.ai_thinking), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Close, null) }
        }
    }
}

@Composable
fun MarkdownText(text: String, textColor: Color, onProductClick: (String) -> Unit) {
    val context = LocalContext.current
    val productIds = remember(text) {
        Regex("\\[PID_([^\\]]+)]").findAll(text).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.distinct().toList()
    }
    val contentWithoutPids = remember(text) { text.replace(Regex("\\[PID_[^\\]]+]"), "").trim() }

    Column {
        Text(text = parseMarkdown(contentWithoutPids), color = textColor, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
        if (productIds.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                items(productIds.take(3)) { productId ->
                    AssistChip(
                        onClick = { onProductClick(productId) },
                        label = { Text(context.getString(R.string.ai_view_product), fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = ChatAccentStart, leadingIconContentColor = ChatAccentStart)
                    )
                }
            }
        }
    }
}

private fun parseMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    val words = text.split(Regex("(?<=\\*\\*)|(?=\\*\\*)"))
    return androidx.compose.ui.text.buildAnnotatedString {
        var isBold = false
        words.forEach { word ->
            if (word == "**") isBold = !isBold
            else {
                if (isBold) {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                    append(word)
                    pop()
                } else append(word)
            }
        }
    }
}

@Composable
fun SuggestionChips(onSuggestionClick: (String) -> Unit) {
    val context = LocalContext.current
    val suggestions = listOf(
        context.getString(R.string.ai_suggest_laptop),
        context.getString(R.string.ai_suggest_gaming),
        context.getString(R.string.ai_suggest_deals),
        context.getString(R.string.ai_suggest_iphone)
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        items(suggestions) { s ->
            AssistChip(onClick = { onSuggestionClick(s) }, label = { Text(s) })
        }
    }
}
