package com.example.easyshop.ai.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.util.Log
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
import java.io.ByteArrayOutputStream

class AIChatViewModel : ViewModel() {

    private val repository = AiRepository()
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        viewModelScope.launch {
            repository.checkAndShowWelcomeMessage()
        }
    }

    val messages: StateFlow<List<ChatMessage>> = repository.getMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _typingMessage = MutableStateFlow<String?>(null)
    val typingMessage: StateFlow<String?> = _typingMessage.asStateFlow()

    private val _speechText = MutableStateFlow("")
    val speechText: StateFlow<String> = _speechText.asStateFlow()

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _typingMessage.value = ""

            try {
                repository.sendMessageStream(message, messages.value).collect { accumulatedText ->
                    _typingMessage.value = accumulatedText
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _typingMessage.value = null
                _isLoading.value = false
            }
        }
    }

    /**
     * Gửi tin nhắn kèm ảnh
     */
    fun sendImageMessage(message: String, bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _typingMessage.value = ""

            try {
                val base64 = bitmapToBase64(bitmap)
                repository.sendMessageWithImageStream(message, base64, messages.value).collect { accumulatedText ->
                    _typingMessage.value = accumulatedText
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _typingMessage.value = null
                _isLoading.value = false
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        // Resize ảnh lớn trước khi nén để tránh OOM
        val resized = com.example.easyshop.AppUtil.resizeBitmap(bitmap, 1024)
        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 60, outputStream) // Nén để giảm tải
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Xử lý Voice (STT)
     */
    fun startListening(context: Context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _error.value = "Thiết bị không hỗ trợ nhận diện giọng nói"
            return
        }

        try {
            if (speechRecognizer != null) {
                speechRecognizer?.destroy()
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _error.value = null
                    Log.d("STT", "Sẵn sàng nhận diện")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("STT", "Bắt đầu nói")
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Lỗi âm thanh"
                        SpeechRecognizer.ERROR_CLIENT -> "Lỗi phía client"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Thiếu quyền truy cập"
                        SpeechRecognizer.ERROR_NETWORK -> "Lỗi mạng"
                        SpeechRecognizer.ERROR_NO_MATCH -> "Không nhận diện được giọng nói"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Không nghe thấy tiếng, vui lòng thử lại"
                        else -> "Lỗi nhận diện: $error"
                    }
                    Log.e("STT", message)
                    // Chỉ hiện lỗi nếu không phải là lỗi kết thúc thông thường
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        _error.value = message
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _speechText.value = matches[0]
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _speechText.value = matches[0]
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Nhận kết quả từng phần để hiển thị nhanh hơn
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _error.value = "Không thể khởi động Micro: ${e.message}"
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("STT", "Stop error: ${e.message}")
        }
        _isListening.value = false
    }

    fun clearSpeechText() {
        _speechText.value = ""
    }

    fun clearChat() {
        viewModelScope.launch { repository.clearChat() }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}