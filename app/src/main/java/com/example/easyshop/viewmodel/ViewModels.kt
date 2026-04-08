package com.example.easyshop.viewmodel

import androidx.lifecycle.ViewModel
import com.example.easyshop.ScreenState
import com.example.easyshop.model.ProductModel
import com.example.easyshop.model.NotificationModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// --- FavoriteViewModel ---
class FavoriteViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val userId = Firebase.auth.currentUser?.uid

    private val _favorites = MutableStateFlow<List<ProductModel>>(emptyList())
    val favorites: StateFlow<List<ProductModel>> = _favorites.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _screenState = MutableStateFlow(ScreenState.LOADING)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    fun fetchFavorites() {
        if (userId == null) {
            _screenState.value = ScreenState.ERROR
            return
        }
        _isLoading.value = true
        _screenState.value = ScreenState.LOADING
        
        firestore.collection("users").document(userId)
            .collection("favorites")
            .get()
            .addOnSuccessListener { result ->
                val productList = result.documents.mapNotNull { doc ->
                    doc.toObject(ProductModel::class.java)?.apply { id = doc.id }
                }
                _favorites.value = productList
                _isLoading.value = false
                _screenState.value = if (productList.isEmpty()) ScreenState.EMPTY else ScreenState.SUCCESS
            }
            .addOnFailureListener { 
                _isLoading.value = false
                _screenState.value = ScreenState.ERROR
            }
    }
}

// --- ProductDetailsViewModel ---
class ProductDetailsViewModel : ViewModel() {
    private val firestore = Firebase.firestore

    private val _product = MutableStateFlow<ProductModel?>(null)
    val product: StateFlow<ProductModel?> = _product.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _screenState = MutableStateFlow(ScreenState.LOADING)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    fun fetchProduct(productId: String) {
        _isLoading.value = true
        _screenState.value = ScreenState.LOADING
        
        firestore.collection("data").document("stock").collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener { doc ->
                val prod = doc.toObject(ProductModel::class.java)?.apply { id = doc.id }
                _product.value = prod
                _isLoading.value = false
                _screenState.value = if (prod != null) ScreenState.SUCCESS else ScreenState.EMPTY
            }
            .addOnFailureListener { 
                _isLoading.value = false
                _screenState.value = ScreenState.ERROR
            }
    }
}

// --- NotificationsViewModel ---
class NotificationsViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val userId = Firebase.auth.currentUser?.uid

    private val _notifications = MutableStateFlow<List<NotificationModel>>(emptyList())
    val notifications: StateFlow<List<NotificationModel>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _screenState = MutableStateFlow(ScreenState.LOADING)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    fun fetchNotifications() {
        if (userId == null) {
            _screenState.value = ScreenState.ERROR
            return
        }
        _isLoading.value = true
        _screenState.value = ScreenState.LOADING
        
        firestore.collection("notifications")
            .whereIn("recipientRole", listOf("user", "all", "user_$userId"))
            .get()
            .addOnSuccessListener { result ->
                val notifList = result.documents.mapNotNull { it.toObject(NotificationModel::class.java) }
                val sortedList = notifList.sortedByDescending { it.createdAt }
                _notifications.value = sortedList
                _isLoading.value = false
                _screenState.value = if (sortedList.isEmpty()) ScreenState.EMPTY else ScreenState.SUCCESS
            }
            .addOnFailureListener { 
                _isLoading.value = false
                _screenState.value = ScreenState.ERROR
            }
    }
}

// --- AdminViewModel ---
class AdminViewModel : ViewModel() {
    private val firestore = Firebase.firestore

    private val _totalProducts = MutableStateFlow(0)
    val totalProducts: StateFlow<Int> = _totalProducts.asStateFlow()

    private val _totalOrders = MutableStateFlow(0)
    val totalOrders: StateFlow<Int> = _totalOrders.asStateFlow()

    private val _pendingOrders = MutableStateFlow(0)
    val pendingOrders: StateFlow<Int> = _pendingOrders.asStateFlow()

    private val _screenState = MutableStateFlow(ScreenState.LOADING)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    fun fetchStats() {
        _screenState.value = ScreenState.LOADING
        
        val p1 = firestore.collection("data").document("stock").collection("products").get()
        val p2 = firestore.collection("orders").get()
        
        p1.addOnSuccessListener { result ->
            _totalProducts.value = result.size()
            if (p2.isSuccessful) _screenState.value = ScreenState.SUCCESS
        }.addOnFailureListener { _screenState.value = ScreenState.ERROR }
        
        p2.addOnSuccessListener { result ->
            _totalOrders.value = result.size()
            _pendingOrders.value = result.documents.count { it.getString("status") == "ORDERED" }
            if (p1.isSuccessful) _screenState.value = ScreenState.SUCCESS
        }.addOnFailureListener { _screenState.value = ScreenState.ERROR }
    }
}
