package com.example.easyshop

import android.app.AlertDialog
import android.content.Context
import androidx.core.content.edit
import com.example.easyshop.components.SnackbarController
import com.example.easyshop.model.OrderModel
import com.example.easyshop.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object AppUtil {
    fun showToast(context: Context, message: String) {
        SnackbarController.info(message)
    }

    fun showSuccess(message: String, subtext: String? = null) = SnackbarController.success(message, subtext)
    fun showError(message: String, subtext: String? = null) = SnackbarController.error(message, subtext)
    fun showInfo(message: String, subtext: String? = null) = SnackbarController.info(message, subtext)
    fun showWarning(message: String, subtext: String? = null) = SnackbarController.warning(message, subtext)

    fun addItemToCart(context: Context, productId :String){
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userDoc = Firebase.firestore.collection("users")
            .document(currentUser.uid)
        userDoc.get().addOnCompleteListener(){
            if(it.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                val currentCart = it.result.get("cartItems") as? Map<String, Long> ?: emptyMap()
                val currentQuantity = currentCart[productId]?:0
                val updateQuantity = currentQuantity + 1;

                val updateCart = mapOf("cartItems.$productId" to updateQuantity)

                userDoc.update(updateCart)
                    .addOnCompleteListener {
                        if(it.isSuccessful) {
                            showSuccess("Đã thêm vào giỏ hàng", "Sản phẩm đã sẵn sàng thanh toán")
                        } else {
                            showError("Thất bại", "Không thể thêm vào giỏ hàng")
                        }
                    }
            }
        }
    }

    fun removeItemFromCart(context: Context, productId :String, removeAll : Boolean = false){
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userDoc = Firebase.firestore.collection("users")
            .document(currentUser.uid)
        userDoc.get().addOnCompleteListener(){
            if(it.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                val currentCart = it.result.get("cartItems") as? Map<String, Long> ?: emptyMap()
                val currentQuantity = currentCart[productId]?:0
                val updateQuantity = currentQuantity - 1;

                val updateCart =
                    if (updateQuantity <= 0 || removeAll )
                        mapOf("cartItems.$productId" to FieldValue.delete())
                    else
                        mapOf("cartItems.$productId" to updateQuantity)

                userDoc.update(updateCart)
                    .addOnCompleteListener {
                        if(it.isSuccessful) {
                            showToast(context, "Đã xóa khỏi giỏ hàng")
                        } else {
                            showToast(context, "Không thể xóa khỏi giỏ hàng")
                        }
                    }
            }
        }
    }

    fun clearCartAndAddToOrder(
        totalAmount: Double = 0.0,
        subtotal: Double = 0.0,
        discount: Double = 0.0,
        promoCode: String = "",
        paymentMethod: String = "COD",
        customOrderId: String? = null
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userDoc = Firebase.firestore.collection("users").document(currentUser.uid)

        userDoc.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val user = document.toObject(UserModel::class.java)
                val cartItems = user?.cartItems ?: emptyMap()

                if (cartItems.isEmpty()) return@addOnSuccessListener

                val orderId = customOrderId ?: ("ORD" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase())

                val order = OrderModel(
                    id = orderId,
                    userId = currentUser.uid,
                    userName = user?.name ?: "Khách hàng",
                    userEmail = user?.email ?: "N/A",
                    userPhone = user?.phone ?: "",
                    date = Timestamp.now(),
                    items = cartItems,
                    subtotal = subtotal,
                    discount = discount,
                    promoCode = promoCode,
                    total = totalAmount,
                    status = "ORDERED",
                    address = user?.address ?: "Chưa có địa chỉ",
                    paymentMethod = paymentMethod
                )

                Firebase.firestore.collection("orders")
                    .document(orderId)
                    .set(order)
                    .addOnSuccessListener {
                        userDoc.update("cartItems", FieldValue.delete())

                        val db = Firebase.firestore
                        val itemCount = cartItems.values.sum()

                        // ✅ 1. Thông báo cho USER: đặt hàng thành công
                        val userNotif = hashMapOf(
                            "userId" to currentUser.uid,
                            "title" to "Đặt hàng thành công ✅",
                            "body" to "Đơn #${orderId.take(8).uppercase()} ($itemCount sản phẩm) đã được ghi nhận. Phương thức: $paymentMethod.",
                            "type" to "ORDER_STATUS",
                            "orderId" to orderId,
                            "isRead" to false,
                            "recipientRole" to "user",
                            "createdAt" to Timestamp.now()
                        )
                        db.collection("notifications").add(userNotif)

                        // 🔔 2. Thông báo cho ADMIN: có đơn mới
                        val adminNotif = hashMapOf(
                            "userId" to "admin",
                            "title" to "Đơn hàng mới #${orderId.take(8).uppercase()}",
                            "body" to "Khách ${user?.name ?: "Ẩn danh"} vừa đặt $itemCount sản phẩm · ${formatCurrency(totalAmount)}",
                            "type" to "NEW_ORDER",
                            "orderId" to orderId,
                            "isRead" to false,
                            "recipientRole" to "admin",
                            "createdAt" to Timestamp.now()
                        )
                        db.collection("notifications").add(adminNotif)
                    }
            }
        }
    }



    private fun showSuccessDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("✅ Thanh toán thành công")
            .setMessage("Đơn hàng của bạn đã được đặt thành công!")
            .setPositiveButton("OK") { _, _ ->
                GlobalNavigation.navController.navigate("home") {
                    popUpTo("checkout") { inclusive = true }
                }
            }
            .setCancelable(false)
            .show()
    }


    fun formatData(timestamp: Timestamp) : String{
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }

    fun formatPrice(price: Any): String {
        return try {
            val p = when (price) {
                is String -> price.replace(",", "").toDoubleOrNull() ?: 0.0
                is Number -> price.toDouble()
                else -> 0.0
            }
            val formatter = java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
            val formatted = formatter.format(p)
            formatted.replace("₫", "đ").replace("VND", "đ").trim()
        } catch (e: Exception) {
            price.toString() + "đ"
        }
    }

    fun formatCurrency(amount: Double): String = formatPrice(amount)

    private const val PREF_NAME = "favorite_pref"
    private const val KEY_FAVORITES = "favorites_list"

    fun addOrRemoveFromFavorite(context: Context, productId: String){
        val list = getFavoriteList(context).toMutableSet()
        if(list.contains(productId)){
            list.remove(productId)
            showWarning("Đã bỏ thích", "Sản phẩm đã được gỡ khỏi danh sách")
        } else {
            list.add(productId)
            showSuccess("Đã thêm yêu thích", "Bạn có thể xem lại trong mục Yêu thích")
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit{
            putStringSet(KEY_FAVORITES, list)
        }


    }

    fun checkFavorite(context: Context, productId: String) :Boolean{
        if(getFavoriteList(context).contains(productId)){
            return true
        }
        return false
    }

    fun getFavoriteList(context: Context) :Set<String>{

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FAVORITES, emptySet())?: emptySet()

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
                        "Số tiền: đ${"%.2f".format(amount)}\n\n" +
                        "Chọn kết quả:"
            )
            .setPositiveButton("✅ Thành công") { _, _ ->
                // Delay để giống thật
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onSuccess()
                }, 1500)
            }
            .setNegativeButton("❌ Thất bại") { _, _ ->
                onFailure()
            }
            .setNeutralButton("Hủy", null)
            .setCancelable(true)
            .show()
    }

}