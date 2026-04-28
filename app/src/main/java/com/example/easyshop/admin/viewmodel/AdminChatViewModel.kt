package com.example.easyshop.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easyshop.model.ChatSession
import com.example.easyshop.model.ShopChatMessage
import com.example.easyshop.repository.ChatRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()
    private val currentUser = auth.currentUser

    // Danh sách các cuộc hội thoại
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    // Tin nhắn của một user cụ thể đang được chọn
    private val _messages = MutableStateFlow<List<ShopChatMessage>>(emptyList())
    val messages: StateFlow<List<ShopChatMessage>> = _messages.asStateFlow()

    private val _selectedUserId = MutableStateFlow<String?>(null)
    val selectedUserId: StateFlow<String?> = _selectedUserId.asStateFlow()

    private val userCache = mutableMapOf<String, com.example.easyshop.model.UserModel>()

    init {
        // Lấy danh sách chat sessions thời gian thực
        viewModelScope.launch   {
            repository.getAllChatSessions().collect { sessions ->
                val updatedSessions = sessions.map { session ->
                    var userModel = userCache[session.userId]
                    if (userModel == null) {
                        try {
                            val userDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(session.userId)
                                .get()
                                .await()
                            userModel = userDoc.toObject(com.example.easyshop.model.UserModel::class.java)
                            if (userModel != null) {
                                userCache[session.userId] = userModel
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    if (userModel != null) {
                        session.copy(
                            userName = userModel.name.takeIf { it.isNotBlank() } ?: session.userName,
                            userProfileImage = userModel.profileImg.takeIf { it.isNotBlank() } ?: session.userProfileImage
                        )
                    } else session
                }
                _chatSessions.value = updatedSessions
            }
        }
    }

    private var messageJob: kotlinx.coroutines.Job? = null

    /**
     * Chọn một user để chat
     */
    fun selectUser(userId: String) {
        _selectedUserId.value = userId
        // Hủy lắng nghe cũ nếu có để tránh memory leak và tốn băng thông
        messageJob?.cancel()
        
        messageJob = viewModelScope.launch {
            repository.getMessages(userId).collect {
                _messages.value = it
                repository.markAsRead(userId, isAdmin = true)
            }
        }
    }

    fun sendMessage(text: String) {
        val userId = _selectedUserId.value ?: return
        val adminName = currentUser?.displayName ?: "Admin"
        
        viewModelScope.launch {
            val message = ShopChatMessage(
                senderId = currentUser?.uid ?: "admin",
                senderName = adminName,
                text = text,
                timestamp = Timestamp.now()
            )
            repository.sendMessage(message, userId, "User", isAdmin = true) // userId ở đây là ID của khách hàng
        }
    }

    fun deleteChatSession(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteChatSession(userId)
            onSuccess()
        }
    }
}

