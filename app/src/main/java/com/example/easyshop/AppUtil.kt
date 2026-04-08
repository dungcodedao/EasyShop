package com.example.easyshop

import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import com.example.easyshop.components.NotifBannerController
import androidx.appcompat.app.AlertDialog
import com.example.easyshop.model.OrderModel
import com.example.easyshop.model.ProductModel
import com.example.easyshop.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object AppUtil {
    private const val PREFS_NAME = "easyshop_prefs"
    private const val FAVORITES_KEY = "favorite_ids"

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

    fun showToast(context: Context, message: String) {
        // Khôi phục lại NotifBanner thay vì Toast truyền thống
        NotifBannerController.show("Thông báo", message)
    }

    fun showSuccess(title: String, message: String? = "") {
        NotifBannerController.show(title, message ?: "", "SUCCESS")
    }

    fun showError(title: String, message: String? = "", detail: String? = null) {
        val finalMessage = if (detail.isNullOrBlank()) (message ?: "") else "${message ?: ""}: $detail"
        NotifBannerController.show(title, finalMessage, "ERROR")
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
}
