package com.example.easyshop.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageSaver {
    /**
     * Tải ảnh từ URL và lưu vào Gallery (Thư viện ảnh) của máy.
     * Sử dụng MediaStore để tương thích với Android 10+ (Scoped Storage).
     */
    suspend fun saveQrToGallery(context: Context, imageUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Tải ảnh từ URL (ưu tiên lấy từ Cache của Coil)
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // Bắt buộc để truy xuất bitmap
                    .build()
                
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap ?: return@withContext false

                // 2. Cấu hình thông tin tệp tin
                val fileName = "EasyShop_QR_${System.currentTimeMillis()}.png"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    
                    // Lưu vào thư mục Pictures/EasyShop
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EasyShop")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                // 3. Thực hiện lưu vào MediaStore
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let { imageUri ->
                    resolver.openOutputStream(imageUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    
                    // Hoàn tất quá trình ghi dữ liệu (với Android 10+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)
                    }
                    true
                } ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
