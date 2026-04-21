package com.example.easyshop.repository

import android.content.Context
import android.net.Uri
import com.example.easyshop.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

class ProductRepository private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: ProductRepository? = null

        fun getInstance(context: Context): ProductRepository {
            return instance ?: synchronized(this) {
                instance ?: ProductRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val cloudinaryCloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
    private val cloudinaryUploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET

    /**
     * Upload ảnh lên Cloudinary thay cho Firebase Storage.
     * @param uri - Uri của ảnh local trên thiết bị
     * @param folder - Folder trên Cloudinary (không bắt buộc vì preset đã cấu hình, nhưng có thể dùng làm prefix)
     * @return Flow<Result<String>> - emit URL tải về khi thành công, hoặc exception khi lỗi
     */
    fun uploadProductImage(uri: Uri, folder: String = "products"): Flow<Result<String>> = callbackFlow {
        if (cloudinaryCloudName.isBlank()) {
            trySend(Result.failure(Exception("Cloudinary Cloud Name is not configured")))
            close()
            return@callbackFlow
        }

        try {
            // 1. Đọc bytes từ URI
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: throw Exception("Cannot read image bytes from URI")
            inputStream.close()

            // 2. Tạo Request gửi tới Cloudinary
            val url = "https://api.cloudinary.com/v1_1/$cloudinaryCloudName/image/upload"
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "product_image.jpg", 
                    bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                .addFormDataPart("upload_preset", cloudinaryUploadPreset)
                .addFormDataPart("folder", folder)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            // 3. Thực thi request trong luồng IO
            httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    trySend(Result.failure(e))
                    close()
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use { resp ->
                        val body = resp.body?.string() ?: ""
                        if (!resp.isSuccessful) {
                            trySend(Result.failure(Exception("Cloudinary Error: ${resp.code} - $body")))
                        } else {
                            val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                            val secureUrl = json.get("secure_url")?.asString
                            if (secureUrl != null) {
                                trySend(Result.success(secureUrl))
                            } else {
                                trySend(Result.failure(Exception("Cloudinary response missing secure_url")))
                            }
                        }
                    }
                    close()
                }
            })
        } catch (e: Exception) {
            trySend(Result.failure(e))
            close()
        }

        awaitClose()
    }
}
