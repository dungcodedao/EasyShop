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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.concurrent.TimeUnit

class AiRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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

    suspend fun sendMessage(userMessage: String, history: List<ChatMessage>): Result<String> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Chua dang nhap"))
        return try {
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
            val userContext = fetchUserContext(userId, productsById)
            val aiReply = callGeminiApi(
                currentMessage = userMessage,
                history = history,
                productsContext = buildProductsContext(selectedProducts),
                userContext = buildUserContext(userContext),
                intent = detectIntent(userMessage)
            )

            chatDocRef.collection("messages").add(
                hashMapOf(
                    "content" to aiReply,
                    "isUser" to false,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
            chatDocRef.update("lastActivity", FieldValue.serverTimestamp())

            Result.success(aiReply)
        } catch (e: Exception) {
            Log.e("AI_CHAT", "sendMessage failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun callGeminiApi(
        currentMessage: String,
        history: List<ChatMessage>,
        productsContext: String,
        userContext: String,
        intent: String
    ): String = withContext(Dispatchers.IO) {
        val systemInstruction = """
            You are EasyShop Copilot, a modern shopping advisor inside a Vietnamese ecommerce app.
            Current detected intent: $intent

            Hard rules:
            1) Always reply in natural Vietnamese, concise, practical, and up-to-date in tone.
            2) Do not use generic old-school sales language. Focus on concrete recommendation logic.
            3) You must only recommend products from the catalog context below.
            4) If recommending a product, append its exact id in square brackets, and ONLY the id in the brackets. Example: Ten san pham [abc123]
            5) Never invent product IDs, prices, stock, specs, promotions, or policies not present in context.
            6) Prioritize in-stock items. If user asks for unavailable specs, say it clearly and suggest nearest alternatives.
            7) Personalize using user context (recent orders/cart/preferences) when relevant, but keep privacy-safe wording.
            8) Default answer structure: quick conclusion, 2-3 fitting options with reason + price, then one short next-step question.
            9) Keep response around 5-10 short lines unless user requests deeper detail.

            User context:
            $userContext

            Catalog context:
            $productsContext
        """.trimIndent()

        val body = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.55)
                put("topP", 0.9)
                put("topK", 40)
                put("maxOutputTokens", 720)
            })

            val contentsArray = JSONArray()
            val recentHistory = history.takeLast(14)
            recentHistory.forEach { msg ->
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "model")
                    put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                })
            }

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", currentMessage)))
            })

            put("contents", contentsArray)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url(apiUrl).post(body).build()
        val response = httpClient.newCall(request).execute()
        val responseStr = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                JSONObject(responseStr).getJSONObject("error").getString("message")
            } catch (_: Exception) {
                "Loi ${response.code}: $responseStr"
            }
            throw Exception(errorMsg)
        }

        val root = JSONObject(responseStr)
        val candidates = root.optJSONArray("candidates") ?: throw Exception("No candidates from model")

        for (i in 0 until candidates.length()) {
            val candidate = candidates.optJSONObject(i) ?: continue
            val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: continue
            val combinedText = buildString {
                for (j in 0 until parts.length()) {
                    val text = parts.optJSONObject(j)?.optString("text").orEmpty().trim()
                    if (text.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(text)
                    }
                }
            }
            if (combinedText.isNotBlank()) {
                return@withContext combinedText
            }
        }

        throw Exception("Model returned empty text")
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
            val welcomeText = "Chao ban! Toi la tro ly AI cua EasyShop. Ban can minh goi y san pham theo nhu cau, ngan sach, hay so sanh nhanh khong?"
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
        return normalized.replace(Regex("\\p{M}+"), " ").replace(Regex("\\s+"), " ").trim()
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
                    title.contains(token) -> score += 4
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
            return "Catalog is currently unavailable."
        }

        val lines = products.mapIndexed { index, product ->
            val topSpecs = product.otherDetails.entries
                .take(3)
                .joinToString(", ") { "${it.key}: ${it.value}" }
                .ifBlank { "n/a" }

            val price = product.price.ifBlank { "n/a" }
            val oldPrice = product.actualPrice.takeIf { it.isNotBlank() && it != product.price }
            val status = if (product.inStock) "in_stock" else "out_of_stock"
            val category = product.category.ifBlank { "unknown" }
            val shortDescription = product.description.take(120).replace("\n", " ").ifBlank { "n/a" }

            buildString {
                append("${index + 1}. ${product.title}")
                append(" | id=[${product.id}]")
                append(" | category=$category")
                append(" | price=$price")
                if (!oldPrice.isNullOrBlank()) append(" | old_price=$oldPrice")
                append(" | status=$status")
                append(" | specs=$topSpecs")
                append(" | summary=$shortDescription")
            }
        }

        return lines.joinToString(separator = "\n")
    }

    private suspend fun fetchUserContext(
        userId: String,
        productsById: Map<String, ProductModel>
    ): UserChatContext {
        val userDoc = runCatching { db.collection("users").document(userId).get().await() }.getOrNull()

        val name = userDoc?.getString("name")
            ?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.displayName
            ?: "Khach hang"
        val email = userDoc?.getString("email")
            ?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email
            ?: "unknown"
        val role = userDoc?.getString("role").orEmpty().ifBlank { "user" }

        val cartItems = readCartItems(userDoc?.get("cartItems"))
        val cartSummary = cartItems.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString("; ") { (productId, qty) ->
                val productName = productsById[productId]?.title ?: productId
                "$productName x$qty"
            }
            .ifBlank { "empty_cart" }

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
            .ifBlank { "no_recent_orders" }

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
            .ifBlank { "unknown" }

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
            name=${context.name}
            email=${context.email}
            role=${context.role}
            cart_top=${context.cartSummary}
            recent_orders=${context.recentOrdersSummary}
            favorite_categories=${context.favoriteCategories}
            avg_delivered_order_vnd=${context.averageDeliveredOrderValue.toLong()}
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
