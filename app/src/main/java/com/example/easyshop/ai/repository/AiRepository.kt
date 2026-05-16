package com.example.easyshop.ai.repository

import android.util.Log
import com.example.easyshop.BuildConfig
import com.example.easyshop.R
import com.example.easyshop.ai.model.ChatMessage
import com.example.easyshop.model.OrderModel
import com.example.easyshop.model.ProductModel
import com.example.easyshop.model.PromoCodeModel
import com.example.easyshop.model.isExpired
import com.example.easyshop.model.isUsageLimitReached
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
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.Normalizer
import java.util.concurrent.TimeUnit

class AiRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()
) {

    private val gson = Gson()

    // Config Gemini
    private val geminiApiKey = BuildConfig.GEMINI_API_KEY
    private val geminiBaseUrl = BuildConfig.GEMINI_BASE_URL.trim().trim('"').trimEnd('/')
        .let { it.ifBlank { "https://generativelanguage.googleapis.com/v1beta" } }
    private val geminiModel = BuildConfig.GEMINI_MODEL.ifBlank { "gemini-2.0-flash" }

    private val beeknoeeApiKey = BuildConfig.BEEKNOEE_API_KEY
    private val beeknoeeBaseUrl = BuildConfig.BEEKNOEE_BASE_URL.trim().trim('"').trimEnd('/')
    private val beeknoeeModel = BuildConfig.BEEKNOEE_MODEL.ifBlank { "gemini-2.0-flash" }

    // Config Cloudinary
    private val cloudinaryCloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
    private val cloudinaryUploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET

    // Cache cho Sản phẩm
    private var cachedProducts: List<ProductModel> = emptyList()
    private var lastProductsFetchTimeMs: Long = 0L
    private val productsCacheTtlMs = 120_000L // 2 phút

    // Cache cho Mã giảm giá
    private var cachedPromoCodes: List<PromoCodeModel> = emptyList()
    private var lastPromoFetchTimeMs: Long = 0L
    private val promoCacheTtlMs = 120_000L // 2 phút

    private fun getMessagesCollection() = auth.currentUser?.uid?.let { uid ->
        db.collection("ai_chats").document(uid).collection("messages")
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
    suspend fun checkAndShowWelcomeMessage(context: android.content.Context? = null) {
        val collection = getMessagesCollection() ?: return
        val snapshot = collection.limit(1).get().await()
        if (snapshot.isEmpty) {
            val welcomeText = context?.getString(R.string.ai_welcome_msg)
                ?: "Chào bạn! Mình là Trợ lý AI của EasyShop. Bạn cần mình gợi ý sản phẩm hay hỗ trợ đơn hàng không ạ?"
            val msg = ChatMessage(content = welcomeText, isUser = false, timestamp = Timestamp.now())
            collection.add(msg).await()
            auth.currentUser?.uid?.let { uid ->
                db.collection("ai_chats").document(uid).set(mapOf("lastActivity" to FieldValue.serverTimestamp()), SetOptions.merge())
            }
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
        kotlinx.coroutines.delay(300)
        checkAndShowWelcomeMessage()
    }

    /**
     * Gửi tin nhắn và xử lý tư vấn thông minh
     */
    fun sendMessageStream(userMessage: String, history: List<ChatMessage>, context: android.content.Context?): Flow<String> = flow {
        val uid = auth.currentUser?.uid ?: throw Exception(context?.getString(R.string.login_required_msg) ?: "Vui lòng đăng nhập")
        val collection = getMessagesCollection()!!

        // 1. Lưu tin nhắn User
        collection.add(ChatMessage(content = userMessage, isUser = true, timestamp = Timestamp.now())).await()

        // 2. Thu thập dữ liệu ngữ cảnh
        val intent = detectIntent(userMessage)
        val needsProducts = intent !in setOf("promo", "payment")
        val allProducts = fetchAllProducts()
        val relevantProducts = if (needsProducts) {
            selectRelevantProducts(userMessage, history, allProducts).take(12)
        } else emptyList()

        val userContext = fetchUserContext(uid, allProducts.associateBy { it.id }, context)
        val activePromos = fetchActivePromoCodes()

        val systemInstruction = buildComplexInstruction(intent, userContext, relevantProducts, activePromos, context)

        // 3. Gọi AI
        var aiResponse = ""
        val geminiError = runCatching {
            aiResponse = requestGeminiWithFallback(systemInstruction, history, userMessage, context)
        }.exceptionOrNull()

        if (aiResponse.isBlank()) {
            val errMsg = geminiError?.message ?: ""
            val isQuota = errMsg.contains("bận") || errMsg.contains("429") || errMsg.contains("503") ||
                         errMsg.contains("RESOURCE_EXHAUSTED") || errMsg.contains("UNAVAILABLE")

            if (isQuota && beeknoeeApiKey.isNotBlank()) {
                try {
                    aiResponse = requestBeeknoeeReply(systemInstruction, history, userMessage)
                } catch (be: Exception) {
                    throw Exception(context?.getString(R.string.ai_busy_msg) ?: "Cửa hàng đang bận, bạn vui lòng thử lại sau vài giây nhé!")
                }
            } else {
                throw geminiError ?: Exception(context?.getString(R.string.ai_not_connected) ?: "Không thể kết nối AI lúc này.")
            }
        }

        // 4. Lưu phản hồi AI
        if (aiResponse.isNotBlank()) {
            collection.add(ChatMessage(content = aiResponse, isUser = false, timestamp = Timestamp.now())).await()
            db.collection("ai_chats").document(uid).update("lastActivity", FieldValue.serverTimestamp())
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
            val list = docs.mapNotNull { doc ->
                doc.toObject(ProductModel::class.java)?.apply { id = doc.id }
            }
            cachedProducts = list
            lastProductsFetchTimeMs = now
            list
        } catch (e: Exception) {
            Log.e("AI_CHAT", "Fetch products failed", e)
            cachedProducts
        }
    }

    private suspend fun fetchActivePromoCodes(): List<PromoCodeModel> {
        val now = System.currentTimeMillis()
        if (cachedPromoCodes.isNotEmpty() && (now - lastPromoFetchTimeMs) < promoCacheTtlMs) {
            return cachedPromoCodes
        }
        return try {
            val docs = db.collection("promoCodes")
                .whereEqualTo("active", true)
                .whereEqualTo("isIssued", true)
                .get().await()
            val list = docs.mapNotNull { doc ->
                doc.toObject(PromoCodeModel::class.java)?.copy(docId = doc.id)
            }.filter { !it.isExpired() && !it.isUsageLimitReached() }
            cachedPromoCodes = list
            lastPromoFetchTimeMs = now
            list
        } catch (e: Exception) {
            Log.e("AI_CHAT", "Fetch promo codes failed", e)
            cachedPromoCodes
        }
    }

    private val STOP_WORDS = setOf("co", "khong", "shop", "ban", "mat", "hang", "mua", "gia", "tim", "xem", "loai", "nao", "giup", "voi", "cho", "em")

    private fun selectRelevantProducts(query: String, history: List<ChatMessage>, all: List<ProductModel>): List<ProductModel> {
        val normalizedQuery = foldText(query)
        val queryTokens = getSearchTokens(normalizedQuery)
        val historyTokens = history.takeLast(8)
            .filter { it.isUser }
            .takeLast(4)
            .flatMap { getSearchTokens(foldText(it.content)) }
            .distinct()

        val allTokens = queryTokens + historyTokens
        val effectiveQuery = if (queryTokens.size < 2 && historyTokens.isNotEmpty()) {
            foldText(history.takeLast(8).filter { it.isUser }.takeLast(2).joinToString(" ") { it.content })
        } else normalizedQuery

        return all.map { product ->
            product to calculateProductScore(product, effectiveQuery, allTokens, queryTokens)
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private suspend fun fetchUserContext(uid: String, productsById: Map<String, ProductModel>, context: android.content.Context?): String {
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val name = userDoc.getString("name") ?: (context?.getString(R.string.ai_unknown_customer) ?: "Khách hàng")
            @Suppress("UNCHECKED_CAST")
            val cartItems = userDoc.get("cartItems") as? Map<String, Long> ?: emptyMap()
            val cartStr = cartItems.entries.joinToString(", ") { "${productsById[it.key]?.title ?: it.key} x${it.value}" }

            val orders = db.collection("orders").whereEqualTo("userId", uid).limit(5).get().await()
                .documents.mapNotNull { doc -> doc.toObject(OrderModel::class.java)?.copy(id = doc.id) }
            val orderSummary = orders.joinToString("; ") { "${it.id.take(5)}:${it.status}:${it.total}" }

            context?.getString(R.string.ai_user_context_format, name, cartStr, orderSummary)
                ?: "Tên: $name\nGiỏ hàng: $cartStr\nĐơn hàng gần đây: $orderSummary"
        } catch (e: Exception) {
            context?.getString(R.string.ai_new_customer) ?: "Khách hàng mới"
        }
    }

    private fun buildComplexInstruction(intent: String, userContext: String, products: List<ProductModel>, promoCodes: List<PromoCodeModel>, context: android.content.Context?): String {
        val productListInfo = formatProductsForAI(products, context)
        val promoListInfo = formatPromosForAI(promoCodes, context)

        val focusInstruction = when(intent) {
            "promo" -> context?.getString(R.string.ai_prompt_focus_promo) ?: "TRỌNG TÂM: Khách đang hỏi về khuyến mãi."
            "payment" -> context?.getString(R.string.ai_prompt_focus_payment) ?: "TRỌNG TÂM: Khách đang hỏi về thanh toán."
            "comparison" -> context?.getString(R.string.ai_prompt_focus_comparison) ?: "TRỌNG TÂM: So sánh sản phẩm."
            "budget" -> context?.getString(R.string.ai_prompt_focus_budget) ?: "TRỌNG TÂM: Tư vấn giá."
            else -> context?.getString(R.string.ai_prompt_focus_general) ?: "TRỌNG TÂM: Tư vấn mua hàng/hỗ trợ chung."
        }

        return """
            ${context?.getString(R.string.ai_prompt_system_role) ?: "BẠN LÀ CHUYÊN VIÊN TƯ VẤN CỦA EASYSHOP."}
            $focusInstruction
            
            ${context?.getString(R.string.ai_prompt_user_context) ?: "NGỮ CẢNH KHÁCH HÀNG:"}
            $userContext
            
            ${context?.getString(R.string.ai_prompt_available_products) ?: "DANH SÁCH SẢN PHẨM KHẢ DỤNG:"}
            $productListInfo
            
            ${context?.getString(R.string.ai_prompt_available_promos) ?: "MÃ GIẢM GIÁ / VOUCHER HIỆN ĐANG CÓ:"}
            $promoListInfo
            
            ${context?.getString(R.string.ai_prompt_payment_methods) ?: ""}
            
            ${context?.getString(R.string.ai_prompt_order_process) ?: ""}
            
            ${context?.getString(R.string.ai_prompt_intent_prefix, intent) ?: "Ý ĐỊNH NGƯỜI DÙNG: $intent"}
            
            ${getCommonRules(context)}
        """.trimIndent()
    }

    private fun formatProductsForAI(products: List<ProductModel>, context: android.content.Context?): String {
        if (products.isEmpty()) return context?.getString(R.string.ai_no_products_found) ?: "Hiện chưa tìm thấy sản phẩm."
        return products.joinToString("\n") { product ->
            val status = if(product.inStock) (context?.getString(R.string.ai_in_stock) ?: "Còn hàng") else (context?.getString(R.string.ai_out_of_stock) ?: "Hết hàng")
            context?.getString(R.string.ai_product_item_format, product.title, product.id, product.price, status)
                ?: "- ${product.title} [${product.id}] | Giá: ${product.price} | Tình trạng: $status"
        }
    }

    private fun formatPromosForAI(promos: List<PromoCodeModel>, context: android.content.Context?): String {
        if (promos.isEmpty()) return context?.getString(R.string.ai_no_promos_found) ?: "Không có mã giảm giá nào."
        return promos.joinToString("\n") { promo ->
            val discountText = if (promo.type == "percentage") {
                context?.getString(R.string.ai_discount_percent_format, promo.value.toInt(), if (promo.maxDiscount > 0) formatVND(promo.maxDiscount) else "0")
                    ?: "Giảm ${promo.value.toInt()}%"
            } else {
                context?.getString(R.string.ai_discount_fixed_format, formatVND(promo.value)) ?: "Giảm ${formatVND(promo.value)}"
            }
            val minOrderText = if (promo.minOrder > 0) formatVND(promo.minOrder) else "0"
            val expiryText = if (promo.expiryDate > 0) {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                sdf.format(java.util.Date(promo.expiryDate))
            } else "Không giới hạn"
            
            context?.getString(R.string.ai_promo_item_format, promo.code, discountText, minOrderText, expiryText)
                ?: "- Mã: ${promo.code} | $discountText | Đơn từ $minOrderText | HSD: $expiryText"
        }
    }

    private fun formatVND(amount: Double): String {
        val formatter = java.text.DecimalFormat("#,###")
        return "${formatter.format(amount.toLong())}₫"
    }

    private fun getCommonRules(context: android.content.Context?): String {
        return context?.getString(R.string.ai_prompt_common_rules) ?: ""
    }

    private suspend fun requestGeminiWithFallback(sys: String, history: List<ChatMessage>, user: String, context: android.content.Context?): String {
        val models = listOf(geminiModel, "gemini-2.0-flash", "gemini-1.5-flash").distinct()
        var lastErr: Exception? = null
        var allQuotaExhausted = true

        for (m in models) {
            try {
                return callGeminiApi(m, sys, history, user)
            } catch (e: Exception) {
                lastErr = e
                val errMsg = e.message ?: ""
                val isQuotaError = errMsg.contains("429") || errMsg.contains("quota") || errMsg.contains("RESOURCE_EXHAUSTED")
                if (isQuotaError) continue else {
                    allQuotaExhausted = false
                    throw e
                }
            }
        }

        if (allQuotaExhausted) {
            throw Exception(context?.getString(R.string.ai_busy_msg) ?: "Cửa hàng đang bận, bạn vui lòng thử lại sau vài giây nhé!")
        }
        throw lastErr ?: Exception(context?.getString(R.string.ai_not_connected) ?: "Không thể kết nối AI lúc này.")
    }

    fun sendMessageWithImageStream(userMessage: String, base64Image: String, history: List<ChatMessage>, context: android.content.Context?): Flow<String> = flow {
        val uid = auth.currentUser?.uid ?: throw Exception(context?.getString(R.string.login_required_msg) ?: "Vui lòng đăng nhập")
        val collection = getMessagesCollection()!!

        val imageUrl = try { uploadImageToCloudinary(base64Image) } catch (e: Exception) { null }
        collection.add(ChatMessage(content = userMessage, isUser = true, timestamp = Timestamp.now(), imageUrl = imageUrl)).await()

        val allProducts = fetchAllProducts()
        val userContext = fetchUserContext(uid, allProducts.associateBy { it.id }, context)

        val visionInstruction = """
            ${context?.getString(R.string.ai_vision_prompt_role) ?: "BẠN LÀ CHUYÊN VIÊN TƯ VẤN THỊ GIÁC."}
            ${context?.getString(R.string.ai_prompt_user_context) ?: "NGỮ CẢNH:"} $userContext
            ${context?.getString(R.string.ai_prompt_available_products) ?: "KHO HÀNG:"}
            ${formatProductsForAI(allProducts, context)}
            ${getCommonRules(context)}
        """.trimIndent()

        var aiResponse = ""
        val visionError = runCatching {
            aiResponse = requestGeminiVisionWithFallback(visionInstruction, history, userMessage, base64Image, context)
        }.exceptionOrNull()

        if (aiResponse.isBlank()) {
            val errMsg = visionError?.message ?: ""
            val isQuota = errMsg.contains("429") || errMsg.contains("503") || errMsg.contains("RESOURCE_EXHAUSTED")

            if (isQuota && beeknoeeApiKey.isNotBlank()) {
                try {
                    aiResponse = requestBeeknoeeReply(visionInstruction, history, userMessage, base64Image)
                } catch (be: Exception) {
                    aiResponse = context?.getString(R.string.ai_generic_error) ?: "Lỗi kết nối AI."
                }
            } else {
                aiResponse = context?.getString(R.string.ai_generic_error) ?: "Lỗi kết nối AI."
            }
        }

        if (aiResponse.isNotBlank()) {
            collection.add(ChatMessage(content = aiResponse, isUser = false, timestamp = Timestamp.now())).await()
            db.collection("ai_chats").document(uid).update("lastActivity", FieldValue.serverTimestamp())
            emit(aiResponse)
        }
    }

    private suspend fun requestGeminiVisionWithFallback(sys: String, history: List<ChatMessage>, user: String, base64Image: String, context: android.content.Context?): String {
        val models = listOf(geminiModel, "gemini-2.0-flash", "gemini-1.5-flash").distinct()
        var lastErr: Exception? = null
        var allQuotaExhausted = true

        for (m in models) {
            try {
                return callGeminiApi(m, sys, history, user, base64Image)
            } catch (e: Exception) {
                lastErr = e
                val errMsg = e.message ?: ""
                val isQuotaError = errMsg.contains("429") || errMsg.contains("quota") || errMsg.contains("RESOURCE_EXHAUSTED")
                if (isQuotaError) continue else {
                    allQuotaExhausted = false
                    throw e
                }
            }
        }

        if (allQuotaExhausted) {
            throw Exception(context?.getString(R.string.ai_busy_msg) ?: "Cửa hàng đang bận, bạn vui lòng thử lại sau vài giây nhé!")
        }
        throw lastErr ?: Exception(context?.getString(R.string.ai_not_connected) ?: "Không thể kết nối AI lúc này.")
    }

    private suspend fun callGeminiApi(
        model: String,
        sys: String,
        history: List<ChatMessage>,
        user: String,
        base64Image: String? = null
    ): String = withContext(Dispatchers.IO) {
        val contents = history.takeLast(6).map {
            mapOf("role" to if(it.isUser) "user" else "model", "parts" to listOf(mapOf("text" to it.content)))
        }.toMutableList()

        val currentParts = mutableListOf<Map<String, Any>>(mapOf("text" to user))
        base64Image?.let {
            currentParts.add(mapOf("inline_data" to mapOf("mime_type" to "image/jpeg", "data" to it)))
        }

        contents.add(mapOf("role" to "user", "parts" to currentParts))

        val payload = mapOf("systemInstruction" to mapOf("parts" to listOf(mapOf("text" to sys))), "contents" to contents)
        val req = Request.Builder()
            .url("$geminiBaseUrl/models/$model:generateContent?key=$geminiApiKey")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception("Gemini Error: ${resp.code} - $body")
            val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
            json.getAsJsonArray("candidates")?.get(0)?.asJsonObject?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
        }
    }

    private suspend fun requestBeeknoeeReply(
        sys: String,
        history: List<ChatMessage>,
        user: String,
        base64Image: String? = null
    ): String = withContext(Dispatchers.IO) {
        val messages = mutableListOf<Map<String, Any>>()
        if (sys.isNotBlank()) messages.add(mapOf("role" to "system", "content" to sys))

        val chatHistory = mutableListOf<Map<String, Any>>()
        history.takeLast(10).forEach { msg ->
            val role = if (msg.isUser) "user" else "assistant"
            val text = msg.content.trim()
            if (text.isNotEmpty()) {
                if (chatHistory.isNotEmpty() && chatHistory.last()["role"] == role) {
                    val last = chatHistory.last().toMutableMap()
                    last["content"] = "${last["content"]}\n$text"
                    chatHistory[chatHistory.lastIndex] = last
                } else {
                    chatHistory.add(mapOf("role" to role, "content" to text))
                }
            }
        }
        messages.addAll(chatHistory)

        if (base64Image == null) {
            messages.add(mapOf("role" to "user", "content" to user))
        } else {
            val content = mutableListOf<Map<String, Any>>(mapOf("type" to "text", "text" to user))
            val finalBase64 = if (!base64Image.startsWith("data:image")) "data:image/jpeg;base64,$base64Image" else base64Image
            content.add(mapOf("type" to "image_url", "image_url" to mapOf("url" to finalBase64)))
            messages.add(mapOf("role" to "user", "content" to content))
        }

        val payload = mapOf("model" to beeknoeeModel, "messages" to messages, "temperature" to 0.7, "max_tokens" to 1500)
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

    private suspend fun uploadImageToCloudinary(base64Image: String): String? = withContext(Dispatchers.IO) {
        if (cloudinaryCloudName.isBlank()) return@withContext null
        val url = "https://api.cloudinary.com/v1_1/$cloudinaryCloudName/image/upload"
        val finalBase64 = if (!base64Image.startsWith("data:image")) "data:image/jpeg;base64,$base64Image" else base64Image
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", finalBase64)
            .addFormDataPart("upload_preset", cloudinaryUploadPreset)
            .build()
        val request = Request.Builder().url(url).post(requestBody).build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) return@withContext null
                val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                json.get("secure_url")?.asString
            }
        } catch (e: Exception) { null }
    }

    private fun detectIntent(msg: String): String {
        val normalized = foldText(msg)
        return when {
            normalized.contains("ma giam gia") || normalized.contains("voucher") || normalized.contains("khuyen mai") || normalized.contains("giam gia") -> "promo"
            normalized.contains("thanh toan") || normalized.contains("chuyen khoan") || normalized.contains("tra gop") || normalized.contains("cod") -> "payment"
            normalized.contains("so sanh") -> "comparison"
            normalized.contains("gia") || normalized.contains("re") -> "budget"
            else -> "advice"
        }
    }

    private fun foldText(text: String): String = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace("đ", "d")

    private fun getSearchTokens(text: String): List<String> = text.split(Regex("\\s+")).filter { it.isNotBlank() && it !in STOP_WORDS }

    private fun calculateProductScore(product: ProductModel, query: String, allTokens: List<String>, primaryTokens: List<String>): Int {
        var score = 0
        val titleFolded = foldText(product.title)
        val categoryFolded = foldText(product.category)
        if (titleFolded.contains(query)) score += 10
        primaryTokens.forEach { token ->
            if (titleFolded.contains(token)) score += 3
            if (categoryFolded.contains(token)) score += 1
        }
        (allTokens - primaryTokens.toSet()).forEach { token -> if (titleFolded.contains(token)) score += 1 }
        return score
    }
}
