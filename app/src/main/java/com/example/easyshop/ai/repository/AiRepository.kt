package com.example.easyshop.ai.repository

import android.util.Log
import com.example.easyshop.BuildConfig
import com.example.easyshop.ai.model.ChatMessage
import com.example.easyshop.model.OrderModel
import com.example.easyshop.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
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
    companion object {
        private const val DEFAULT_BEE_BASE_URL = "https://platform.beeknoee.com/api/v1"
    }

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val beeknoeeApiKey = BuildConfig.BEEKNOEE_API_KEY
    private val beeknoeeBaseUrl = BuildConfig.BEEKNOEE_BASE_URL
        .trim()
        .trim('"')
        .trimEnd('/')
        .let { configured ->
            if (configured.startsWith("https://") || configured.startsWith("http://")) {
                configured
            } else {
                DEFAULT_BEE_BASE_URL
            }
        }
    private val beeknoeeModel = BuildConfig.BEEKNOEE_MODEL.ifBlank { "deepseek-chat" }
    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS)
        .writeTimeout(70, TimeUnit.SECONDS)
        .build()

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
        val averageDeliveredOrderValue: Double,
        val profileHealth: String,
        val defaultAddressSummary: String,
        val favoriteProductsSummary: String,
        val preferredPaymentMethods: String,
        val recentPromoUsage: String,
        val orderStatusSummary: String,
        val cartEstimatedValue: Long
    )

    private data class OpenAIChatResponse(
        val choices: List<OpenAIChoice> = emptyList(),
        val error: OpenAIError? = null
    )

    private data class OpenAIChoice(
        val message: OpenAIMessage? = null
    )

    private data class OpenAIMessage(
        val role: String = "",
        val content: String = ""
    )

    private data class OpenAIError(
        val message: String? = null
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
            relevantProducts.isNotEmpty() -> relevantProducts.take(18)
            allProducts.isNotEmpty() -> allProducts
                .sortedWith(compareByDescending<ProductModel> { it.inStock }.thenBy { parseCurrencyToLong(it.price) ?: Long.MAX_VALUE })
                .take(18)
            else -> emptyList()
        }

        val productsById = allProducts.filter { it.id.isNotBlank() }.associateBy { it.id }
        val userContextInfo = fetchUserContext(userId, productsById)

        val intent = detectIntent(userMessage)
        val userContextStr = buildUserContext(userContextInfo)
        val productsContextStr = buildProductsContext(selectedProducts)

        val systemInstructionStr = """
            BAN LA: EasyShop AI , tro ly tu van mua sam chuyen sau cho EasyShop.
            Y DINH NGUOI DUNG: $intent

            QUY TAC:
            1) Tra loi bang tieng Viet co dau, tu nhien, de hieu.
            2) Chi dung thong tin co trong ngu canh ben duoi. Khong bịa dat thong so.
            3) Neu nhac san pham, bat buoc giu format: Ten [id].
            4) Neu nguoi dung can tu van mua: de xuat 2-4 lua chon phu hop nhat.
            5) Moi lua chon nen co: gia, tinh trang hang, 3-5 ly do, diem can luu y.
            6) Neu nguoi dung hoi so sanh: tra loi bang bullet points theo tieu chi (hieu nang, gia tri, do ben, nang cap, gia).
            7) Neu khong co dung san pham can tim: noi ro "khong thay dung mau" va de xuat mau gan nhat.
            8) Khong tu nhan da dat don/giu hang/khuyen mai neu ngu canh khong co.
            9) Luon ket thuc bang 1 cau hoi goi mo de chot nhu cau tiep theo.
            10) Neu lien quan thanh toan/checkout: dua ra huong dan ro rang theo trang thai ho so, dia chi, gio hang va phuong thuc thanh toan thuong dung.
            11) Neu lien quan yeu thich: uu tien de xuat theo danh sach yeu thich va danh muc ua thich cua nguoi dung.
            12) Neu lien quan profile: chi ro thong tin thieu va huong dan cap nhat tung buoc ngan gon.

            MAU TRA LOI UU TIEN:
            - Tom tat nhanh nhu cau cua nguoi dung (1-2 cau).
            - Goi y chinh (2-4 san pham), moi san pham 4-7 dong ro rang.
            - Ket luan va de xuat buoc tiep theo.

            NGU CANH KHACH HANG:
            $userContextStr

            DANH MUC SAN PHAM KHA DUNG:
            $productsContextStr
        """.trimIndent()

        val aiReplyAccumulator = requestBeeSynapseReply(
            systemInstructionStr = systemInstructionStr,
            history = history,
            userMessage = userMessage
        )
        emit(aiReplyAccumulator)

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

    private suspend fun requestBeeSynapseReply(
        systemInstructionStr: String,
        history: List<ChatMessage>,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        if (beeknoeeApiKey.isBlank()) {
            throw Exception("Thieu BEEKNOEE_API_KEY trong local.properties")
        }

        val conversation = history.takeLast(14)
            .filter { msg -> msg.isUser || msg.content.length > 10 }

        val payloadMessages = mutableListOf<Map<String, String>>()
        payloadMessages.add(
            mapOf(
                "role" to "system",
                "content" to systemInstructionStr
            )
        )

        conversation.forEach { msg ->
            payloadMessages.add(
                mapOf(
                    "role" to if (msg.isUser) "user" else "assistant",
                    "content" to msg.content
                )
            )
        }

        payloadMessages.add(
            mapOf(
                "role" to "user",
                "content" to userMessage
            )
        )

        val requestPayload = mapOf(
            "model" to beeknoeeModel,
            "messages" to payloadMessages,
            "temperature" to 0.45,
            "top_p" to 0.9,
            "max_tokens" to 1600
        )

        val bodyJson = gson.toJson(requestPayload)
        val request = Request.Builder()
            .url("$beeknoeeBaseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $beeknoeeApiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val parsedError = runCatching {
                    gson.fromJson(rawBody, OpenAIChatResponse::class.java)
                        ?.error
                        ?.message
                }.getOrNull()

                val message = parsedError?.takeIf { it.isNotBlank() }
                    ?: "Beeknoee request failed (${response.code})"
                throw Exception(message)
            }

            if (rawBody.isBlank()) {
                throw Exception("AI provider tra ve rong")
            }

            val parsed = runCatching {
                gson.fromJson(rawBody, OpenAIChatResponse::class.java)
            }.getOrNull()

            val text = parsed?.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?.trim()
                .orEmpty()

            if (text.isBlank()) {
                throw Exception(parsed?.error?.message ?: "Khong nhan duoc phan hoi tu AI")
            }

            text
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
            listOf("thanh toan", "payment", "cod", "chuyen khoan", "the ngan hang", "checkout").any { msg.contains(it) } -> "payment_support"
            listOf("yeu thich", "wishlist", "favorite", "favourite").any { msg.contains(it) } -> "favorite_support"
            listOf("ho so", "profile", "dia chi", "so dien thoai", "cap nhat thong tin").any { msg.contains(it) } -> "profile_support"
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
            return "Khong co du lieu san pham kha dung."
        }

        return products.mapIndexed { index, product ->
            val isMain = index < 6
            val price = product.price.ifBlank { "khong_ro" }
            val category = product.category.ifBlank { "chua_phan_loai" }
            val oldPrice = product.actualPrice.takeIf { it.isNotBlank() && it != product.price }
            val status = if (product.inStock) "con_hang" else "het_hang"

            if (isMain) {
                val topSpecs = product.otherDetails.entries
                    .take(6)
                    .joinToString(", ") { "${it.key}: ${it.value}" }
                    .ifBlank { "khong_ro" }

                val shortDescription = product.description
                    .take(220)
                    .replace("\n", " ")
                    .ifBlank { "khong_co_mo_ta" }

                buildString {
                    append("${index + 1}. ${product.title} [${product.id}]")
                    append(" | gia_hien_tai=$price")
                    if (!oldPrice.isNullOrBlank()) append(" | gia_goc=$oldPrice")
                    append(" | tinh_trang=$status | danh_muc=$category")
                    append("\n   - thong_so_noi_bat: $topSpecs")
                    append("\n   - mo_ta_ngan: $shortDescription")
                }
            } else {
                "${index + 1}. ${product.title} [${product.id}] | gia_hien_tai=$price | tinh_trang=$status | danh_muc=$category"
            }
        }.joinToString(separator = "\n\n")
    }
        private suspend fun fetchUserContext(
        userId: String,
        productsById: Map<String, ProductModel>
    ): UserChatContext {
        val userDoc = runCatching { db.collection("users").document(userId).get().await() }.getOrNull()

        val name = userDoc?.getString("name")?.takeIf { it.isNotBlank() } ?: auth.currentUser?.displayName ?: "Khach hang"
        val email = userDoc?.getString("email")?.takeIf { it.isNotBlank() } ?: auth.currentUser?.email ?: "khong_ro"
        val role = userDoc?.getString("role").orEmpty().ifBlank { "nguoi_dung" }
        val phone = userDoc?.getString("phone").orEmpty()

        val addressMaps = (userDoc?.get("addressList") as? List<*>)
            ?.mapNotNull { it as? Map<*, *> }
            .orEmpty()
        val defaultAddressMap = addressMaps.firstOrNull { (it["isDefault"] as? Boolean) == true } ?: addressMaps.firstOrNull()
        val defaultAddressSummary = if (defaultAddressMap == null) {
            "chua_co_dia_chi_mac_dinh"
        } else {
            val label = defaultAddressMap["label"]?.toString().orEmpty().ifBlank { "dia_chi" }
            val fullName = defaultAddressMap["fullName"]?.toString().orEmpty().ifBlank { "khong_ro_ten" }
            val addr = defaultAddressMap["detailedAddress"]?.toString().orEmpty().ifBlank { "khong_ro_dia_chi" }
            val phoneAddr = defaultAddressMap["phone"]?.toString().orEmpty().ifBlank { "khong_ro_sdt" }
            "$label|$fullName|$phoneAddr|$addr"
        }

        val cartItems = readCartItems(userDoc?.get("cartItems"))
        val cartSummary = cartItems.entries
            .sortedByDescending { it.value }
            .take(4)
            .joinToString("; ") { (productId, qty) ->
                val productName = productsById[productId]?.title ?: productId
                "$productName x$qty"
            }
            .ifBlank { "gio_hang_trong" }
        val cartEstimatedValue = cartItems.entries.sumOf { (productId, qty) ->
            (parseCurrencyToLong(productsById[productId]?.price.orEmpty()) ?: 0L) * qty
        }

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
            .ifBlank { "khong_co_don_hang_gan_day" }

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
            .ifBlank { "chua_ro" }

        val orderStatusSummary = orders
            .groupingBy { it.status.ifBlank { "UNKNOWN" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .joinToString(", ") { "${it.key}:${it.value}" }
            .ifBlank { "chua_co_don_hang" }

        val preferredPaymentMethods = orders
            .map { it.paymentMethod.ifBlank { "COD" } }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { "${it.key}:${it.value}" }
            .ifBlank { "COD:0" }

        val recentPromoUsage = orders
            .map { it.promoCode.trim() }
            .filter { it.isNotBlank() && it != "NONE" }
            .distinct()
            .take(3)
            .joinToString(", ")
            .ifBlank { "khong_dung_promo_gan_day" }

        val favoriteProductsSummary = runCatching {
            db.collection("users")
                .document(userId)
                .collection("favorites")
                .limit(8)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> doc.toObject(ProductModel::class.java)?.apply { id = doc.id } }
                .take(5)
                .joinToString("; ") { "${it.title} [${it.id}]" }
        }.getOrElse { "" }.ifBlank { "chua_co_san_pham_yeu_thich" }

        val profileCompletenessScore = listOf(
            name != "Khach hang",
            email != "khong_ro",
            phone.isNotBlank(),
            defaultAddressMap != null
        ).count { it }
        val profileHealth = when (profileCompletenessScore) {
            4 -> "day_du"
            3 -> "kha_day_du"
            else -> "thieu_thong_tin"
        }

        return UserChatContext(
            name = name,
            email = email,
            role = role,
            cartSummary = cartSummary,
            recentOrdersSummary = recentOrdersSummary,
            favoriteCategories = favoriteCategories,
            averageDeliveredOrderValue = avgDeliveredOrderValue,
            profileHealth = profileHealth,
            defaultAddressSummary = defaultAddressSummary,
            favoriteProductsSummary = favoriteProductsSummary,
            preferredPaymentMethods = preferredPaymentMethods,
            recentPromoUsage = recentPromoUsage,
            orderStatusSummary = orderStatusSummary,
            cartEstimatedValue = cartEstimatedValue
        )
    }

    private fun buildUserContext(context: UserChatContext): String {
        return """
            ten=${context.name}
            email=${context.email}
            vai_tro=${context.role}
            profile_health=${context.profileHealth}
            dia_chi_mac_dinh=${context.defaultAddressSummary}
            gio_hang=${context.cartSummary}
            uoc_tinh_gia_tri_gio_hang_vnd=${context.cartEstimatedValue}
            don_hang_gan_day=${context.recentOrdersSummary}
            thong_ke_trang_thai_don=${context.orderStatusSummary}
            danh_muc_ua_thich=${context.favoriteCategories}
            san_pham_yeu_thich=${context.favoriteProductsSummary}
            phuong_thuc_thanh_toan_ua_dung=${context.preferredPaymentMethods}
            promo_gan_day=${context.recentPromoUsage}
            gia_tri_don_da_giao_trung_binh_vnd=${context.averageDeliveredOrderValue.toLong()}
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
