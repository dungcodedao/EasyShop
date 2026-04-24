package com.example.easyshop.ai.repository

import android.util.Log
import com.example.easyshop.BuildConfig
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
    private val geminiModel = BuildConfig.GEMINI_MODEL.ifBlank { "gemini-2.5-flash" }

    private val beeknoeeApiKey = BuildConfig.BEEKNOEE_API_KEY
    private val beeknoeeBaseUrl = BuildConfig.BEEKNOEE_BASE_URL.trim().trim('"').trimEnd('/')
    private val beeknoeeModel = BuildConfig.BEEKNOEE_MODEL.ifBlank { "gemini-2.5-flash-lite" }
    
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

        // 2. Thu thập dữ liệu ngữ cảnh (Products + User Context + Promo Codes)
        val allProducts = fetchAllProducts()
        val relevantProducts = selectRelevantProducts(userMessage, history, allProducts).take(12)
        val userContext = fetchUserContext(uid, allProducts.associateBy { it.id })
        val activePromos = fetchActivePromoCodes()
        
        val intent = detectIntent(userMessage)
        val systemInstruction = buildComplexInstruction(intent, userContext, relevantProducts, activePromos)

        // 3. Gọi AI với cơ chế Beeknoee là Primary
        var aiResponse = ""
        try {
            if (beeknoeeApiKey.isNotBlank()) {
                Log.d("AI_CHAT", "Calling Beeknoee (Primary)...")
                aiResponse = requestBeeknoeeReply(systemInstruction, history, userMessage)
            } else {
                throw Exception("Beeknoee Key missing")
            }
        } catch (e: Exception) {
            Log.w("AI_CHAT", "Beeknoee failed: ${e.message}. Using Gemini Fallback...")
            try {
                aiResponse = requestGeminiWithFallback(systemInstruction, history, userMessage)
            } catch (ge: Exception) {
                throw ge
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

    /**
     * Lấy danh sách mã giảm giá đang hoạt động từ Firestore (có cache)
     */
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
            Log.d("AI_CHAT", "Fetched ${list.size} active promo codes")
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
        
        return all.map { product ->
            product to calculateProductScore(product, normalizedQuery, queryTokens)
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

    private fun buildComplexInstruction(intent: String, userContext: String, products: List<ProductModel>, promoCodes: List<PromoCodeModel> = emptyList()): String {
        val productListInfo = formatProductsForAI(products)
        val promoListInfo = formatPromosForAI(promoCodes)
        return """
            BẠN LÀ CHUYÊN VIÊN TƯ VẤN CỦA EASYSHOP.
            NGỮ CẢNH KHÁCH HÀNG:
            $userContext
            
            DANH SÁCH SẢN PHẨM KHẢ DỤNG:
            $productListInfo
            
            MÃ GIẢM GIÁ / VOUCHER HIỆN ĐANG CÓ:
            $promoListInfo
            
            Ý ĐỊNH NGƯỜI DÙNG: $intent
            
            ${getCommonRules()}
        """.trimIndent()
    }

    private fun formatProductsForAI(products: List<ProductModel>): String {
        if (products.isEmpty()) return "Hiện chưa tìm thấy sản phẩm chính xác trong kho."
        return products.joinToString("\n") { product ->
            "- ${product.title} [${product.id}] | Giá: ${product.price} | Tình trạng: ${if(product.inStock) "Còn hàng" else "Hết hàng"}"
        }
    }

    private fun formatPromosForAI(promos: List<PromoCodeModel>): String {
        if (promos.isEmpty()) return "Hiện tại shop chưa phát hành mã giảm giá nào."
        return promos.joinToString("\n") { promo ->
            val discountText = if (promo.type == "percentage") {
                "Giảm ${promo.value.toInt()}%" + if (promo.maxDiscount > 0) " (tối đa ${formatVND(promo.maxDiscount)})" else ""
            } else {
                "Giảm ${formatVND(promo.value)}"
            }
            val minOrderText = if (promo.minOrder > 0) "đơn tối thiểu ${formatVND(promo.minOrder)}" else "không yêu cầu đơn tối thiểu"
            val expiryText = if (promo.expiryDate > 0) {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                "HSD: ${sdf.format(java.util.Date(promo.expiryDate))}"
            } else "Không giới hạn thời gian"
            val usageText = if (promo.usageLimit > 0) "Còn ${promo.usageLimit - promo.usedCount} lượt" else "Không giới hạn lượt"
            "- Mã: ${promo.code} | $discountText | $minOrderText | $expiryText | $usageText | Mô tả: ${promo.description}"
        }
    }

    private fun formatVND(amount: Double): String {
        val formatter = java.text.DecimalFormat("#,###")
        return "${formatter.format(amount.toLong())}₫"
    }

    private fun getCommonRules(): String {
        return """
            QUY TẮC PHẢN HỒI BẮT BUỘC:
            1. KHÔNG HIỂN THỊ MÃ ID: Tuyệt đối không viết các mã lộn xộn (như 3qifh...) ra nội dung chat.
            2. GẮN THẺ SẢN PHẨM: Khi nhắc đến sản phẩm, phải gắn thẻ [PID_id] ngay sau tên hoặc cuối câu. 
               Thẻ này dùng để hiển thị nút bấm ngầm, người dùng sẽ không thấy thẻ này.
               Ví dụ: "Bạn có thể tham khảo mẫu Laptop Gaming [PID_abc123] này ạ!"
            3. TRƯỜNG HỢP HẾT HÀNG: Nếu sản phẩm khách hỏi đã hết hàng, hãy thông báo lịch sự và chủ động gợi ý sản phẩm thay thế có gắn [PID_...].
            4. MÃ GIẢM GIÁ / VOUCHER: 
               - Khi khách hỏi về mã giảm giá, voucher, khuyến mãi, ưu đãi → PHẢI trả lời dựa trên danh sách "MÃ GIẢM GIÁ HIỆN ĐANG CÓ" phía trên.
               - Liệt kê RÕ RÀNG mã code, mức giảm, điều kiện áp dụng.
               - Nếu không có mã nào → thông báo "Shop hiện chưa có mã giảm giá nào" và gợi ý khách theo dõi thông báo.
               - Hướng dẫn khách nhập mã ở trang Thanh toán (Checkout).
               - KHÔNG bịa ra mã giảm giá không có trong danh sách.
            5. PHONG CÁCH: Thân thiện, chuyên nghiệp (Xưng hô: Shop - Bạn).
            6. NGÔN NGỮ: Tiếng Việt.
        """.trimIndent()
    }

    private suspend fun requestGeminiWithFallback(sys: String, history: List<ChatMessage>, user: String): String {
        val models = listOf(geminiModel, "gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash").distinct()
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

    /**
     * Gửi tin nhắn kèm hình ảnh (Vision)
     */
    fun sendMessageWithImageStream(userMessage: String, base64Image: String, history: List<ChatMessage>): Flow<String> = flow {
        val uid = auth.currentUser?.uid ?: throw Exception("Vui lòng đăng nhập")
        val collection = getMessagesCollection()!!

        // 1. Tải ảnh lên Cloudinary trước để có URL
        val imageUrl = try {
            uploadImageToCloudinary(base64Image)
        } catch (e: Exception) {
            Log.e("AI_CHAT", "Upload image to Cloudinary failed", e)
            null
        }

        // 2. Lưu tin nhắn User kèm URL ảnh
        collection.add(ChatMessage(content = userMessage, isUser = true, timestamp = Timestamp.now(), imageUrl = imageUrl)).await()

        // 3. Thu thập dữ liệu ngữ cảnh
        val allProducts = fetchAllProducts()
        val userContext = fetchUserContext(uid, allProducts.associateBy { it.id })
        
        // Hướng dẫn đặc biệt cho Vision
        val visionSystemInstruction = """
            BẠN LÀ CHUYÊN VIÊN TƯ VẤN THỊ GIÁC CỦA EASYSHOP.
            NGỮ CẢNH: $userContext
            
            KHO HÀNG SHOP ĐANG CÓ:
            ${formatProductsForAI(allProducts)}
            
            NHIỆM VỤ CỦA BẠN:
            1. PHÂN TÍCH ẢNH: Xác định sản phẩm trong ảnh là gì.
            2. TRA CỨU KHO: So khớp với DANH SÁCH SẢN PHẨM KHẢ DỤNG bên trên.
            3. TƯ VẤN: 
               - NẾU KHỚP: Tư vấn giá và cấu hình, bắt buộc kèm [PID_id] ngầm.
               - NẾU KHÔNG KHỚP: Thông báo chưa có mẫu này, gợi ý sản phẩm TƯƠNG TỰ nhất kèm [PID_id] ngầm.
            
            ${getCommonRules()}
        """.trimIndent()

        // 4. Gọi AI Vision (Beeknoee Primary)
        var aiResponse = ""
        try {
            if (beeknoeeApiKey.isNotBlank()) {
                Log.d("AI_CHAT", "Calling Beeknoee Vision (Primary)...")
                aiResponse = requestBeeknoeeReply(visionSystemInstruction, history, userMessage, base64Image)
            } else {
                throw Exception("Beeknoee Key missing")
            }
        } catch (e: Exception) {
            Log.w("AI_CHAT", "Beeknoee Vision failed: ${e.message}. Using Gemini Vision Fallback...")
            try {
                aiResponse = requestGeminiVisionWithFallback(visionSystemInstruction, history, userMessage, base64Image)
            } catch (ge: Exception) {
                Log.e("AI_CHAT", "All AI providers failed", ge)
                aiResponse = "Xin lỗi, mình gặp trục trặc khi kết nối với máy chủ AI. Bạn vui lòng thử lại sau nhé!"
            }
        }

        // 4. Lưu phản hồi AI
        if (aiResponse.isNotBlank()) {
            collection.add(ChatMessage(content = aiResponse, isUser = false, timestamp = Timestamp.now())).await()
            db.collection("chats").document(uid).update("lastActivity", FieldValue.serverTimestamp())
            emit(aiResponse)
        }
    }

    private suspend fun requestGeminiVisionWithFallback(sys: String, history: List<ChatMessage>, user: String, base64Image: String): String {
        val models = listOf(geminiModel, "gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash").distinct()
        var lastErr: Exception? = null
        for (m in models) {
            try {
                Log.d("AI_CHAT", "Trying Gemini Vision: $m")
                return callGeminiApi(m, sys, history, user, base64Image)
            } catch (e: Exception) {
                lastErr = e
                Log.w("AI_CHAT", "Gemini Vision $m failed: ${e.message}")
                if (e.message?.contains("quota") == true || e.message?.contains("429") == true) continue
                if (m == models.last()) throw e
            }
        }
        throw lastErr ?: Exception("Gemini Vision Error")
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

        // Tạo phần "parts" cho tin nhắn hiện tại
        val currentParts = mutableListOf<Map<String, Any>>(
            mapOf("text" to user)
        )
        
        // Nếu có ảnh, thêm vào parts dưới dạng inline_data
        base64Image?.let {
            currentParts.add(
                mapOf(
                    "inline_data" to mapOf(
                        "mime_type" to "image/jpeg",
                        "data" to it
                    )
                )
            )
        }

        contents.add(mapOf("role" to "user", "parts" to currentParts))

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

    private suspend fun requestBeeknoeeReply(
        sys: String, 
        history: List<ChatMessage>, 
        user: String,
        base64Image: String? = null
    ): String = withContext(Dispatchers.IO) {
        val messages = mutableListOf<Map<String, Any>>()
        
        // 1. System Instruction
        if (sys.isNotBlank()) {
            messages.add(mapOf("role" to "system", "content" to sys))
        }

        // 2. Chuyển đổi lịch sử & Lọc tin nhắn trống (Tránh lỗi 400)
        val chatHistory = history.takeLast(10).mapNotNull { msg ->
            val role = if (msg.isUser) "user" else "assistant"
            val text = msg.content.trim()
            if (text.isNotEmpty()) {
                mapOf("role" to role, "content" to text)
            } else null
        }
        messages.addAll(chatHistory)

        // 3. Tin nhắn hiện tại (Hỗ trợ Vision)
        if (base64Image == null) {
            // Text only
            messages.add(mapOf("role" to "user", "content" to user))
        } else {
            // Multi-modal (OpenAI Format)
            val content = mutableListOf<Map<String, Any>>(
                mapOf("type" to "text", "text" to user)
            )
            val finalBase64 = if (!base64Image.startsWith("data:image")) {
                "data:image/jpeg;base64,$base64Image"
            } else base64Image
            
            content.add(mapOf(
                "type" to "image_url",
                "image_url" to mapOf("url" to finalBase64)
            ))
            messages.add(mapOf("role" to "user", "content" to content))
        }

        val payload = mapOf(
            "model" to beeknoeeModel,
            "messages" to messages,
            "temperature" to 0.7,
            "max_tokens" to 1500
        )

        val req = Request.Builder()
            .url("$beeknoeeBaseUrl/chat/completions")
            .header("Authorization", "Bearer $beeknoeeApiKey")
            .header("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()

        try {
            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    Log.e("AI_CHAT", "Beeknoee API Error: ${resp.code} - $body")
                    throw Exception("Beeknoee Error: ${resp.code}")
                }
                val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                json.getAsJsonArray("choices")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")?.asString ?: "Lỗi phản hồi từ Bee."
            }
        } catch (e: Exception) {
            Log.e("AI_CHAT", "Beeknoee Request Exception", e)
            throw e
        }
    }

    private suspend fun uploadImageToCloudinary(base64Image: String): String? = withContext(Dispatchers.IO) {
        if (cloudinaryCloudName.isBlank()) return@withContext null
        
        val url = "https://api.cloudinary.com/v1_1/$cloudinaryCloudName/image/upload"
        
        // Cloudinary có thể nhận base64 trực tiếp kèm prefix
        val finalBase64 = if (!base64Image.startsWith("data:image")) {
            "data:image/jpeg;base64,$base64Image"
        } else base64Image

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", finalBase64)
            .addFormDataPart("upload_preset", cloudinaryUploadPreset)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("AI_CHAT", "Cloudinary Error: ${response.code} - $body")
                    return@withContext null
                }
                val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                val secureUrl = json.get("secure_url")?.asString
                Log.d("AI_CHAT", "Uploaded to Cloudinary: $secureUrl")
                secureUrl
            }
        } catch (e: Exception) {
            Log.e("AI_CHAT", "Cloudinary upload Exception: ${e.message}")
            null
        }
    }

    private fun detectIntent(msg: String): String {
        val normalized = foldText(msg)
        return when {
            normalized.contains("ma giam gia") || normalized.contains("voucher") ||
            normalized.contains("khuyen mai") || normalized.contains("uu dai") ||
            normalized.contains("giam gia") || normalized.contains("coupon") ||
            normalized.contains("promo") -> "promo"
            normalized.contains("so sanh") -> "comparison"
            normalized.contains("gia") || normalized.contains("re") -> "budget"
            else -> "advice"
        }
    }

    private fun foldText(text: String): String = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private fun getSearchTokens(text: String): List<String> {
        return text.split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in STOP_WORDS }
    }

    private fun calculateProductScore(product: ProductModel, query: String, queryTokens: List<String>): Int {
        var score = 0
        val titleFolded = foldText(product.title)
        
        // Match nguyên câu (ưu tiên cao)
        if (titleFolded.contains(query)) score += 10
        
        // Match từng từ
        queryTokens.forEach { token ->
            if (titleFolded.contains(token)) score += 2
        }
        
        return score
    }
}
