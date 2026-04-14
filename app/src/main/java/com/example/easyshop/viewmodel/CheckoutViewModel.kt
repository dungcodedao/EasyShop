package com.example.easyshop.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easyshop.model.AddressModel
import com.example.easyshop.model.OrderModel
import com.example.easyshop.model.ProductModel
import com.example.easyshop.model.PromoCodeModel
import com.example.easyshop.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class CheckoutResult {
    object Idle : CheckoutResult()
    object Loading : CheckoutResult()
    data class Success(val orderId: String, val total: Double) : CheckoutResult()
    data class Error(val message: String) : CheckoutResult()
}

class CheckoutViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _userModel = MutableStateFlow<UserModel?>(null)
    val userModel: StateFlow<UserModel?> = _userModel.asStateFlow()

    private val _cartProducts = MutableStateFlow<List<ProductModel>>(emptyList())
    val cartProducts: StateFlow<List<ProductModel>> = _cartProducts.asStateFlow()

    private val _subtotal = MutableStateFlow(0.0)
    val subtotal: StateFlow<Double> = _subtotal.asStateFlow()

    private val _discount = MutableStateFlow(0.0)
    val discount: StateFlow<Double> = _discount.asStateFlow()

    private val _promoCode = MutableStateFlow("")
    val promoCode: StateFlow<String> = _promoCode.asStateFlow()

    private val _checkoutResult = MutableStateFlow<CheckoutResult>(CheckoutResult.Idle)
    val checkoutResult: StateFlow<CheckoutResult> = _checkoutResult.asStateFlow()

    private val _selectedAddress = MutableStateFlow<AddressModel?>(null)
    val selectedAddress: StateFlow<AddressModel?> = _selectedAddress.asStateFlow()

    fun fetchData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(uid).get().await()
                val user = userDoc.toObject(UserModel::class.java)
                _userModel.value = user
                
                user?.let {
                    if (it.addressList.isNotEmpty()) {
                        _selectedAddress.value = it.addressList.find { addr -> addr.isDefault } 
                            ?: it.addressList.firstOrNull()
                    }

                    if (it.cartItems.isNotEmpty()) {
                        val productIds = it.cartItems.keys.toList()
                        // Chunking productIds for large carts (Firebase whereIn limit is 30)
                        val products = mutableListOf<ProductModel>()
                        productIds.chunked(30).forEach { chunk ->
                            val productSnapshot = db.collection("data").document("stock")
                                .collection("products")
                                .whereIn("id", chunk)
                                .get().await()
                            products.addAll(productSnapshot.toObjects(ProductModel::class.java))
                        }
                        
                        _cartProducts.value = products
                        calculateTotals(it.cartItems, products)
                    }
                }
            } catch (e: Exception) {
                _checkoutResult.value = CheckoutResult.Error("Lỗi khi tải dữ liệu: ${e.message}")
            }
        }
    }

    private fun calculateTotals(cartItems: Map<String, Long>, products: List<ProductModel>) {
        var sub = 0.0
        products.forEach { product ->
            val qty = cartItems[product.id] ?: 0L
            val price = parsePrice(product.actualPrice)
            sub += price * qty
        }
        _subtotal.value = sub
    }

    private fun parsePrice(priceStr: String): Double {
        // Robust price parsing: "80.000đ" -> 80000.0
        return priceStr.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 0.0
    }

    fun setSelectedAddress(address: AddressModel) {
        _selectedAddress.value = address
    }

    fun applyPromoCode(code: String, promo: PromoCodeModel) {
        _promoCode.value = code
        val disc = when (promo.type) {
            "percentage" -> {
                val d = _subtotal.value * (promo.value.toDouble() / 100.0)
                if (promo.maxDiscount > 0 && d > promo.maxDiscount) promo.maxDiscount.toDouble() else d
            }
            "fixed" -> promo.value.toDouble()
            else -> 0.0
        }
        _discount.value = if (disc > _subtotal.value) _subtotal.value else disc
    }

    fun removePromoCode() {
        _promoCode.value = ""
        _discount.value = 0.0
    }

    fun placeOrder(paymentMethod: String) {
        val user = _userModel.value ?: return
        val address = _selectedAddress.value
        if (address == null) {
            _checkoutResult.value = CheckoutResult.Error("Vui lòng chọn hoặc thêm địa chỉ nhận hàng")
            return
        }
        val uid = auth.currentUser?.uid ?: return

        if (_checkoutResult.value is CheckoutResult.Loading) return

        _checkoutResult.value = CheckoutResult.Loading

        viewModelScope.launch {
            val orderId = "ORD" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
            val total = (_subtotal.value - _discount.value).coerceAtLeast(0.0)
            
            val order = OrderModel(
                id = orderId,
                userId = uid,
                userName = user.name,
                userEmail = user.email,
                userPhone = address.phone,
                items = user.cartItems,
                subtotal = _subtotal.value,
                discount = _discount.value,
                promoCode = _promoCode.value,
                total = total,
                status = "ORDERED",
                address = "${address.label}: ${address.detailedAddress}",
                paymentMethod = paymentMethod,
                date = Timestamp.now()
            )

            try {
                db.runTransaction { transaction ->
                    // 1. Verify Stock and Promo in ONE transaction
                    user.cartItems.forEach { (pid, qty) ->
                        val productRef = db.collection("data").document("stock")
                            .collection("products").document(pid)
                        val productSnap = transaction.get(productRef)
                        val stock = productSnap.getLong("stockCount") ?: 0L
                        if (stock < qty) {
                            throw Exception("Sản phẩm ${productSnap.getString("title")} đã hết hàng hoặc không đủ số lượng")
                        }
                        
                        // Update Stock
                        transaction.update(productRef, "stockCount", stock - qty)
                        if (stock - qty <= 0) {
                            transaction.update(productRef, "inStock", false)
                        }
                    }

                    if (_promoCode.value.isNotEmpty()) {
                        val promoRef = db.collection("promoCodes").document(_promoCode.value)
                        val promoSnap = transaction.get(promoRef)
                        if (!promoSnap.exists() || promoSnap.getBoolean("active") == false) {
                            throw Exception("Mã giảm giá không còn khả dụng")
                        }
                        val usageLimit = promoSnap.getLong("usageLimit") ?: 0L
                        val usedCount = promoSnap.getLong("usedCount") ?: 0L
                        if (usageLimit > 0 && usedCount >= usageLimit) {
                            throw Exception("Mã giảm giá đã hết lượt sử dụng")
                        }
                        transaction.update(promoRef, "usedCount", usedCount + 1)
                    }

                    // 2. Finalize: Set Order and Clear Cart
                    transaction.set(db.collection("orders").document(orderId), order)
                    transaction.update(db.collection("users").document(uid), "cartItems", emptyMap<String, Long>())
                }.await()

                _checkoutResult.value = CheckoutResult.Success(orderId, total)
            } catch (e: Exception) {
                _checkoutResult.value = CheckoutResult.Error(e.message ?: "Có lỗi xảy ra khi đặt hàng")
            }
        }
    }
    
    fun resetResult() {
        _checkoutResult.value = CheckoutResult.Idle
    }
}
