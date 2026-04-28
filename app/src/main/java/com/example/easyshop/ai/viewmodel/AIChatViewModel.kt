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

    // Chỉ dùng để preview text đang nói trên UI, không append vào inputText
    private val _partialSpeechText = MutableStateFlow("")
    val partialSpeechText: StateFlow<String> = _partialSpeechText.asStateFlow()

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
                _error.value = toFriendlyError(e)
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
                _error.value = toFriendlyError(e)
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
     *
     * Fix:
     * 1. _isListening = true ngay khi bấm, không chờ onReadyForSpeech → UI phản hồi tức thì
     * 2. onPartialResults chỉ cập nhật _partialSpeechText riêng để preview trên UI
     *    onResults mới cập nhật _speechText → tránh duplicate text trong inputText
     * 3. onEndOfSpeech KHÔNG tắt isListening — chờ onResults/onError mới tắt
     *    → Tránh trường hợp icon mic tắt trước khi text xuất hiện
     * 4. SpeechRecognizer.createSpeechRecognizer phải gọi trên Main thread
     */
    fun startListening(context: Context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _error.value = "Thiết bị không hỗ trợ nhận diện giọng nói"
            return
        }

        // Set ngay lập tức để UI phản hồi tức thì khi bấm mic
        _isListening.value = true
        _error.value = null
        _partialSpeechText.value = ""

        try {
            speechRecognizer?.destroy()
            // SpeechRecognizer PHẢI tạo trên Main thread
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("STT", "Sẵn sàng nhận diện")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("STT", "Bắt đầu nói")
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    // KHÔNG tắt isListening ở đây — chờ onResults/onError
                    // để tránh icon mic tắt trước khi text xuất hiện
                    Log.d("STT", "Kết thúc giọng nói, đang xử lý...")
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _partialSpeechText.value = ""
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Lỗi âm thanh"
                        SpeechRecognizer.ERROR_CLIENT -> "Lỗi phía client"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Thiếu quyền micro"
                        SpeechRecognizer.ERROR_NETWORK -> "Lỗi mạng khi nhận diện giọng nói"
                        SpeechRecognizer.ERROR_NO_MATCH -> null // Im lặng — không hiện lỗi
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> null // Im lặng — user chưa nói
                        else -> "Lỗi nhận diện: $error"
                    }
                    if (message != null) {
                        Log.e("STT", message)
                        _error.value = message
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _partialSpeechText.value = ""
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        // Chỉ onResults mới đưa vào _speechText để append vào inputText
                        // tránh duplicate từ partial results
                        _speechText.value = matches[0]
                        Log.d("STT", "Kết quả cuối: ${matches[0]}")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // Chỉ dùng để preview trên placeholder TextField, KHÔNG append vào inputText
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _partialSpeechText.value = matches[0]
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
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
        _partialSpeechText.value = ""
    }

    fun clearSpeechText() {
        _speechText.value = ""
    }

    fun clearPartialSpeechText() {
        _partialSpeechText.value = ""
    }

    fun clearChat() {
        viewModelScope.launch { repository.clearChat() }
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Chuyển exception kỹ thuật thành thông báo thân thiện cho user.
     * Tránh hiện JSON thô hoặc stack trace ra UI.
     */
    private fun toFriendlyError(e: Exception): String {
        val msg = e.message ?: return "Đã có lỗi xảy ra, vui lòng thử lại."
        return when {
            // Quota / rate limit — đã được xử lý thành câu tiếng Việt trong Repository
            msg.contains("bận") || msg.contains("thử lại") -> msg
            msg.contains("429") || msg.contains("quota") || msg.contains("RESOURCE_EXHAUSTED") ->
                "Shop đang bận, bạn vui lòng thử lại sau vài giây nhé! 🙏"
            msg.contains("401") || msg.contains("403") || msg.contains("API key") ->
                "Lỗi xác thực API, vui lòng liên hệ shop để được hỗ trợ."
            msg.contains("timeout") || msg.contains("SocketTimeout") || msg.contains("connect") ->
                "Kết nối mạng không ổn định, bạn kiểm tra lại Wi-Fi/4G rồi thử lại nhé."
            msg.contains("đăng nhập") -> msg
            else -> "Trợ lý AI đang gặp sự cố nhỏ, bạn thử lại sau giây lát nhé!"
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}