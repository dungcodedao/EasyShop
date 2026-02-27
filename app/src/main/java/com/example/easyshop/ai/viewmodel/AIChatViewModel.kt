package com.example.easyshop.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easyshop.ai.model.ChatMessage
import com.example.easyshop.ai.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AIChatViewModel : ViewModel() {

    private val repository = AiRepository()

    init {
        viewModelScope.launch {
            // Mỗi khi vào màn hình Chat, dọn sạch lịch sử cũ để tạo phiên mới
            repository.clearChat()
        }
    }

    val messages: StateFlow<List<ChatMessage>> = repository.getMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.sendMessage(message, messages.value).onFailure { e ->
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch { repository.clearChat() }
    }

    fun clearError() {
        _error.value = null
    }
}
