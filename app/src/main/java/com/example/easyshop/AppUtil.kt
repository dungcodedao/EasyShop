package com.example.easyshop

import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.example.easyshop.R
import com.example.easyshop.components.NotifBannerController
import com.example.easyshop.model.OrderModel
import com.example.easyshop.model.ProductModel
import com.example.easyshop.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object AppUtil {
    private const val PREFS_NAME = "easyshop_prefs"
    private const val FAVORITES_KEY = "favorite_ids"
    private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"

    fun appContext(): Context = FirebaseAuth.getInstance().app.applicationContext

    fun getString(resId: Int, vararg formatArgs: Any): String {
        return appContext().getString(resId, *formatArgs)
    }

    private fun getFavoriteIds(context: Context): MutableSet<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(FAVORITES_KEY, emptySet())
            ?.toMutableSet()
            ?: mutableSetOf()
    }

    private fun saveFavoriteIds(context: Context, ids: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(FAVORITES_KEY, ids)
            .apply()
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(ONBOARDING_COMPLETED_KEY, false)
    }

    fun setOnboardingCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ONBOARDING_COMPLETED_KEY, true)
            .apply()
    }

    fun showToast(context: Context, message: String) {
        // Tự động dịch nếu là lỗi hệ thống (Firebase, SePay, v.v.)
        val translatedMessage = translateSystemError(message)
        NotifBannerController.show(getString(R.string.notification_title), translatedMessage)
    }

    fun showSuccess(title: String, message: String? = "") {
        val translatedMessage = translateSystemError(message)
        NotifBannerController.show(title, translatedMessage, "SUCCESS")
    }

    fun showError(title: String, message: String? = "", detail: String? = null) {
        val translatedMessage = translateSystemError(message)
        val translatedDetail = translateSystemError(detail)
        val finalMessage = if (translatedDetail.isBlank()) translatedMessage else "$translatedMessage: $translatedDetail"
        NotifBannerController.show(title, finalMessage, "ERROR")
    }

    internal fun translateSystemError(message: String?): String {
        if (message == null || message.isBlank()) return ""
        val msgLower = message.lowercase()
        return when {
            // Auth Errors
            msgLower.contains("auth credential is incorrect") || msgLower.contains("invalid-credential") -> getString(R.string.error_auth_invalid_credential)
            msgLower.contains("no user record") || msgLower.contains("user-not-found") -> getString(R.string.error_auth_user_not_found)
            msgLower.contains("password is invalid") || msgLower.contains("wrong-password") -> getString(R.string.error_auth_wrong_password)
            msgLower.contains("email address is already in use") || msgLower.contains("email-already-in-use") -> getString(R.string.error_auth_email_in_use)
            msgLower.contains("email address is badly formatted") || msgLower.contains("invalid-email") -> getString(R.string.error_auth_invalid_email)
            msgLower.contains("too many unsuccessful login attempts") -> getString(R.string.error_auth_too_many_attempts)
            msgLower.contains("user-disabled") -> getString(R.string.error_auth_user_disabled)
            msgLower.contains("operation-not-allowed") -> getString(R.string.error_auth_op_not_allowed)
            
            // SePay & Payment Errors
            msgLower.contains("unauthenticated") -> getString(R.string.error_payment_unauthenticated)
            msgLower.contains("account not found") || msgLower.contains("merchant") -> getString(R.string.error_payment_config)
            msgLower.contains("insufficient balance") -> getString(R.string.error_payment_insufficient_balance)
            msgLower.contains("invalid amount") -> getString(R.string.error_payment_invalid_amount)
            msgLower.contains("payment reference") -> getString(R.string.error_payment_invalid_ref)
            
            // Common Errors
            msgLower.contains("network error") || msgLower.contains("network-request-failed") || msgLower.contains("timeout") -> getString(R.string.error_common_network)
            msgLower.contains("internal error") || msgLower.contains("500") -> getString(R.string.error_common_server)
            msgLower.contains("expired") -> getString(R.string.error_common_expired)
            msgLower.contains("permission-denied") -> getString(R.string.error_common_permission)
            msgLower.contains("unavailable") -> getString(R.string.error_common_unavailable)
            
            else -> message
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun checkNetworkAndNotify(context: Context): Boolean {
        if (!isNetworkAvailable(context)) {
            showToast(context, context.getString(R.string.no_internet_message))
            return false
        }
        return true
    }

    fun formatCurrency(amount: Double): String {
        return getString(R.string.currency_symbol) + String.format("%,.0f", amount)
    }

    /**
     * Hỗ trợ định dạng giá từ nhiều kiểu dữ liệu khác nhau (String, Float, Double)
     */
    fun formatPrice(price: Any?): String {
        val amount = when (price) {
            is String -> price.replace(",", "").replace("đ", "").trim().toDoubleOrNull() ?: 0.0
            is Float -> price.toDouble()
            is Double -> price
            is Int -> price.toDouble()
            is Long -> price.toDouble()
            else -> 0.0
        }
        return formatCurrency(amount)
    }

    fun formatDate(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }

    fun formatDate(millis: Long): String {
        if (millis <= 0L) return ""
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }

    fun formatData(timestamp: Timestamp?): String = formatDate(timestamp)

    fun checkFavorite(context: Context, productId: String): Boolean {
        return getFavoriteIds(context).contains(productId)
    }

    fun addOrRemoveFromFavorite(context: Context, product: ProductModel) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            showToast(context, getString(R.string.app_util_login_required_fav))
            return
        }

        val productId = product.id
        val db = Firebase.firestore
        val favoritesRef = db.collection("users").document(uid).collection("favorites")
        
        // ✅ 1. Lấy danh sách hiện tại
        val favoriteIds = getFavoriteIds(context).toMutableSet()
        val isRemoving = favoriteIds.contains(productId)

        // ✅ 2. CẬP NHẬT TỨC THÌ (Optimistic UI)
        if (isRemoving) {
            favoriteIds.remove(productId)
            showSuccess(getString(R.string.app_util_fav_remove_title), getString(R.string.app_util_fav_remove_msg))
        } else {
            favoriteIds.add(productId)
            showSuccess(getString(R.string.app_util_fav_add_title), getString(R.string.app_util_fav_add_msg))
        }
        saveFavoriteIds(context, favoriteIds)

        // ✅ 3. Xử lý Firebase ngầm bên dưới
        if (isRemoving) {
            favoritesRef.document(productId).delete()
                .addOnFailureListener {
                    favoriteIds.add(productId)
                    saveFavoriteIds(context, favoriteIds)
                    showError(getString(R.string.app_util_sync_error_title), getString(R.string.app_util_sync_error_remove))
                }
        } else {
            favoritesRef.document(productId).set(product)
                .addOnFailureListener {
                    favoriteIds.remove(productId)
                    saveFavoriteIds(context, favoriteIds)
                    showError(getString(R.string.app_util_sync_error_title), getString(R.string.app_util_sync_error_add))
                }
        }
    }

    fun generateOrderNumber(): String {
        return "ES-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
    }

    fun addItemToCart(context: Context, productId: String, quantity: Int = 1) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            showToast(context, getString(R.string.app_util_login_required_cart))
            return
        }
        if (productId.isBlank()) return
        
        val db = Firebase.firestore
        val userRef = db.collection("users").document(uid)
        
        // Cách tiếp cận an toàn nhất: Thử update, nếu lỗi (do doc/field thiếu) thì dùng set merge
        userRef.update("cartItems.$productId", FieldValue.increment(quantity.toLong()))
            .addOnSuccessListener { showSuccess(getString(R.string.app_util_cart_add_success)) }
            .addOnFailureListener {
                // Nếu update thất bại, ta dùng set merge để khởi tạo cấu trúc map
                val data = mapOf("cartItems" to mapOf(productId to quantity.toLong()))
                userRef.set(data, SetOptions.merge())
                    .addOnSuccessListener { showSuccess(getString(R.string.app_util_cart_add_success)) }
                    .addOnFailureListener { e -> 
                        showError(getString(R.string.app_util_cart_add_failed))
                    }
            }
    }

    fun incrementCartItem(productId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .update("cartItems.$productId", FieldValue.increment(1))
    }

    fun decrementCartItem(productId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore
        val userRef = db.collection("users").document(uid)
        
        db.runTransaction { transaction ->
            val userDoc = transaction.get(userRef)
            @Suppress("UNCHECKED_CAST")
            val cartItems = userDoc.get("cartItems") as? Map<String, Long> ?: emptyMap()
            val currentQty = cartItems[productId] ?: 0L
            
            if (currentQty <= 1L) {
                transaction.update(userRef, "cartItems.$productId", FieldValue.delete())
            } else {
                transaction.update(userRef, "cartItems.$productId", FieldValue.increment(-1))
            }
        }.addOnFailureListener { e ->
             showError(getString(R.string.app_util_cart_update_error), e.message)
        }
    }

    fun removeItemFromCart(context: Context, productId: String, removeAll: Boolean = false) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore
        
        if (removeAll) {
            db.collection("users").document(uid)
                .update("cartItems.$productId", FieldValue.delete())
                .addOnSuccessListener {
                    showToast(context, getString(R.string.app_util_cart_removed))
                }
        } else {
            decrementCartItem(productId)
        }
    }

    fun placeOrder(
        context: Context,
        order: OrderModel,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!checkNetworkAndNotify(context)) {
            onFailure(getString(R.string.app_util_no_network))
            return
        }

        val db = Firebase.firestore
        val ordersRef = db.collection("orders")
        
        ordersRef.document(order.id).set(order)
            .addOnSuccessListener {
                updateStockAfterOrder(order)
                clearCartInFirestore(order.userId)
                
                // Gửi thông báo tới các Admin
                com.example.easyshop.services.FcmSender.sendToAdmins(
                    title = getString(R.string.app_util_order_notif_title),
                    body = getString(R.string.app_util_order_notif_body, order.id.take(8).uppercase(), order.userName),
                    type = "NEW_ORDER"
                )
                
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: getString(R.string.app_util_order_error))
            }
    }

    private fun updateStockAfterOrder(order: OrderModel) {
        val db = Firebase.firestore
        order.items.forEach { (productId, quantity) ->
            val productRef = db.collection("data").document("stock").collection("products").document(productId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(productRef)
                val currentStock = snapshot.getLong("stockCount") ?: 0L
                val newStock = (currentStock - quantity).coerceAtLeast(0)
                
                transaction.update(productRef, "stockCount", newStock)
                
                // Cập nhật trạng thái inStock nếu hết hàng
                if (newStock <= 0) {
                    transaction.update(productRef, "inStock", false)
                }
            }
        }
    }

    fun clearCartAndAddToOrder(
        context: Context,
        total: Double,
        subtotal: Double,
        discount: Double,
        promoCode: String,
        paymentMethod: String,
        orderId: String
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore
        
        // 1. Lấy thông tin user để điền vào đơn hàng
        db.collection("users").document(uid).get().addOnSuccessListener { snapshot ->
            val user = snapshot.toObject(UserModel::class.java) ?: return@addOnSuccessListener
            
            // 2. Tạo đối tượng Order
            val order = OrderModel(
                id = orderId,
                userId = uid,
                userName = user.name,
                userEmail = user.email,
                userPhone = user.phone,
                items = user.cartItems,
                subtotal = subtotal,
                discount = discount,
                promoCode = promoCode,
                total = total,
                status = "ORDERED",
                address = user.addressList.find { it.isDefault }?.detailedAddress ?: user.addressList.firstOrNull()?.detailedAddress ?: "",
                paymentMethod = paymentMethod
            )

            // 3. Đặt hàng
            placeOrder(context, order, {
                // Success handled by page
            }, { 
                showToast(context, getString(R.string.app_util_order_error_detail, it))
            })
        }
    }

    fun clearCartInFirestore(userId: String) {
        val db = Firebase.firestore
        db.collection("users").document(userId)
            .update("cartItems", emptyMap<String, Long>())
    }

    fun getOrderHistory(
        userId: String,
        onSuccess: (List<OrderModel>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val db = Firebase.firestore
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val orders = result.documents.mapNotNull { it.toObject(OrderModel::class.java) }
                onSuccess(orders)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: getString(R.string.app_util_history_error))
            }
    }

    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val db = Firebase.firestore
        db.collection("orders").document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: getString(R.string.app_util_update_failed)) }
    }

    fun startMockPayment(
        context: Context,
        amount: Float,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.app_util_demo_payment_title))
            .setMessage(
                getString(R.string.app_util_demo_payment_msg, formatCurrency(amount.toDouble()))
            )
            .setPositiveButton(getString(R.string.app_util_demo_payment_success)) { dialog: DialogInterface, _: Int ->
                if (!checkNetworkAndNotify(context)) {
                    dialog.dismiss()
                    return@setPositiveButton
                }

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onSuccess()
                }, 1500)
            }
            .setNegativeButton(getString(R.string.app_util_demo_payment_failed)) { _: DialogInterface, _: Int ->
                onFailure()
            }
            .setNeutralButton(getString(R.string.app_util_demo_payment_cancel), null)
            .setCancelable(true)
            .show()
    }

    /**
     * Lưu FCM Token vào SharedPreferences và Firestore
     */
    fun saveFcmToken(token: String) {
        if (token.isBlank()) return
        
        // 1. Lưu local (để có thể lấy lại khi user đăng nhập sau này)
        val context = appContext()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
            
        // 2. Nếu đã đăng nhập, lưu đồng bộ lên Firestore
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = Firebase.firestore
            db.collection("users").document(uid)
                .update("fcmToken", token)
                .addOnFailureListener {
                    // Nếu field chưa có hoặc doc chưa có, dùng merge
                    db.collection("users").document(uid)
                        .set(mapOf("fcmToken" to token), SetOptions.merge())
                }
        }
    }

    /**
     * Đồng bộ Token hiện tại lên Firestore (thường gọi sau khi Login/Signup)
     */
    fun syncFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                saveFcmToken(token)
            }
    }

    /**
     * Parse price string sang Double một cách an toàn.
     * Xử lý định dạng Việt Nam: "80.000đ" -> 80000.0
     * Xử lý dấu phẩy ngàn: "1,500,000" -> 1500000.0
     * Xử lý thập phân: "15.50" -> 15.50
     */
    fun parsePrice(priceStr: String): Double {
        // Loại bỏ ký tự tiền tệ và khoảng trắng
        val cleaned = priceStr.replace(Regex("[đ₫$€£¥\\s]"), "").trim()
        if (cleaned.isEmpty()) return 0.0

        // Định dạng Việt Nam: dấu chấm ngăn hàng nghìn (vd: "80.000" hoặc "1.500.000")
        val vnPattern = Regex("^\\d{1,3}(\\.\\d{3})+$")
        if (vnPattern.matches(cleaned)) {
            return cleaned.replace(".", "").toDoubleOrNull() ?: 0.0
        }

        // Dấu phẩy ngăn hàng nghìn (vd: "1,000,000" hoặc "1,500.50")
        val commaThousandsPattern = Regex("^\\d{1,3}(,\\d{3})+(\\.\\d+)?$")
        if (commaThousandsPattern.matches(cleaned)) {
            return cleaned.replace(",", "").toDoubleOrNull() ?: 0.0
        }

        // Định dạng số thông thường (vd: "15.50", "1000")
        return cleaned.replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    /**
     * Resize bitmap để vừa với maxSize mà giữ nguyên tỷ lệ.
     * Giúp tránh OOM khi xử lý ảnh lớn từ camera/gallery.
     *
     * @param bitmap Ảnh gốc
     * @param maxSize Kích thước cạnh lớn nhất cho phép (px), mặc định 1024
     * @return Bitmap đã resize (hoặc gốc nếu đã nhỏ hơn maxSize)
     */
    fun resizeBitmap(bitmap: Bitmap, maxSize: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Sử dụng UTF-8 BOM để Excel hiển thị đúng tiếng Việt.
     */
    private fun escapeCSV(value: String?): String {
        val str = value ?: ""
        // Thay thế dấu " bằng "" và bao quanh toàn bộ bằng dấu "
        return "\"" + str.replace("\"", "\"\"") + "\""
    }

    private fun formatCurrencyCSV(amount: Double): String {
        val formatter = java.text.NumberFormat.getInstance(java.util.Locale("vi", "VN"))
        return "${formatter.format(amount)} " + getString(R.string.currency_suffix_dong)
    }

    /**
     * Thay thế cấu trúc để thêm đơn giá và thành tiền, giúp báo cáo minh bạch hơn.
     */
    fun exportOrdersToCSV(
        context: Context, 
        orders: List<OrderModel>, 
        productNames: Map<String, String> = emptyMap(),
        productPrices: Map<String, Double> = emptyMap()
    ) {
        try {
            val fileName = "EasyShop_Orders_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(java.util.Date())}.csv"
            
            // 1. Chuẩn bị dữ liệu CSV thành String
            val csvBuilder = StringBuilder()
            // Viết UTF-8 BOM để Excel nhận diện đúng font tiếng Việt
            csvBuilder.append("\uFEFF")
            
            // Header
            csvBuilder.append(getString(R.string.csv_header_orders) + "\n")
            
            // Data rows 
            orders.forEach { order ->
                val dateStr = formatDate(order.date)
                order.items.forEach { (productId, qty) ->
                    val productName = productNames[productId] ?: productId
                    val unitPrice = productPrices[productId] ?: 0.0
                    val itemTotal = unitPrice * qty
                    
                    val row = listOf(
                        escapeCSV(order.id),
                        escapeCSV(dateStr),
                        escapeCSV(order.userName),
                        escapeCSV(productName),
                        qty.toString(),
                        escapeCSV(formatCurrencyCSV(unitPrice)),
                        escapeCSV(formatCurrencyCSV(itemTotal)),
                        escapeCSV(formatCurrencyCSV(order.total)),
                        escapeCSV(order.status),
                        escapeCSV(order.userEmail),
                        escapeCSV(order.userPhone)
                    ).joinToString(",")
                    csvBuilder.append("$row\n")
                }
            }

            // 2. Lưu file vào máy
            val contentResolver = context.contentResolver
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Cách lưu cho Android 10+ (Sử dụng MediaStore)
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EasyShop_Reports")
                }

                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
                    }
                    
                    // Thêm logic tự động mở file sau khi lưu (Android 10+)
                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "text/csv")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(openIntent)

                    showSuccess(getString(R.string.app_util_export_success_title), getString(R.string.app_util_export_success_msg, "Bộ nhớ máy/Download/EasyShop_Reports"))
                } else {
                    throw Exception(getString(R.string.error_mediastore))
                }
            } else {
                // Cách lưu cho các bản Android cũ hơn (Sử dụng File API truyền thống)
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val reportDir = File(downloadDir, "EasyShop_Reports")
                if (!reportDir.exists()) reportDir.mkdirs()
                
                val file = File(reportDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
                }
                
                // Thêm logic tự động mở file sau khi lưu (Android cũ)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "text/csv")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(openIntent)

                showSuccess(getString(R.string.app_util_export_success_title), getString(R.string.app_util_export_success_msg, "Bộ nhớ máy/Download/EasyShop_Reports"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            showError(getString(R.string.app_util_export_error_title), e.message)
        }
    }

    fun restoreStock(items: Map<String, Long>) {
        if (items.isEmpty()) return
        val db = Firebase.firestore
        val batch = db.batch()
        
        items.forEach { (productId, quantity) ->
            val productRef = db.collection("data").document("stock")
                .collection("products").document(productId)
            
            // Tăng stockCount và đảm bảo inStock là true
            batch.update(productRef, "stockCount", FieldValue.increment(quantity))
            batch.update(productRef, "inStock", true)
        }
        
        batch.commit()
            .addOnSuccessListener {
                android.util.Log.d("EasyShop_Stock", "--- Hoàn kho thành công cho ${items.size} mặt hàng ---")
                items.forEach { (pid, q) -> 
                    android.util.Log.d("EasyShop_Stock", "ID SP: $pid | Đã cộng lại: $q")
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("EasyShop_Stock", "Lỗi hoàn kho: ${e.message}")
            }
    }
}
