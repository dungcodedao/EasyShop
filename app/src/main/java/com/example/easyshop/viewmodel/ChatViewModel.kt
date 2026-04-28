package com.example.easyshop.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easyshop.model.ShopChatMessage
import com.example.easyshop.repository.ChatRepository
import com.example.easyshop.util.CloudinaryUploader
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser

    private val _messages = MutableStateFlow<List<ShopChatMessage>>(emptyList())
    val messages: StateFlow<List<ShopChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Trạng thái đang upload ảnh */
    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    init {
        currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                repository.getMessages(uid).collect {
                    _messages.value = it
                    repository.markAsRead(uid, isAdmin = false)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || currentUser == null) return

        viewModelScope.launch {
            val message = ShopChatMessage(
                senderId = currentUser.uid,
                senderName = currentUser.displayName ?: "Khách hàng",
                text = text,
                timestamp = Timestamp.now()
            )
            repository.sendMessage(
                message = message,
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Khách hàng",
                isAdmin = false,
                userProfileImage = currentUser.photoUrl?.toString()
            )
        }
    }

    fun sendImageMessage(imageUrl: String) {
        if (currentUser == null) return
        viewModelScope.launch {
            val message = ShopChatMessage(
                senderId = currentUser.uid,
                senderName = currentUser.displayName ?: "Khách hàng",
                text = "",
                imageUrl = imageUrl,
                timestamp = Timestamp.now()
            )
            repository.sendMessage(
                message = message,
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Khách hàng",
                isAdmin = false,
                userProfileImage = currentUser.photoUrl?.toString()
            )
        }
    }

    /**
     * Upload ảnh từ URI lên Cloudinary rồi gửi tin nhắn ảnh.
     */
    fun uploadAndSendImage(context: Context, uri: Uri) {
        if (currentUser == null) return
        viewModelScope.launch {
            _isUploadingImage.value = true
            try {
                val imageUrl = CloudinaryUploader.uploadFromUri(context, uri, folder = "chat_images")
                if (imageUrl != null) {
                    sendImageMessage(imageUrl)
                }
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    fun deleteChatSession(onSuccess: () -> Unit) {
        val uid = currentUser?.uid ?: return
        viewModelScope.launch {
            repository.deleteChatSession(uid)
            onSuccess()
        }
    }
}
