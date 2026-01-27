package com.example.easyshop

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import androidx.core.content.edit
import com.example.easyshop.model.OrderItem
import com.example.easyshop.model.OrderModel
import com.example.easyshop.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.razorpay.Checkout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object AppUtil {
    fun showToast(context : Context, massage : String){
        Toast.makeText(context, massage, Toast.LENGTH_LONG).show()
    }

    fun addItemToCart(context: Context, productId :String){
        val userDoc = Firebase.firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
        userDoc.get().addOnCompleteListener(){
            if(it.isSuccessful) {
                val currentCart = it.result.get("cartItems") as? Map<String, Long> ?: emptyMap()
                val currentQuantity = currentCart[productId]?:0
                val updateQuantity = currentQuantity + 1;

                val updateCart = mapOf("cartItems.$productId" to updateQuantity)

                userDoc.update(updateCart)
                    .addOnCompleteListener {
                        if(it.isSuccessful) {
                            showToast(context, "Item added to cart")
                        } else {
                            showToast(context, "Failed adding item to the cart")
                        }
                    }
            }
        }
    }

    fun removeItemFromCart(context: Context, productId :String, removeAll : Boolean = false){
        val userDoc = Firebase.firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
        userDoc.get().addOnCompleteListener(){
            if(it.isSuccessful) {
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
                            showToast(context, "Item removed to cart")
                        } else {
                            showToast(context, "Failed removing item to the cart")
                        }
                    }
            }
        }
    }

    fun clearCartAndAddToOrder(totalAmount: Double = 0.0, paymentMethod: String = "COD") {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userDoc = Firebase.firestore.collection("users").document(currentUser.uid)

        userDoc.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val user = document.toObject(UserModel::class.java)
                val cartItems = user?.cartItems ?: emptyMap()

                if (cartItems.isEmpty()) return@addOnSuccessListener

                val orderId = "ORD" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase()

                val order = OrderModel(
                    id = orderId,
                    userId = currentUser.uid,
                    userName = user?.name ?: "Customer",
                    userEmail = user?.email ?: "N/A",
                    date = Timestamp.now(),
                    items = cartItems,
                    total = totalAmount,
                    status = "ORDERED",
                    address = user?.address ?: "No Address",
                    paymentMethod = paymentMethod
                )

                Firebase.firestore.collection("orders")
                    .document(orderId)
                    .set(order)
                    .addOnSuccessListener {
                        userDoc.update("cartItems", FieldValue.delete())
                    }
            }
        }
    }

    fun getTaxPercentage() : Float{
        return 13.0f
    }

    //add payment methods
    fun razorpayApiKey() : String {
        return "rzp_test_5WgA34F9ljiXAX"
    }

    fun startPayment(context: Context, amount: Float, useMockPayment: Boolean = false) {
        if (useMockPayment) {
            startMockPayment(
                context = context,
                amount = amount,
                onSuccess = {
                    clearCartAndAddToOrder(amount.toDouble(), "Mock Payment")
                    showSuccessDialog(context)
                },
                onFailure = {
                    showToast(context, "Payment Failed")
                }
            )
        } else {
            // Razorpay
            GlobalNavigation.pendingOrderTotal = amount.toDouble()
            val checkout = Checkout()
            checkout.setKeyID(razorpayApiKey())

            val options = JSONObject()
            options.put("name", "EasyShop")
            options.put("description", "")
            options.put("amount", amount * 100)
            options.put("currency", "USD")

            checkout.open(context as Activity, options)
        }
    }


    private fun showSuccessDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("✅ Payment Successful")
            .setMessage("Your order has been placed successfully!")
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

    private const val PREF_NAME = "favorite_pref"
    private const val KEY_FAVORITES = "favorites_list"

    fun addOrRemoveFromFavorite(context: Context, productId: String){
        val list = getFavoriteList(context).toMutableSet()
        if(list.contains(productId)){
            list.remove(productId)
        } else {
            list.add(productId)
            showToast(context, "Item removed from Favorite")
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
        builder.setTitle("💳 Mock Payment")
            .setMessage(
                "Test Payment\n" +
                        "Amount: $${"%.2f".format(amount)}\n\n" +
                        "Choose result:"
            )
            .setPositiveButton("✅ Success") { _, _ ->
                // Delay để giống thật
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    onSuccess()
                }, 1500)
            }
            .setNegativeButton("❌ Failed") { _, _ ->
                onFailure()
            }
            .setNeutralButton("Cancel", null)
            .setCancelable(true)
            .show()
    }

}