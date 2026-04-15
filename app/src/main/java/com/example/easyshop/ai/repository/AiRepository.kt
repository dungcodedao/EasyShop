package com.example.easyshop.ai.repository

import android.util.Log
import com.example.easyshop.BuildConfig
import com.example.easyshop.ai.model.ChatMessage
import com.example.easyshop.model.OrderModel
import com.example.easyshop.model.ProductModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.FinishReason
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.text.Normalizer

class AiRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private var cachedProducts: List<ProductModel> = emptyList()
    private var lastProductsFetchTimeMs: Long = 0L
    private val productsCacheTtlMs = 90_000L

    private data class UserChatContext(
        val name: String,
        val email: String,
        val role: String,
        val cartSummary: String,
        val recentOrdersSummary: String,
        val favoriteCategories: String,
        val averageDeliveredOrderValue: Double
    )

    fun getMessages(): Flow<List<ChatMessage>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val listener = db.collection("chats")
            .document(userId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    private suspend fun fetchAllProducts(): List<ProductModel> {
        val now = System.currentTimeMillis()
        if (cachedProducts.isNotEmpty() && (now - lastProductsFetchTimeMs) < productsCacheTtlMs) {
            return cachedProducts
        }

        return try {
            val docs = db.collection("data")
                .document("stock")
                .collection("products")
                .get()
                .await()

            val productList = docs.mapNotNull { doc ->
                doc.toObject(ProductModel::class.java)?.apply { id = doc.id }
            }

            cachedProducts = productList
            lastProductsFetchTimeMs = now
            productList
        } catch (e: Exception) {
            Log.e("AI_CHAT", "fetchAllProducts failed: ${e.message}")
            cachedProducts
        }
    }

    fun sendMessageStream(userMessage: String, history: List<ChatMessage>): Flow<String> = flow {
        val userId = auth.currentUser?.uid ?: throw Exception("Chua dang nhap")
        val chatDocRef = db.collection("chats").document(userId)

        chatDocRef.collection("messages").add(
            hashMapOf(
                "content" to userMessage,
                "isUser" to true,
                "timestamp" to FieldValue.serverTimestamp()
            )
        ).await()

        val allProducts = fetchAllProducts()
        val relevantProducts = selectRelevantProducts(userMessage, history, allProducts)
        val selectedProducts = when {
            relevantProducts.isNotEmpty() -> relevantProducts.take(14)
            allProducts.isNotEmpty() -> allProducts
                .sortedWith(compareByDescending<ProductModel> { it.inStock }.thenBy { parseCurrencyToLong(it.price) ?: Long.MAX_VALUE })
                .take(14)
            else -> emptyList()
        }

        val productsById = allProducts.filter { it.id.isNotBlank() }.associateBy { it.id }
        val userContextInfo = fetchUserContext(userId, productsById)

        val intent = detectIntent(userMessage)
        val userContextStr = buildUserContext(userContextInfo)
        val productsContextStr = buildProductsContext(selectedProducts)

        val systemInstructionStr = """
            BẠN LÀ: AI EasyShop - Chuyên gia tư vấn mua sắm công nghệ cao cấp tại Việt Nam.
            Ý ĐỊNH NGƯỜI DÙNG: $intent

            PHONG CÁCH PHỤC VỤ (CORE RULES):
            1) Ngôn ngữ: Tiếng Việt tự nhiên, tinh tế, am hiểu sản phẩm. Tuyệt đối không dùng từ ngữ máy móc hoặc sai chính tả.
            2) Tránh lặp lại: Không lặp lại câu chào "Chào bạn..." nếu người dùng đang trong luồng hội thoại. Chỉ chào ở tin nhắn đầu tiên.
            3) Tư vấn chuyên sâu: Khi so sánh, hãy tập trung vào sự khác biệt thực tế về hiệu năng, giá trị sử dụng thay vì chỉ liệt kê thông số.
            4) Quy tắc ID sản phẩm: Luôn kèm ID chính xác trong ngoặc vuông ngay sau tên sản phẩm nếu có trong ngữ cảnh. Ví dụ: Samsung S25 Ultra [s25u_01].
            5) Tuyệt đối không bịa đặt: Chỉ tư vấn dựa trên dữ liệu sản phẩm thật bên dưới. Nếu không có máy người dùng hỏi, hãy xin lỗi và gợi ý máy tương đương tốt nhất.
            6) Trình bày: Sử dụng bullet points (-) để so sánh hoặc liệt kê cho dễ đọc. 
            7) Luôn kết thúc bằng một câu hỏi gợi mở để hỗ trợ người dùng chốt đơn hoặc tìm hiểu thêm (VD: "Bạn có muốn mình so sánh camera của 2 máy này không?").
            8) Tự do ngôn luận: Bạn có thể trả lời chi tiết và đầy đủ nhất có thể để làm hài lòng người dùng, không bị giới hạn số dòng.

            NGỮ CẢNH KHÁCH HÀNG:
            $userContextStr

            DANH MỤC SẢN PHẨM KHẢ DỤNG:
            $productsContextStr
        """.trimIndent()

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            systemInstruction = content { text(systemInstructionStr) },
            generationConfig = generationConfig {
                temperature = 0.6f
                topP = 0.95f
                topK = 40
                maxOutputTokens = 1024
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
            )
        )

        val chatContextContents = history.takeLast(14)
            .filter { msg ->
                msg.isUser || (msg.content.length > 50 || !msg.content.contains("Xin lỗi"))
            }
            .map { msg ->
                content(if (msg.isUser) "user" else "model") { text(msg.content) }
            }.toMutableList()

        chatContextContents.add(content("user") { text(userMessage) })

        var aiReplyAccumulator = ""

        val responseStream = generativeModel.generateContentStream(*chatContextContents.toTypedArray())

        try {
            responseStream.collect { chunk ->
                val finishReason = chunk.candidates.firstOrNull()?.finishReason
                if (finishReason != null && finishReason != FinishReason.STOP) {
                    Log.w("AI_DEBUG", "AI dừng sớm vì lý do: $finishReason")
                }

                val textPart = try {
                    chunk.text ?: ""
                } catch (e: Exception) {
                    if (e.message?.contains("SAFETY", ignoreCase = true) == true) {
                        throw Exception("Nội dung bị bộ lọc an toàn chặn. Hãy thử diễn đạt khác.")
                    }
                    ""
                }
                if (textPart.isNotEmpty()) {
                    aiReplyAccumulator += textPart
                    emit(aiReplyAccumulator)
                }
            }
        } catch (e: Exception) {
            Log.e("AI_CHAT_ERROR", "Lỗi khi stream: ${e.message}", e)
            val msg = e.message ?: ""
            if (msg.contains("MAX_TOKENS", ignoreCase = true)) {
                if (aiReplyAccumulator.isNotBlank()) {
                    emit(aiReplyAccumulator + "\n\n*(Hệ thống: Do nội dung quá dài nên mình xin phép ngắt lời tại đây. Bạn muốn mình nói tiếp về phần nào không?)*")
                } else {
                    throw Exception("Câu hỏi quá dài khiến mình bị 'quá tải', bạn thử hỏi ngắn gọn hơn nhé!")
                }
            } else if (!msg.contains("MAX_TOKENS", ignoreCase = true) || aiReplyAccumulator.isBlank()) {
                throw e
            }
        }

        if (aiReplyAccumulator.isNotBlank()) {
            chatDocRef.collection("messages").add(
                hashMapOf(
                    "content" to aiReplyAccumulator,
                    "isUser" to false,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
            chatDocRef.update("lastActivity", FieldValue.serverTimestamp())
        }
    }

    suspend fun clearChat() {
        val userId = auth.currentUser?.uid ?: return
        val docs = db.collection("chats").document(userId).collection("messages").get().await()
        docs.documents.forEach { it.reference.delete() }
        checkAndShowWelcomeMessage()
    }

    suspend fun checkAndShowWelcomeMessage() {
        val userId = auth.currentUser?.uid ?: return
        val messagesCount = db.collection("chats")
            .document(userId)
            .collection("messages")
            .limit(1)
            .get()
            .await()
            .size()

        if (messagesCount == 0) {
            val welcomeText = "Chào bạn! Tôi là AI EasyShop. Bạn cần mình gợi ý sản phẩm theo nhu cầu, ngân sách, hay so sánh nhanh không?"
            db.collection("chats").document(userId).collection("messages").add(
                hashMapOf(
                    "content" to welcomeText,
                    "isUser" to false,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("chats").document(userId).update("lastActivity", FieldValue.serverTimestamp())
        }
    }

    private fun detectIntent(message: String): String {
        val msg = foldText(message)
        return when {
            listOf("so sanh", "compare", "khac nhau").any { msg.contains(it) } -> "comparison"
            listOf("ngan sach", "duoi", "toi da", "tam gia", "bao nhieu").any { msg.contains(it) } -> "budget_advice"
            listOf("con hang", "het hang", "in stock", "stock").any { msg.contains(it) } -> "stock_check"
            listOf("don hang", "giao den", "shipping", "van chuyen", "order").any { msg.contains(it) } -> "order_support"
            listOf("khuyen mai", "giam gia", "deal", "voucher").any { msg.contains(it) } -> "promotion"
            else -> "product_advice"
        }
    }

    private fun foldText(raw: String): String {
        val normalized = Normalizer.normalize(raw.lowercase(), Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{M}+"), "").replace(Regex("\\s+"), " ").trim()
    }

    private fun tokenize(raw: String): List<String> {
        return foldText(raw)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 }
            .distinct()
    }

    private fun parseCurrencyToLong(raw: String): Long? {
        val digits = raw.filter(Char::isDigit)
        return digits.toLongOrNull()
    }

    private fun extractBudgetVnd(raw: String): Long? {
        val folded = foldText(raw)
        val mPattern = Regex("(\\d+(?:[\\.,]\\d+)?)\\s*(tr|trieu|m|million)")
        val kPattern = Regex("(\\d+(?:[\\.,]\\d+)?)\\s*(k|nghin)")

        mPattern.find(folded)?.let { m ->
            val value = m.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@let
            return (value * 1_000_000L).toLong()
        }

        kPattern.find(folded)?.let { k ->
            val value = k.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@let
            return (value * 1_000L).toLong()
        }

        return Regex("\\b\\d{6,11}\\b").find(folded)?.value?.toLongOrNull()
    }

    private fun selectRelevantProducts(
        currentMessage: String,
        history: List<ChatMessage>,
        allProducts: List<ProductModel>
    ): List<ProductModel> {
        if (allProducts.isEmpty()) return emptyList()

        val recentUserContext = history
            .filter { it.isUser }
            .takeLast(4)
            .joinToString(" ") { it.content }

        val querySeed = "$currentMessage $recentUserContext".trim()
        val queryTokens = tokenize(querySeed)
        val messageFolded = foldText(currentMessage)
        val budget = extractBudgetVnd(querySeed)

        val scored = allProducts.map { product ->
            val title = foldText(product.title)
            val category = foldText(product.category)
            val description = foldText(product.description.take(260))
            val specs = foldText(
                product.otherDetails.entries
                    .take(10)
                    .joinToString(" ") { "${it.key} ${it.value}" }
            )

            var score = 0

            if (messageFolded.length >= 6 && title.contains(messageFolded.take(40))) {
                score += 7
            }

            queryTokens.forEach { token ->
                when {
                    title.contains(token) -> score += 10 // Tăng mạnh điểm nếu khớp tên máy
                    category.contains(token) -> score += 3
                    specs.contains(token) -> score += 2
                    description.contains(token) -> score += 1
                }
            }

            if (product.inStock) score += 2 else score -= 2

            val price = parseCurrencyToLong(product.price) ?: parseCurrencyToLong(product.actualPrice)
            if (budget != null && price != null) {
                when {
                    price <= budget -> score += 3
                    price <= (budget * 1.15).toLong() -> score += 1
                    else -> score -= 1
                }
            }

            if (messageFolded.contains("giam gia") || messageFolded.contains("khuyen mai") || messageFolded.contains("deal")) {
                val current = parseCurrencyToLong(product.price)
                val old = parseCurrencyToLong(product.actualPrice)
                if (current != null && old != null && old > current) score += 2
            }

            product to score
        }

        return scored
            .sortedWith(
                compareByDescending<Pair<ProductModel, Int>> { it.second }
                    .thenByDescending { it.first.inStock }
                    .thenBy { parseCurrencyToLong(it.first.price) ?: Long.MAX_VALUE }
            )
            .filter { it.second > 0 }
            .map { it.first }
            .distinctBy { it.id }
    }

    private fun buildProductsContext(products: List<ProductModel>): String {
        if (products.isEmpty()) {
            return "Danh mục sản phẩm hiện không khả dụng."
        }

        return products.mapIndexed { index, product ->
            val isMain = index < 3 // Chỉ lấy thông số kỹ thuật đầy đủ cho 3 sản phẩm đầu tiên

            val price = product.price.ifBlank { "không có" }
            val category = product.category.ifBlank { "chưa_phân_loại" }

            if (isMain) {
                val topSpecs = product.otherDetails.entries
                    .take(4)
                    .joinToString(", ") { "${it.key}: ${it.value}" }
                    .ifBlank { "không có" }

                val oldPrice = product.actualPrice.takeIf { it.isNotBlank() && it != product.price }
                val status = if (product.inStock) "còn_hàng" else "hết_hàng"
                val shortDescription = product.description.take(150).replace("\n", " ").ifBlank { "không có mô tả" }

                buildString {
                    append("${index + 1}. ${product.title} [${product.id}]")
                    append(" | giá=$price")
                    if (!oldPrice.isNullOrBlank()) append(" | giá_cũ=$oldPrice")
                    append(" | trạng_thái=$status | danh_mục=$category")
                    append("\n   - Thông số: $topSpecs")
                    append("\n   - Tóm tắt: $shortDescription...")
                }
            } else {
                // Nén tối đa để tiết kiệm bộ nhớ
                "${index + 1}. ${product.title} [${product.id}] | giá=$price | danh_mục=$category"
            }
        }.joinToString(separator = "\n\n")
    }

    private suspend fun fetchUserContext(
        userId: String,
        productsById: Map<String, ProductModel>
    ): UserChatContext {
        val userDoc = runCatching { db.collection("users").document(userId).get().await() }.getOrNull()

        val name = userDoc?.getString("name")
            ?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.displayName
            ?: "Khách hàng"
        val email = userDoc?.getString("email")
            ?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email
            ?: "không_rõ"
        val role = userDoc?.getString("role").orEmpty().ifBlank { "người_dùng" }

        val cartItems = readCartItems(userDoc?.get("cartItems"))
        val cartSummary = cartItems.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString("; ") { (productId, qty) ->
                val productName = productsById[productId]?.title ?: productId
                "$productName x$qty"
            }
            .ifBlank { "giỏ_hàng_trống" }

        val orders = runCatching {
            db.collection("orders")
                .whereEqualTo("userId", userId)
                .limit(20)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> doc.toObject(OrderModel::class.java)?.copy(id = doc.id) }
                .sortedByDescending { it.date.toDate().time }
        }.getOrElse { emptyList() }

        val recentOrdersSummary = orders
            .take(4)
            .joinToString("; ") { order ->
                val shortId = order.id.take(8).uppercase()
                "#$shortId:${order.status}:${order.total.toLong()}"
            }
            .ifBlank { "không_có_đơn_hang_gần_đây" }

        val deliveredOrders = orders.filter { it.status == "DELIVERED" }
        val avgDeliveredOrderValue = deliveredOrders.map { it.total }.average().takeIf { !it.isNaN() } ?: 0.0

        val categoryCount = mutableMapOf<String, Long>()
        deliveredOrders.forEach { order ->
            order.items.forEach { (productId, quantity) ->
                val category = productsById[productId]?.category.orEmpty().ifBlank { "other" }
                categoryCount[category] = (categoryCount[category] ?: 0L) + quantity
            }
        }

        val favoriteCategories = categoryCount.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { "${it.key}(${it.value})" }
            .ifBlank { "chưa_rõ" }

        return UserChatContext(
            name = name,
            email = email,
            role = role,
            cartSummary = cartSummary,
            recentOrdersSummary = recentOrdersSummary,
            favoriteCategories = favoriteCategories,
            averageDeliveredOrderValue = avgDeliveredOrderValue
        )
    }

    private fun buildUserContext(context: UserChatContext): String {
        return """
            tên=${context.name}
            email=${context.email}
            vai_trò=${context.role}
            giỏ_hàng=${context.cartSummary}
            đơn_hàng_gần_đây=${context.recentOrdersSummary}
            danh_mục_yêu_thích=${context.favoriteCategories}
            giá_trị_đơn_hàng_trung_bình_vnd=${context.averageDeliveredOrderValue.toLong()}
        """.trimIndent()
    }

    private fun readCartItems(raw: Any?): Map<String, Long> {
        val map = raw as? Map<*, *> ?: return emptyMap()
        return map.entries.mapNotNull { entry ->
            val key = entry.key?.toString().orEmpty().trim()
            val qty = toLongSafe(entry.value)
            if (key.isNotBlank() && qty > 0) key to qty else null
        }.toMap()
    }

    private fun toLongSafe(value: Any?): Long {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }
}