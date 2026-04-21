package com.example.easyshop.services

import android.util.Log
import com.example.easyshop.EasyShopApplication
import com.example.easyshop.R
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

/**
 * Tiện ích gửi thông báo FCM theo chuẩn HTTP v1 mới nhất.
 * Sử dụng Service Account JSON để xác thực OAuth2.
 */
object FcmSender {

    private const val TAG = "FcmSender"

    // Project ID từ Firebase Console
    private const val PROJECT_ID = "easyshop-29d03"
    private const val FCM_V1_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"

    // Bộ nhớ đệm cho Access Token để tránh việc tạo mới liên tục
    private var cachedToken: String? = null
    private var tokenExpireTime: Long = 0

    /**
     * Tạo OAuth2 Access Token từ file Service Account JSON (res/raw/fcm_service_account.json)
     */
    private fun getAccessToken(): String? {
        try {
            // Kiểm tra cache (token thường có hiệu lực 60 phút)
            if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime) {
                return cachedToken
            }

            val context = EasyShopApplication.instance
            val stream = context.resources.openRawResource(R.raw.fcm_service_account)
            
            val credentials = GoogleCredentials.fromStream(stream)
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"))
            
            val token = credentials.refreshAccessToken()
            cachedToken = token.tokenValue
            // Lưu thời gian hết hạn (trừ hao 5 phút)
            tokenExpireTime = System.currentTimeMillis() + (token.expirationTime.time - System.currentTimeMillis()) - 300000
            
            return cachedToken
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy Access Token: ${e.message}")
            return null
        }
    }

    /**
     * Gửi thông báo tới một user cụ thể dựa trên userId.
     */
    fun sendToUser(userId: String, title: String, body: String, type: String = "SYSTEM", excludeUserId: String? = null) {
        if (userId == excludeUserId) return // Không gửi cho chính mình
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(userId).get().await()
                val token = userDoc.getString("fcmToken")

                if (token.isNullOrBlank()) {
                    Log.w(TAG, "User $userId chưa có FCM Token, bỏ qua gửi push.")
                    return@launch
                }

                sendToToken(token, title, body, type)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi gửi FCM tới user $userId: ${e.message}")
            }
        }
    }

    /**
     * Gửi thông báo broadcast tới TẤT CẢ user.
     */
    fun sendToAllUsers(title: String, body: String, type: String = "PROMO") {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val usersSnapshot = db.collection("users").get().await()

                var sentCount = 0
                for (doc in usersSnapshot.documents) {
                    val token = doc.getString("fcmToken")
                    if (!token.isNullOrBlank()) {
                        sendToToken(token, title, body, type)
                        sentCount++
                    }
                }
                Log.d(TAG, "Đã gửi FCM broadcast tới $sentCount user.")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi gửi FCM broadcast: ${e.message}")
            }
        }
    }

    /**
     * Gửi thông báo tới TẤT CẢ Admin.
     */
    fun sendToAdmins(title: String, body: String, type: String = "SYSTEM", excludeUserId: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val adminsSnapshot = db.collection("users")
                    .whereEqualTo("role", "admin")
                    .get().await()

                var sentCount = 0
                for (doc in adminsSnapshot.documents) {
                    val adminId = doc.id
                    if (adminId == excludeUserId) continue // Bỏ qua người gửi nếu là admin

                    val token = doc.getString("fcmToken")
                    if (!token.isNullOrBlank()) {
                        sendToToken(token, title, body, type)
                        sentCount++
                    }
                }
                Log.d(TAG, "Đã gửi FCM tới $sentCount Admin.")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi gửi FCM tới Admin: ${e.message}")
            }
        }
    }

    /**
     * Gửi HTTP POST tới FCM API v1.
     */
    private fun sendToToken(token: String, title: String, body: String, type: String) {
        try {
            val accessToken = getAccessToken()
            if (accessToken == null) {
                Log.e(TAG, "Không thể gửi Push vì không lấy được Access Token.")
                return
            }

            val url = URL(FCM_V1_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; UTF-8")
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.doOutput = true

            // Payload FCM v1: Phải bọc trong object "message"
            val message = JSONObject().apply {
                put("token", token)
                
                // Phần hiển thị trên thanh thông báo
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                })
                
                // Phần dữ liệu đi kèm để app xử lý
                put("data", JSONObject().apply {
                    put("type", type)
                    put("title", title)
                    put("body", body)
                })

                // Cấu hình riêng cho Android (để có âm thanh, v.v...)
                put("android", JSONObject().apply {
                    put("priority", "high")
                    put("notification", JSONObject().apply {
                        put("sound", "default")
                        put("click_action", "OPEN_NOTIF")
                    })
                })
            }

            val root = JSONObject().apply {
                put("message", message)
            }

            val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
            writer.write(root.toString())
            writer.flush()
            writer.close()

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                Log.d(TAG, "FCM v1 gửi thành công tới token: ${token.take(15)}...")
            } else {
                val errorStream = conn.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "FCM v1 lỗi $responseCode: $errorStream")
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "HTTP FCM v1 error: ${e.message}")
        }
    }
}
