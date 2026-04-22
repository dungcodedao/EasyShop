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
import com.example.easyshop.BuildConfig
import com.example.easyshop.network.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val _paymentReference = MutableStateFlow("")
    val paymentReference: StateFlow<String> = _paymentReference.asStateFlow()

    private val _isPaymentChecking = MutableStateFlow(false)
    val isPaymentChecking: StateFlow<Boolean> = _isPaymentChecking.asStateFlow()

    private var pollingJob: Job? = null

    private val _isPaymentConfirmed = MutableStateFlow(false)
    val isPaymentConfirmed: StateFlow<Boolean> = _isPaymentConfirmed.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _savedVouchers = MutableStateFlow<List<PromoCodeModel>>(emptyList())
    val savedVouchers: StateFlow<List<PromoCodeModel>> = _savedVouchers.asStateFlow()

    fun updateNote(newNote: String) {
        _note.value = newNote
    }

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
                
                // Tải thông tin chi tiết voucher trong ví
                user?.savedPromoCodes?.filter { it.isNotBlank() }?.let { promoIds ->
                    if (promoIds.isNotEmpty()) {
                        val promoSnapshots = mutableListOf<PromoCodeModel>()
                        promoIds.chunked(30).forEach { chunk ->
                            val result = db.collection("promoCodes")
                                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                                .get().await()
                            promoSnapshots.addAll(result.toObjects(PromoCodeModel::class.java))
                        }
                        _savedVouchers.value = promoSnapshots
                    }
                }

                // Khởi tạo mã tham chiếu thanh toán duy nhất cho phiên này
                if (_paymentReference.value.isEmpty()) {
                    _paymentReference.value = "ES" + System.currentTimeMillis().toString().takeLast(6)
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
        return com.example.easyshop.AppUtil.parsePrice(priceStr)
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

    fun setDiscountInfo(promoCode: String, discount: Double) {
        _promoCode.value = promoCode
        _discount.value = discount
    }

    fun placeOrder(paymentMethod: String, note: String = "") {
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
                date = Timestamp.now(),
                note = if (note.isNotBlank()) note else _note.value
            )

            try {
                db.runTransaction { transaction ->
                    // --- GIAI ĐOẠN 1: ĐỌC (READS) ---
                    // Phải đọc tất cả dữ liệu trước khi thực hiện bất kỳ lệnh ghi nào
                    val productSnapshots = user.cartItems.map { (pid, _) ->
                        val productRef = db.collection("data").document("stock")
                            .collection("products").document(pid)
                        productRef to transaction.get(productRef)
                    }

                    val promoSnap = if (_promoCode.value.isNotEmpty()) {
                        val promoRef = db.collection("promoCodes").document(_promoCode.value)
                        promoRef to transaction.get(promoRef)
                    } else null

                    // --- GIAI ĐOẠN 2: KIỂM TRA & GHI (WRITES) ---
                    productSnapshots.forEach { (productRef, snapshot) ->
                        val qty = user.cartItems[snapshot.id] ?: 0L
                        val stock = snapshot.getLong("stockCount") ?: 0L
                        
                        if (stock < qty) {
                            throw Exception("Sản phẩm ${snapshot.getString("title")} đã hết hàng hoặc không đủ số lượng")
                        }
                        
                        // Update Stock
                        transaction.update(productRef, "stockCount", stock - qty)
                        if (stock - qty <= 0) {
                            transaction.update(productRef, "inStock", false)
                        }
                    }

                    promoSnap?.let { (promoRef, snapshot) ->
                        if (!snapshot.exists() || snapshot.getBoolean("active") == false) {
                            throw Exception("Mã giảm giá không còn khả dụng")
                        }
                        val usageLimit = snapshot.getLong("usageLimit") ?: 0L
                        val usedCount = snapshot.getLong("usedCount") ?: 0L
                        if (usageLimit > 0 && usedCount >= usageLimit) {
                            throw Exception("Mã giảm giá đã hết lượt sử dụng")
                        }
                        transaction.update(promoRef, "usedCount", usedCount + 1)
                    }

                    // Lưu đơn hàng và xóa giỏ hàng
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

    /**
     * Xác thực thanh toán qua SePay API
     */
    fun verifySePayPayment(amount: Double) {
        if (_isPaymentChecking.value) return
        
        _isPaymentChecking.value = true
        _checkoutResult.value = CheckoutResult.Loading

        viewModelScope.launch {
            try {
                val token = "Bearer " + BuildConfig.SEPAY_TOKEN
                val response = RetrofitClient.sePayApiService.getTransactions(
                    token = token,
                    content = _paymentReference.value,
                    amountInMin = amount - 1.0, // Cho phép sai số nhỏ nếu cần
                    limit = 5
                )

                if (response.status == 200) {
                    // Kiểm tra xem có giao dịch nào khớp chính xác không
                    val successTransaction = response.transactions.find { it.transactionContent?.contains(_paymentReference.value, ignoreCase = true) == true }
                    
                    if (successTransaction != null) {
                        // Thanh toán thành công -> Tiến hành đặt hàng
                        placeOrder("MB")
                    } else {
                        // Chưa tìm thấy giao dịch
                        _checkoutResult.value = CheckoutResult.Error("Chưa tìm thấy giao dịch chuyển khoản cho mã ${_paymentReference.value}. Vui lòng thử lại sau 1-2 phút hoặc liên hệ hỗ trợ.")
                    }
                } else {
                    _checkoutResult.value = CheckoutResult.Error("Lỗi từ hệ thống SePay: ${response.messages}")
                }
            } catch (e: Exception) {
                _checkoutResult.value = CheckoutResult.Error("Lỗi kết nối khi kiểm tra thanh toán: ${e.message}")
            } finally {
                _isPaymentChecking.value = false
            }
        }
    }

    /**
     * Bắt đầu quét thanh toán tự động mỗi 5 giây
     */
    fun startPaymentPolling(amount: Double) {
        if (pollingJob != null && pollingJob?.isActive == true) return
        
        pollingJob = viewModelScope.launch {
            // Quét trong tối đa 15 phút (180 lần x 5s)
            var retryCount = 0
            while (retryCount < 180) {
                try {
                    val token = "Bearer " + BuildConfig.SEPAY_TOKEN
                    val response = RetrofitClient.sePayApiService.getTransactions(
                        token = token,
                        content = _paymentReference.value,
                        amountInMin = amount - 1.0,
                        limit = 5
                    )

                    if (response.status == 200) {
                        val successTransaction = response.transactions.find { 
                            it.transactionContent?.contains(_paymentReference.value, ignoreCase = true) == true 
                        }
                        
                        if (successTransaction != null) {
                            // Tìm thấy tiền! Kích hoạt trạng thái Xác nhận để sáng nút bấm
                            _isPaymentConfirmed.value = true
                            break
                        }
                    }
                } catch (e: Exception) {
                    // Lỗi mạng tạm thời, bỏ qua và quét tiếp ở vòng sau
                }
                
                delay(5000) // Chờ 5 giây cho vòng quét tiếp theo
                retryCount++
            }
        }
    }

    /**
     * Dừng quét thanh toán
     */
    fun stopPaymentPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPaymentPolling()
    }
}
