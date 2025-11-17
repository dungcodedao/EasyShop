package com.example.easyshop

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.example.easyshop.model.OrderModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.razorpay.Checkout
import org.json.JSONObject
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

    fun clearCartAndAddToOrder(){
        val userDoc = Firebase.firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
        userDoc.get().addOnCompleteListener(){
            if(it.isSuccessful) {
                val currentCart = it.result.get("cartItems") as? Map<String, Long> ?: emptyMap()

                val order = OrderModel(
                    id = "ORD" + UUID.randomUUID().toString().replace("-","").take(10).uppercase(),

                    userId = FirebaseAuth.getInstance().currentUser?.uid!!,
                    date = Timestamp.now(),
                    items = currentCart,
                    status = "ORDERED",
                    address = it.result.get("address") as String
                )
                Firebase.firestore.collection("orders")
                    .document(order.id).set(order)
                    .addOnCompleteListener {
                        if (it.isSuccessful ) {
                            userDoc.update("cartItems", FieldValue.delete())

                        }
                    }

            }
        }

    }

    fun getDiscountPercentage() : Float{
        return 10.0f
    }

    fun getTaxPercentage() : Float{
        return 13.0f
    }

    //add payment methods
    fun razorpayApiKey() : String {
        return "rzp_test_5WgA34F9ljiXAX"
    }

    fun startPayment(amount : Float){
        val checkout = Checkout()
        checkout.setKeyID(razorpayApiKey())

        val options = JSONObject()
        options.put("name", "EasyShop")
        options.put("description", "")
        options.put("amount", amount*100)
        options.put("currency", "USD")

        checkout.open(GlobalNavigation.navController.context as Activity,options)


    }


}