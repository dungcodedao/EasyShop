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
            db.collection("ai_chats").document(auth.currentUser!!.uid).set(mapOf("lastActivity" to FieldValue.serverTimestamp()), SetOptions.merge())
        }
    }

    /**
     * Xóa chat — dùng flag để tránh race condition với Firestore snapshot listener.
     * Đợi batch delete hoàn tất 300ms trước khi thêm welcome message mới.
     */
    suspend fun clearChat() {
        val collection = getMessagesCollection() ?: return
        val snapshot = collection.get().await()
        db.runBatch { batch ->
            snapshot.documents.forEach { batch.delete(it.reference) }
        }.await()
        // Delay nhỏ để Firestore local cache kịp propagate xóa
        // trước khi checkAndShowWelcomeMessage thêm tin nhắn mới
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

        // 2. Thu thập dữ liệu ngữ cảnh (Products + User Context + Promo Codes)
        val intent = detectIntent(userMessage)

        // Với intent promo/payment không cần danh sách sản phẩm → tiết kiệm ~1000 token/request
        val needsProducts = intent !in setOf("promo", "payment")
        val allProducts = fetchAllProducts()
        val relevantProducts = if (needsProducts) {
            selectRelevantProducts(userMessage, history, allProducts).take(12)
        } else emptyList()

        val userContext = fetchUserContext(uid, allProducts.associateBy { it.id }, context)
        val activePromos = fetchActivePromoCodes()

        val systemInstruction = buildComplexInstruction(intent, userContext, relevantProducts, activePromos, context)

        // 3. Gọi AI — Gemini trước (miễn phí), Beeknoee là fallback (tiết kiệm credit)
        // Nếu Gemini hết quota/503 → tự động chuyển sang Beeknoee
        var aiResponse = ""
        val geminiQuotaError = runCatching {
            aiResponse = requestGeminiWithFallback(systemInstruction, history, userMessage, context)
        }.exceptionOrNull()

        if (aiResponse.isBlank()) {
            val isQuota = geminiQuotaError?.message?.contains("bận") == true
                    || geminiQuotaError?.message?.contains("429") == true
                    || geminiQuotaError?.message?.contains("503") == true
                    || geminiQuotaError?.message?.contains("RESOURCE_EXHAUSTED") == true
                    || geminiQuotaError?.message?.contains("UNAVAILABLE") == true

            if (isQuota && beeknoeeApiKey.isNotBlank()) {
                Log.w("AI_CHAT", "Gemini quota/unavailable → switching to Beeknoee fallback...")
                try {
                    aiResponse = requestBeeknoeeReply(systemInstruction, history, userMessage)
                } catch (be: Exception) {
                    Log.e("AI_CHAT", "Beeknoee also failed: ${be.message}")
                    throw Exception(context?.getString(R.string.ai_busy_msg) ?: "Cửa hàng đang bận, bạn vui lòng thử lại sau vài giây nhé!")
                }
            } else {
                throw geminiQuotaError ?: Exception(context?.getString(R.string.ai_not_connected) ?: "Không thể kết nối AI lúc này.")
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

        // Mở rộng token từ lịch sử hội thoại gần nhất (4 tin nhắn User)
        // Giúp xử lý các câu hỏi tham chiếu như "cái đó giá bao nhiêu?" hay "còn hàng không?"
        val historyTokens = history.takeLast(8)
            .filter { it.isUser }
            .takeLast(4)
            .flatMap { getSearchTokens(foldText(it.content)) }
            .distinct()

        // Query hiện tại có độ ưu tiên cao hơn (weight x2), history bổ sung thêm
        val allTokens = queryTokens + historyTokens

        // Nếu query hiện tại quá ngắn/mơ hồ (< 2 token có nghĩa), dùng thêm history để bù
        val effectiveQuery = if (queryTokens.size < 2 && historyTokens.isNotEmpty()) {
            foldText(history.takeLast(8).filter { it.isUser }.takeLast(2)
                .joinToString(" ") { it.content })
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
                .documents.mapNotNull { doc -> doc.toObject(OrderModel::class.java) }
            val orderSummary = orders.joinToString("; ") { "${it.id.take(5)}:${it.status}:${it.total}" }

            context?.getString(R.string.ai_user_context_format, name, cartStr, orderSummary)
                ?: "Tên: $name\nGiỏ hàng: $cartStr\nĐơn hàng gần đây: $orderSummary"
        } catch (e: Exception) { context?.getString(R.string.ai_new_customer) ?: "Khách hàng mới" }
    }

    private fun buildComplexInstruction(intent: String, userContext: String, products: List<ProductModel>, promoCodes: List<PromoCodeModel> = emptyList(), context: android.content.Context?): String {
        val productListInfo = formatProductsForAI(products, context)
        val promoListInfo = formatPromosForAI(promoCodes, context)

        val focusInstruction = when(intent) {
            "promo" -> context?.getString(R.string.ai_prompt_focus_promo) ?: "TRỌNG TÂM: Khách đang hỏi về khuyến mãi. Hãy liệt kê mã giảm giá rõ ràng, KHÔNG tư vấn sản phẩm trừ khi khách hỏi kèm."
            "payment" -> context?.getString(R.string.ai_prompt_focus_payment) ?: "TRỌNG TÂM: Khách đang hỏi về thanh toán. Giải đáp các phương thức thanh toán, KHÔNG lan man sang khuyến mãi hay sản phẩm."
            "comparison" -> context?.getString(R.string.ai_prompt_focus_comparison) ?: "TRỌNG TÂM: So sánh sản phẩm. Phân tích ưu nhược điểm các sản phẩm khách quan tâm."
            "budget" -> context?.getString(R.string.ai_prompt_focus_budget) ?: "TRỌNG TÂM: Tư vấn giá. Tìm sản phẩm phù hợp ngân sách của khách."
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
            
            ${context?.getString(R.string.ai_prompt_payment_methods) ?: "PHƯƠNG THỨC THANH TOÁN SHOP HỖ TRỢ..."}
            
            ${context?.getString(R.string.ai_prompt_order_process) ?: "QUY TRÌNH ĐẶT HÀNG..."}
            
            ${context?.getString(R.string.ai_prompt_intent_prefix, intent) ?: "Ý ĐỊNH NGƯỜI DÙNG: $intent"}
            
            ${getCommonRules(context)}
        """.trimIndent()
    }

    private fun formatProductsForAI(products: List<ProductModel>, context: android.content.Context?): String {
        if (products.isEmpty()) return context?.getString(R.string.ai_no_products_found) ?: "Hiện chưa tìm thấy sản phẩm chính xác trong kho."
        return products.joinToString("\n") { product ->
            val status = if(product.inStock) (context?.getString(R.string.ai_in_stock) ?: "Còn hàng") else (context?.getString(R.string.ai_out_of_stock) ?: "Hết hàng")
            context?.getString(R.string.ai_product_item_format, product.title, product.id, product.price.toString(), status)
                ?: "- ${product.title} [${product.id}] | Giá: ${product.price} | Tình trạng: $status"
        }
    }

    private fun formatPromosForAI(promos: List<PromoCodeModel>, context: android.content.Context?): String {
        if (promos.isEmpty()) return context?.getString(R.string.ai_no_promos_found) ?: "Hiện tại cửa hàng chưa phát hành mã giảm giá nào."
        return promos.joinToString("\n") { promo ->
            val discountText = if (promo.type == "percentage") {
                context?.getString(R.string.ai_discount_percent_format, promo.value.toInt(), if (promo.maxDiscount > 0) formatVND(promo.maxDiscount) else "0")
                    ?: ("Giảm ${promo.value.toInt()}%" + if (promo.maxDiscount > 0) " (tối đa ${formatVND(promo.maxDiscount)})" else "")
            } else {
                context?.getString(R.string.ai_discount_fixed_format, formatVND(promo.value))
                    ?: "Giảm ${formatVND(promo.value)}"
            }
            val minOrderText = if (promo.minOrder > 0) {
                context?.getString(R.string.ai_min_order_format, formatVND(promo.minOrder)) ?: "đơn tối thiểu ${formatVND(promo.minOrder)}"
            } else context?.getString(R.string.ai_no_min_order) ?: "không yêu cầu đơn tối thiểu"
            
            val expiryText = if (promo.expiryDate > 0) {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                context?.getString(R.string.ai_expiry_date_format, sdf.format(java.util.Date(promo.expiryDate))) ?: "HSD: ${sdf.format(java.util.Date(promo.expiryDate))}"
            } else context?.getString(R.string.ai_no_limit_time) ?: "Không giới hạn thời gian"
            
            context?.getString(R.string.ai_promo_item_format, promo.code, discountText, minOrderText, expiryText)
                ?: "- Mã: ${promo.code} | $discountText | $minOrderText | $expiryText"
        }
    }

    private fun formatVND(amount: Double): String {
        val formatter = java.text.DecimalFormat("#,###")
        return "${formatter.format(amount.toLong())}₫"
    }

    private fun getCommonRules(context: android.content.Context?): String {
        return context?.getString(R.string.ai_prompt_common_rules) ?: """
            QUY TẮC PHẢN HỒI BẮT BUỘC:
            ...
        """.trimIndent()
    }

    private suspend fun requestGeminiWithFallback(sys: String, history: List<ChatMessage>, user: String, context: android.content.Context?): String {
        // gemini-2.0-flash-lite và gemini-1.5-flash đều không có free tier quota → loại khỏi danh sách
        // Chỉ giữ các model có free tier hợp lệ
        val models = listOf(geminiModel, "gemini-2.5-flash", "gemini-2.0-flash").distinct()
        var lastErr: Exception? = null
        var allQuotaExhausted = true

        for (m in models) {
            try {
                Log.d("AI_CHAT", "Trying Gemini: $m")
                return callGeminiApi(m, sys, history, user)
            } catch (e: Exception) {
                lastErr = e
                Log.w("AI_CHAT", "Gemini $m failed: ${e.message}")
                val isQuotaError = e.message?.contains("429") == true
                        || e.message?.contains("quota") == true
                        || e.message?.contains("RESOURCE_EXHAUSTED") == true
                if (isQuotaError) {
                    continue // thử model tiếp theo
                } else {
                    allQuotaExhausted = false
                    throw e // lỗi khác (404, 400...) → throw ngay, không retry
                }
            }
        }

        // Tất cả model đều bị quota → hiện thông báo thân thiện thay vì JSON thô
        if (allQuotaExhausted) {
            throw Exception(context?.getString(R.string.ai_busy_msg) ?: "Cửa hàng đang bận, bạn vui lòng thử lại sau vài giây nhé!")
        }
        throw lastErr ?: Exception(context?.getString(R.string.ai_not_connected) ?: "Không thể kết nối AI lúc này.")
    }

    /**
     * Gửi tin nhắn kèm hình ảnh (Vision)
     */
    fun sendMessageWithImageStream(userMessage: String, base64Image: String, history: List<ChatMessage>, context: android.content.Context?): Flow<String> = flow {
        val uid = auth.currentUser?.uid ?: throw Exception(context?.getString(R.string.login_required_msg) ?: "Vui lòng đăng nhập")
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
        val userContext = fetchUserContext(uid, allProducts.associateBy { it.id }, context)

        // Hướng dẫn đặc biệt cho Vision
        val visionSystemInstruction = """
            ${context?.getString(R.string.ai_vision_prompt_role) ?: "BẠN LÀ CHUYÊN VIÊN TƯ VẤN THỊ GIÁC CỦA EASYSHOP."}
            ${context?.getString(R.string.ai_prompt_user_context) ?: "NGỮ CẢNH:"} $userContext
            
            ${context?.getString(R.string.ai_prompt_available_products) ?: "KHO HÀNG SHOP ĐANG CÓ:"}
            ${formatProductsForAI(allProducts, context)}
            
            ${context?.getString(R.string.ai_vision_prompt_task) ?: "NHIỆM VỤ CỦA BẠN..."}
            
            ${getCommonRules(context)}
        """.trimIndent()

        // 4. Gọi AI Vision — Gemini trước (miễn phí), Beeknoee fallback khi Gemini hết quota
        var aiResponse = ""
        val geminiVisionError = runCatching {
            aiResponse = requestGeminiVisionWithFallback(visionSystemInstruction, history, userMessage, base64Image, context)
        }.exceptionOrNull()

        if (aiResponse.isBlank()) {
            val isQuota = geminiVisionError?.message?.contains("bận") == true
                    || geminiVisionError?.message?.contains("429") == true
                    || geminiVisionError?.message?.contains("503") == true
                    || geminiVisionError?.message?.contains("RESOURCE_EXHAUSTED") == true
                    || geminiVisionError?.message?.contains("UNAVAILABLE") == true

            if (isQuota && beeknoeeApiKey.isNotBlank()) {
                Log.w("AI_CHAT", "Gemini Vision quota/unavailable → switching to Beeknoee Vision fallback...")
                try {
                    aiResponse = requestBeeknoeeReply(visionSystemInstruction, history, userMessage, base64Image)
                } catch (be: Exception) {
                    Log.e("AI_CHAT", "All AI providers failed: ${be.message}")
                    aiResponse = context?.getString(R.string.ai_generic_error) ?: "Xin lỗi, mình gặp trục trặc khi kết nối với máy chủ AI. Bạn vui lòng thử lại sau nhé!"
                }
            } else {
                aiResponse = context?.getString(R.string.ai_generic_error) ?: "Xin lỗi, mình gặp trục trặc khi kết nối với máy chủ AI. Bạn vui lòng thử lại sau nhé!"
            }
        }

        // 4. Lưu phản hồi AI
        if (aiResponse.isNotBlank()) {
            collection.add(ChatMessage(content = aiResponse, isUser = false, timestamp = Timestamp.now())).await()
            db.collection("ai_chats").document(uid).update("lastActivity", FieldValue.serverTimestamp())
            emit(aiResponse)
        }
    }

    private suspend fun requestGeminiVisionWithFallback(sys: String, history: List<ChatMessage>, user: String, base64Image: String, context: android.content.Context?): String {
        val models = listOf(geminiModel, "gemini-2.5-flash", "gemini-2.0-flash").distinct()
        var lastErr: Exception? = null
        var allQuotaExhausted = true

        for (m in models) {
            try {
                Log.d("AI_CHAT", "Trying Gemini Vision: $m")
                return callGeminiApi(m, sys, history, user, base64Image)
            } catch (e: Exception) {
                lastErr = e
                Log.w("AI_CHAT", "Gemini Vision $m failed: ${e.message}")
                val isQuotaError = e.message?.contains("429") == true
                        || e.message?.contains("quota") == true
                        || e.message?.contains("RESOURCE_EXHAUSTED") == true
                if (isQuotaError) {
                    continue
                } else {
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
        val rawHistory = history.takeLast(10).mapNotNull { msg ->
            val role = if (msg.isUser) "user" else "assistant"
            val text = msg.content.trim()
            if (text.isNotEmpty()) mapOf("role" to role, "content" to text) else null
        }

        // Loại bỏ các tin nhắn có role liên tiếp giống nhau (user→user hoặc assistant→assistant)
        // vì một số model sẽ trả lỗi 400 với sequence không hợp lệ
        val chatHistory = mutableListOf<Map<String, Any>>()
        for (msg in rawHistory) {
            if (chatHistory.isEmpty() || chatHistory.last()["role"] != msg["role"]) {
                chatHistory.add(msg)
            } else {
                // Merge nội dung vào tin nhắn cuối cùng cùng role thay vì bỏ qua
                val last = chatHistory.last().toMutableMap()
                last["content"] = "${last["content"]}\n${msg["content"]}"
                chatHistory[chatHistory.lastIndex] = last
            }
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
            normalized.contains("thanh toan") || normalized.contains("chuyen khoan") ||
                    normalized.contains("tra gop") || normalized.contains("cod") ||
                    normalized.contains("momo") || normalized.contains("mbbank") ||
                    normalized.contains("visa") || normalized.contains("mastercard") ||
                    normalized.contains("qr") || normalized.contains("tien mat") -> "payment"
            normalized.contains("so sanh") -> "comparison"
            normalized.contains("gia") || normalized.contains("re") -> "budget"
            else -> "advice"
        }
    }

    private fun foldText(text: String): String = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace("đ", "d")

    private fun getSearchTokens(text: String): List<String> {
        return text.split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in STOP_WORDS }
    }

    /**
     * @param query          Câu query đã normalize (dùng để match nguyên cụm)
     * @param allTokens      Tất cả token = query hiện tại + history (dùng để match từng từ)
     * @param primaryTokens  Chỉ token từ query hiện tại (điểm cao hơn history tokens)
     */
    private fun calculateProductScore(
        product: ProductModel,
        query: String,
        allTokens: List<String>,
        primaryTokens: List<String> = allTokens
    ): Int {
        var score = 0
        val titleFolded = foldText(product.title)
        val categoryFolded = foldText(product.category.ifBlank { "" })

        // Match nguyên câu — ưu tiên tuyệt đối
        if (titleFolded.contains(query)) score += 10

        // Match token từ query hiện tại — ưu tiên cao
        primaryTokens.forEach { token ->
            if (titleFolded.contains(token)) score += 3
            if (categoryFolded.contains(token)) score += 1
        }

        // Match token bổ sung từ history — ưu tiên thấp hơn
        val historyOnlyTokens = allTokens - primaryTokens.toSet()
        historyOnlyTokens.forEach { token ->
            if (titleFolded.contains(token)) score += 1
        }

        return score
    }
}
