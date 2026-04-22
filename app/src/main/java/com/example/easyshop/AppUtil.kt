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

    private fun appContext(): Context = FirebaseAuth.getInstance().app.applicationContext

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
        NotifBannerController.show("Thông báo", translatedMessage)
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

    private fun translateSystemError(message: String?): String {
        if (message == null || message.isBlank()) return ""
        val msgLower = message.lowercase()
        return when {
            // Auth Errors
            msgLower.contains("auth credential is incorrect") || msgLower.contains("invalid-credential") -> "Email hoặc mật khẩu không chính xác"
            msgLower.contains("no user record") || msgLower.contains("user-not-found") -> "Tài khoản này không tồn tại"
            msgLower.contains("password is invalid") || msgLower.contains("wrong-password") -> "Mật khẩu không chính xác"
            msgLower.contains("email address is already in use") || msgLower.contains("email-already-in-use") -> "Email này đã được sử dụng"
            msgLower.contains("email address is badly formatted") || msgLower.contains("invalid-email") -> "Định dạng email không hợp lệ"
            msgLower.contains("too many unsuccessful login attempts") -> "Thử đăng nhập sai quá nhiều lần. Vui lòng quay lại sau"
            msgLower.contains("user-disabled") -> "Tài khoản của bạn đã bị khóa"
            msgLower.contains("operation-not-allowed") -> "Phương thức đăng nhập này chưa được kích hoạt"
            
            // SePay & Payment Errors
            msgLower.contains("unauthenticated") -> "Phiên làm việc hết hạn. Vui lòng đăng nhập lại"
            msgLower.contains("account not found") || msgLower.contains("merchant") -> "Lỗi cấu hình tài khoản thanh toán"
            msgLower.contains("insufficient balance") -> "Số dư không đủ để thực hiện giao dịch"
            msgLower.contains("invalid amount") -> "Số tiền giao dịch không hợp lệ"
            msgLower.contains("payment reference") -> "Mã tham chiếu thanh toán không hợp lệ"
            
            // Common Errors
            msgLower.contains("network error") || msgLower.contains("network-request-failed") || msgLower.contains("timeout") -> "Lỗi kết nối mạng. Vui lòng kiểm tra lại"
            msgLower.contains("internal error") || msgLower.contains("500") -> "Lỗi hệ thống máy chủ. Vui lòng thử lại sau"
            msgLower.contains("expired") -> "Phiên làm việc đã hết hạn hoặc mã đã hết hiệu lực"
            msgLower.contains("permission-denied") -> "Bạn không có quyền thực hiện hành động này"
            msgLower.contains("unavailable") -> "Dịch vụ hiện không khả dụng. Vui lòng thử lại sau"
            
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
            showToast(context, "Mất kết nối Internet. Vui lòng kiểm tra lại!")
            return false
        }
        return true
    }

    fun formatCurrency(amount: Double): String {
        return "đ" + String.format("%,.0f", amount)
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
            showToast(context, "Vui lòng đăng nhập để dùng yêu thích")
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
            showSuccess("Gỡ yêu thích", "Đã xóa sản phẩm khỏi danh sách")
        } else {
            favoriteIds.add(productId)
            showSuccess("Yêu thích", "Đã thêm vào danh sách yêu thích của bạn")
        }
        saveFavoriteIds(context, favoriteIds)

        // ✅ 3. Xử lý Firebase ngầm bên dưới
        if (isRemoving) {
            favoritesRef.document(productId).delete()
                .addOnFailureListener {
                    // Rollback nếu lỗi
                    favoriteIds.add(productId)
                    saveFavoriteIds(context, favoriteIds)
                    showError("Lỗi đồng bộ", "Không thể xóa yêu thích")
                }
        } else {
            favoritesRef.document(productId).set(product)
                .addOnFailureListener {
                    // Rollback nếu lỗi
                    favoriteIds.remove(productId)
                    saveFavoriteIds(context, favoriteIds)
                    showError("Lỗi đồng bộ", "Không thể thêm yêu thích")
                }
        }
    }

    fun generateOrderNumber(): String {
        return "ES-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
    }

    fun addItemToCart(context: Context, productId: String, quantity: Int = 1) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            showToast(context, "Vui lòng đăng nhập để thêm vào giỏ hàng")
            return
        }
        if (productId.isBlank()) return
        
        val db = Firebase.firestore
        val userRef = db.collection("users").document(uid)
        
        // Cách tiếp cận an toàn nhất: Thử update, nếu lỗi (do doc/field thiếu) thì dùng set merge
        userRef.update("cartItems.$productId", FieldValue.increment(quantity.toLong()))
            .addOnSuccessListener { showSuccess("Đã thêm vào giỏ hàng") }
            .addOnFailureListener {
                // Nếu update thất bại, ta dùng set merge để khởi tạo cấu trúc map
                val data = mapOf("cartItems" to mapOf(productId to quantity.toLong()))
                userRef.set(data, SetOptions.merge())
                    .addOnSuccessListener { showSuccess("Đã thêm vào giỏ hàng") }
                    .addOnFailureListener { e -> 
                        showError("Thêm vào giỏ hàng thất bại")
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
            val snapshot = transaction.get(userRef)
            val cartItems = snapshot.get("cartItems") as? Map<String, Long> ?: emptyMap()
            val currentQty = cartItems[productId] ?: 0L
            
            if (currentQty <= 1L) {
                transaction.update(userRef, "cartItems.$productId", FieldValue.delete())
            } else {
                transaction.update(userRef, "cartItems.$productId", FieldValue.increment(-1))
            }
        }.addOnFailureListener { e ->
             showError("Lỗi cập nhật giỏ hàng", e.message)
        }
    }

    fun removeItemFromCart(context: Context, productId: String, removeAll: Boolean = false) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore
        
        if (removeAll) {
            db.collection("users").document(uid)
                .update("cartItems.$productId", FieldValue.delete())
                .addOnSuccessListener {
                    showToast(context, "Đã xóa khỏi giỏ hàng")
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
            onFailure("Không có kết nối mạng")
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
                    title = "Đơn hàng mới 📦",
                    body = "Đơn #${order.id.take(8).uppercase()} vừa được đặt bởi ${order.userName}",
                    type = "NEW_ORDER"
                )
                
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Có lỗi xảy ra khi đặt hàng")
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
                showToast(context, "Lỗi đặt hàng: $it")
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
                onFailure(e.message ?: "Không thể lấy lịch sử đơn hàng")
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
            .addOnFailureListener { e -> onFailure(e.message ?: "Cập nhật thất bại") }
    }

    fun startMockPayment(
        context: Context,
        amount: Float,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("💳 Thanh toán Demo")
            .setMessage(
                "Thanh toán thử nghiệm\n" +
                        "Số tiền: ${formatCurrency(amount.toDouble())}\n\n" +
                        "Chọn kết quả:"
            )
            .setPositiveButton("✅ Thành công") { dialog: DialogInterface, _: Int ->
                if (!checkNetworkAndNotify(context)) {
                    dialog.dismiss()
                    return@setPositiveButton
                }

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onSuccess()
                }, 1500)
            }
            .setNegativeButton("❌ Thất bại") { _: DialogInterface, _: Int ->
                onFailure()
            }
            .setNeutralButton("Hủy", null)
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
        return "${formatter.format(amount)} đồng"
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
            csvBuilder.append("Mã đơn hàng,Ngày đặt,Khách hàng,Tên Sản phẩm,Số lượng,Đơn giá,Thành tiền,Tổng hóa đơn,Trạng thái,Email,SĐT\n")
            
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

                    showSuccess("Lưu file thành công", "Báo cáo đã được lưu vào: Bộ nhớ máy/Download/EasyShop_Reports")
                } else {
                    throw Exception("Không thể tạo file qua MediaStore")
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

                showSuccess("Lưu file thành công", "Báo cáo đã được lưu tại: Bộ nhớ máy/Download/EasyShop_Reports")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            showError("Lỗi xuất file", e.message)
        }
    }
}
