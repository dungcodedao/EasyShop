package com.example.easyshop.ai.repository

import android.util.Log
import com.example.easyshop.BuildConfig
import com.example.easyshop.ai.model.ChatMessage
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
import com.example.easyshop.model.ProductModel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
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

    // Tự động kéo toàn bộ danh sách sản phẩm từ DB lên để nhét vào não AI
    private suspend fun fetchAllProductsForAi(): String {
        return try {
            val docs = db.collection("data").document("stock").collection("products").get().await()
            val productList = docs.mapNotNull { doc -> 
                doc.toObject(ProductModel::class.java)?.apply { id = doc.id } 
            }
            
            // Xây dựng một chuỗi danh mục dễ hiểu cho AI
            val sb = StringBuilder()
            sb.append("Danh sách sản phẩm hiện có:\n")
            productList.forEach { p ->
                sb.append("- Tên: ${p.title}. PID: [${p.id}]. Giá: ${p.price}. Còn hàng: ${p.inStock}.\n")
            }
            sb.toString()
        } catch (e: Exception) {
            "Không thể lấy danh sách sản phẩm lúc này."
        }
    }

    suspend fun sendMessage(userMessage: String, history: List<ChatMessage>): Result<String> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Chưa đăng nhập"))
        return try {
            val chatDocRef = db.collection("chats").document(userId)

            // Lưu tin nhắn người dùng
            chatDocRef.collection("messages").add(
                hashMapOf(
                    "content" to userMessage,
                    "isUser" to true,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            // Gọi Gemini API qua OkHttp kèm theo lịch sử để AI có "trí nhớ"
            val productsContext = fetchAllProductsForAi()
            val aiReply = callGeminiApi(userMessage, history, productsContext)

            // Lưu phản hồi AI
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
            Log.e("AI_CHAT", "Lỗi: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun callGeminiApi(currentMessage: String, history: List<ChatMessage>, productsContext: String): String = withContext(Dispatchers.IO) {
        // Thêm hướng dẫn để AI trả lời ngắn gọn, súc tích
        val systemInstruction = "Bạn là trợ lý AI chuyên nghiệp của cửa hàng EasyShop. " +
                "Hãy trả lời thân thiện, nhiệt tình. BẠN CHỈ ĐƯỢC PHÉP GIỚI THIỆU CÁC SẢN PHẨM CÓ TRONG DANH SÁCH SAU ĐÂY:\n$productsContext\n\n" +
                "NẾU KHÁCH TÌM SẢN PHẨM MÀ KHÔNG CÓ TRONG DANH SÁCH, hãy báo hết hàng hoặc không có chức năng đó. Tránh viết quá dài. " +
                "NẾU BẠN GỢI Ý MỘT SẢN PHẨM, HÃY KÈM THEO CHÍNH XÁC ID CỦA SẢN PHẨM ĐÓ TRONG DẤU NGOẶC VUÔNG, TUYỆT ĐỐI KHÔNG THÊM BẤT KỲ CHỮ NÀO KHÁC VÀO TRONG NGOẶC. VÍ DỤ NẾU SẢN PHẨM CÓ PID: [12345] THÌ BẠN VIẾT: Điện thoại X [12345]. Các text tư vấn khác cứ viết bình thường."

        val body = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONObject().apply {
                    put("text", systemInstruction)
                })
            })
            
            val contentsArray = JSONArray()
            
            // 1. Thêm lịch sử cũ (Giới hạn 10 tin nhắn gần nhất để tối ưu)
            val recentHistory = history.takeLast(10)
            recentHistory.forEach { msg ->
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", msg.content))
                    })
                })
            }
            
            // 2. Thêm tin nhắn hiện tại của người dùng
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", currentMessage))
                })
            })

            put("contents", contentsArray)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url(apiUrl).post(body).build()
        val response = httpClient.newCall(request).execute()
        val responseStr = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                JSONObject(responseStr).getJSONObject("error").getString("message")
            } catch (e: Exception) {
                "Lỗi ${response.code}: $responseStr"
            }
            throw Exception(errorMsg)
        }

        JSONObject(responseStr)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }

    suspend fun clearChat() {
        val userId = auth.currentUser?.uid ?: return
        val docs = db.collection("chats").document(userId).collection("messages").get().await()
        docs.documents.forEach { it.reference.delete() }
        // Sau khi xóa xong, gửi lại lời chào tự động
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
            val welcomeText = "Chào bạn! Tôi là Trợ lý AI của EasyShop. Tôi có thể giúp gì cho bạn hôm nay?"
            db.collection("chats").document(userId).collection("messages").add(
                hashMapOf(
                    "content" to welcomeText,
                    "isUser" to false, // Tin nhắn của AI
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("chats").document(userId).update("lastActivity", FieldValue.serverTimestamp())
        }
    }
}
