package com.example.easyshop.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.easyshop.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Upload ảnh lên Cloudinary từ URI local.
 * Dùng chung cho cả User chat và Admin chat.
 */
object CloudinaryUploader {

    private val TAG = "CloudinaryUploader"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
    private val uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET

    /**
     * Upload ảnh từ URI lên Cloudinary, trả về URL ảnh hoặc null nếu lỗi.
     */
    suspend fun uploadFromUri(context: Context, uri: Uri, folder: String = "chat_images"): String? =
        withContext(Dispatchers.IO) {
            if (cloudName.isBlank()) {
                Log.e(TAG, "Cloudinary Cloud Name chưa được cấu hình")
                return@withContext null
            }

            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                    ?: throw Exception("Không đọc được dữ liệu ảnh từ URI")
                inputStream.close()

                val url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", "chat_image.jpg",
                        bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    .addFormDataPart("upload_preset", uploadPreset)
                    .addFormDataPart("folder", folder)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Cloudinary Error: ${response.code} - $body")
                        return@withContext null
                    }
                    val json = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                    val secureUrl = json.get("secure_url")?.asString
                    Log.d(TAG, "Upload thành công: $secureUrl")
                    secureUrl
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload ảnh lỗi: ${e.message}", e)
                null
            }
        }
}
