package com.example.easyshop.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

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

    private val storage = Firebase.storage

    /**
     * Upload ảnh lên Firebase Storage.
     * @param uri - Uri của ảnh local trên thiết bị
     * @param folder - Thư mục lưu trữ trên Storage (mặc định: "products")
     * @return Flow<Result<String>> - emit URL tải về khi thành công, hoặc exception khi lỗi
     */
    fun uploadProductImage(uri: Uri, folder: String = "products"): Flow<Result<String>> = callbackFlow {
        val filename = "${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("$folder/$filename")

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                trySend(Result.success(downloadUri.toString()))
                close()
            }
            .addOnFailureListener { exception ->
                trySend(Result.failure(exception))
                close()
            }

        awaitClose()
    }
}
