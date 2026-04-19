package com.example.easyshop.ai.repository

import android.util.Log
import com.example.easyshop.BuildConfig
import com.example.easyshop.ai.model.ChatMessage
import com.example.easyshop.model.OrderModel
import com.example.easyshop.model.ProductModel
import com.example.easyshop.model.UserModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.Normalizer
import java.util.concurrent.TimeUnit

class AiRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    // Config Gemini
    private val geminiApiKey = BuildConfig.GEMINI_API_KEY
    private val geminiBaseUrl = BuildConfig.GEMINI_BASE_URL.trim().trim('"').trimEnd('/')
        .let { it.ifBlank { "https://generativelanguage.googleapis.com/v1beta" } }
    private val geminiModel = BuildConfig.GEMINI_MODEL.ifBlank { "gemini-3.1-flash-latest" }

    // Config Beeknoee
    private val beeknoeeApiKey = BuildConfig.BEEKNOEE_API_KEY
    private val beeknoeeBaseUrl = BuildConfig.BEEKNOEE_BASE_URL.trim().trim('"').trimEnd('/')
    private val beeknoeeModel = BuildConfig.BEEKNOEE_MODEL.ifBlank { "deepseek-chat" }

    // Cache cho Sản phẩm
    private var cachedProducts: List<ProductModel> = emptyList()
    private var lastProductsFetchTimeMs: Long = 0L
    private val productsCacheTtlMs = 120_000L // 2 phút

    private fun getMessagesCollection() = auth.currentUser?.uid?.let { uid ->
        db.collection("chats").document(uid).collection("messages")
    }

    /**
     * Lấy tin nhắn thời gian thực
     */
    fun getMessages(): Flow<List<ChatMessage>> = callbackFlow {
        val collection = getMessagesCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = collection.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.apply { id = doc.id }
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Kiểm tra lời chào
     */
    suspend fun checkAndShowWelcomeMessage() {
        val collection = getMessagesCollection() ?: return
        val snapshot = collection.limit(1).get().await()
        if (snapshot.isEmpty) {
            val welcomeText = "Chào bạn! Mình là Trợ lý AI của EasyShop. Bạn cần mình gợi ý sản phẩm hay hỗ trợ đơn hàng không ạ?"
            val msg = ChatMessage(content = welcomeText, isUser = false, timestamp = Timestamp.now())
            collection.add(msg).await()
            db.collection("chats").document(auth.currentUser!!.uid).set(mapOf("lastActivity" to FieldValue.serverTimestamp()), SetOptions.merge())
        }
    }

    /**
     * Xóa chat
     */
    suspend fun clearChat() {
        val collection = getMessagesCollection() ?: return
        val snapshot = collection.get().await()
        db.runBatch { batch ->
            snapshot.documents.forEach { batch.delete(it.reference) }
        }.await()
        checkAndShowWelcomeMessage()
    }

    /**
     * Gửi tin nhắn và xử lý tư vấn thông minh
     */
    fun sendMessageStream(userMessage: String, history: List<ChatMessage>): Flow<String> = flow {
        val uid = auth.currentUser?.uid ?: throw Exception("Vui lòng đăng nhập")
        val collection = getMessagesCollection()!!

        // 1. Lưu tin nhắn User
        collection.add(ChatMessage(content = userMessage, isUser = true, timestamp = Timestamp.now())).await()

        // 2. Thu thập dữ liệu ngữ cảnh (Products + User Context)
        val allProducts = fetchAllProducts()
        val relevantProducts = selectRelevantProducts(userMessage, history, allProducts).take(12)
        val userContext = fetchUserContext(uid, allProducts.associateBy { it.id })
        
        val intent = detectIntent(userMessage)
        val systemInstruction = buildComplexInstruction(intent, userContext, relevantProducts)

        // 3. Gọi AI với cơ chế Hybrid Fallback
        var aiResponse = ""
        try {
            aiResponse = requestGeminiWithFallback(systemInstruction, history, userMessage)
        } catch (e: Exception) {
            if (beeknoeeApiKey.isNotBlank()) {
                Log.w("AI_CHAT", "Gemini failed: ${e.message}. Using Beeknoee Fallback...")
                aiResponse = requestBeeknoeeReply(systemInstruction, history, userMessage)
            } else {
                throw e
            }
        }

        // 4. Lưu phản hồi AI
        if (aiResponse.isNotBlank()) {
            collection.add(ChatMessage(content = aiResponse, isUser = false, timestamp = Timestamp.now())).await()
            db.collection("chats").document(uid).update("lastActivity", FieldValue.serverTimestamp())
            emit(aiResponse)
        }
    }

    private suspend fun fetchAllProducts(): List<ProductModel> {
        val now = System.currentTimeMillis()
        if (cachedProducts.isNotEmpty() && (now - lastProductsFetchTimeMs) < productsCacheTtlMs) {
            return cachedProducts
        }
        return try {
            val docs = db.collection("data").document("stock").collection("products").get().await()
            val list = docs.mapNotNull { it.toObject(ProductModel::class.java)?.apply { id = it.id } }
            cachedProducts = list
            lastProductsFetchTimeMs = now
            list
        } catch (e: Exception) {
            Log.e("AI_CHAT", "Fetch products failed", e)
            cachedProducts
        }
    }

    private fun selectRelevantProducts(query: String, history: List<ChatMessage>, all: List<ProductModel>): List<ProductModel> {
        val q = foldText(query + " " + history.takeLast(2).joinToString(" ") { it.content })
        val tokens = q.split(" ").filter { it.length > 2 }
        
        return all.map { p ->
            var score = 0
            val pText = foldText("${p.title} ${p.category} ${p.description}")
            if (pText.contains(foldText(query))) score += 15
            tokens.forEach { if (pText.contains(it)) score += 5 }
            if (p.inStock) score += 2
            p to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private suspend fun fetchUserContext(uid: String, productsById: Map<String, ProductModel>): String {
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val name = userDoc.getString("name") ?: "Khách hàng"
            val cartItems = userDoc.get("cartItems") as? Map<String, Long> ?: emptyMap()
            val cartStr = cartItems.entries.joinToString(", ") { "${productsById[it.key]?.title ?: it.key} x${it.value}" }
            
            val orders = db.collection("orders").whereEqualTo("userId", uid).limit(5).get().await()
                .documents.mapNotNull { doc -> doc.toObject(OrderModel::class.java) }
            val orderSummary = orders.joinToString("; ") { "${it.id.take(5)}:${it.status}:${it.total}" }

            "Tên: $name\nGiỏ hàng: $cartStr\nĐơn hàng gần đây: $orderSummary"
        } catch (e: Exception) { "Khách hàng mới" }
    }

    private fun buildComplexInstruction(intent: String, userContext: String, products: List<ProductModel>): String {
        val productStr = products.joinToString("\n") { "- ${it.title} [${it.id}] | Giá: ${it.price} | Kho: ${if(it.inStock) "Còn" else "Hết"}" }
        return """
            BẠN LÀ CHUYÊN VIÊN TƯ VẤN CỦA EASYSHOP.
            NGỮ CẢNH KHÁCH HÀNG:
            $userContext
            
            KHO HÀNG KHẢ DỤNG:
            $productStr
            
            Ý ĐỊNH NGƯỜI DÙNG: $intent
            
            QUY TẮC:
            1. Ưu tiên sản phẩm có trong kho hàng bên trên.
            2. Xưng hô Thân thiện (Shop - Bạn).
            3. Trả lời tư vấn chuyên nghiệp, ngắn gọn.
            4. Trả lời bằng Tiếng Việt.
        """.trimIndent()
    }

    private suspend fun requestGeminiWithFallback(sys: String, history: List<ChatMessage>, user: String): String {
        val models = listOf(geminiModel, "gemini-3.1-flash-latest", "gemini-2.5-flash", "gemini-1.5-flash").distinct()
        var lastErr: Exception? = null
        for (m in models) {
            try {
                Log.d("AI_CHAT", "Trying Gemini: $m")
                return callGeminiApi(m, sys, history, user)
            } catch (e: Exception) {
                lastErr = e
                Log.w("AI_CHAT", "Gemini $m failed: ${e.message}")
                if (e.message?.contains("quota") == true || e.message?.contains("429") == true) continue
                if (m == models.last()) throw e
            }
        }
        throw lastErr ?: Exception("Gemini Error")
    }

    private suspend fun callGeminiApi(model: String, sys: String, history: List<ChatMessage>, user: String): String = withContext(Dispatchers.IO) {
        val contents = history.takeLast(10).map { 
            mapOf("role" to if(it.isUser) "user" else "model", "parts" to listOf(mapOf("text" to it.content)))
        }.toMutableList()
        contents.add(mapOf("role" to "user", "parts" to listOf(mapOf("text" to user))))

        val payload = mapOf("systemInstruction" to mapOf("parts" to listOf(mapOf("text" to sys))), "contents" to contents)
        val req = Request.Builder()
            .url("$geminiBaseUrl/models/$model:generateContent?key=$geminiApiKey")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception("Gemini $model Error: ${resp.code} - $body")
            val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
            json.getAsJsonArray("candidates")?.get(0)?.asJsonObject?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
        }
    }

    private suspend fun requestBeeknoeeReply(sys: String, history: List<ChatMessage>, user: String): String = withContext(Dispatchers.IO) {
        val messages = mutableListOf(mapOf("role" to "system", "content" to sys))
        messages.addAll(history.takeLast(10).map { mapOf("role" to if(it.isUser) "user" else "assistant", "content" to it.content) })
        messages.add(mapOf("role" to "user", "content" to user))

        val payload = mapOf("model" to beeknoeeModel, "messages" to messages)
        val req = Request.Builder()
            .url("$beeknoeeBaseUrl/chat/completions")
            .header("Authorization", "Bearer $beeknoeeApiKey")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception("Beeknoee Error: ${resp.code}")
            val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
            json.getAsJsonArray("choices")?.get(0)?.asJsonObject?.getAsJsonObject("message")?.get("content")?.asString ?: ""
        }
    }

    private fun detectIntent(msg: String): String = when {
        msg.contains("so sanh", true) -> "comparison"
        msg.contains("gia", true) || msg.contains("re", true) -> "budget"
        else -> "advice"
    }

    private fun foldText(text: String): String = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
}
