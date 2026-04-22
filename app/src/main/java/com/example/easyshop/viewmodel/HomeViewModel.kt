package com.example.easyshop.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easyshop.ScreenState
import com.example.easyshop.model.CategoryModel
import com.example.easyshop.model.PromoCodeModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {
    private val firestore = Firebase.firestore

    private val _banners = MutableStateFlow<List<String>>(emptyList())
    val banners: StateFlow<List<String>> = _banners.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryModel>>(emptyList())
    val categories: StateFlow<List<CategoryModel>> = _categories.asStateFlow()

    private val _promos = MutableStateFlow<List<PromoCodeModel>>(emptyList())
    val promos: StateFlow<List<PromoCodeModel>> = _promos.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _screenState = MutableStateFlow(ScreenState.LOADING)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            initData()
            _isRefreshing.value = false
        }
    }

    private suspend fun initData() {
        _screenState.value = ScreenState.LOADING
        try {
            // Parallel Fetch using async
            val bannersDeferred = viewModelScope.async { firestore.collection("data").document("banners").get().await() }
            val categoriesDeferred = viewModelScope.async { firestore.collection("data").document("stock").collection("categories").get().await() }
            val promosDeferred = viewModelScope.async { 
                firestore.collection("promoCodes")
                    .whereEqualTo("active", true)
                    .get().await()
            }
            
            val bannersResult = bannersDeferred.await()
            val categoriesResult = categoriesDeferred.await()
            val promosResult = promosDeferred.await()
            
            @Suppress("UNCHECKED_CAST")
            var bannerUrls = (bannersResult.get("urls") as? List<String>) 
                ?: (bannersResult.get("url") as? List<String>) 
                ?: emptyList()
            
            // Fallback to high-quality default banners if empty
            if (bannerUrls.isEmpty()) {
                bannerUrls = listOf(
                    "https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?q=80&w=2070&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1441986300917-64674bd600d8?q=80&w=2070&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1498049794561-7780e7231661?q=80&w=2070&auto=format&fit=crop"
                )
            }
            
            val categoryList = categoriesResult.documents.mapNotNull { it.toObject(CategoryModel::class.java) }
            val promoList = promosResult.documents.mapNotNull { it.toObject(PromoCodeModel::class.java) }
                .filter { it.expiryDate == 0L || it.expiryDate > System.currentTimeMillis() }
            
            _banners.value = bannerUrls
            _categories.value = categoryList
            _promos.value = promoList
            
            _screenState.value = ScreenState.SUCCESS
        } catch (e: Exception) {
            e.printStackTrace()
            _screenState.value = ScreenState.ERROR
        }
    }

    fun savePromoCode(codeId: String, onComplete: (Boolean, String) -> Unit) {
        val uid = Firebase.auth.currentUser?.uid ?: run {
            onComplete(false, "Vui lòng đăng nhập để lưu mã")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update("savedPromoCodes", FieldValue.arrayUnion(codeId))
                    .await()
                onComplete(true, "Đã lưu mã vào ví của bạn!")
            } catch (e: Exception) {
                onComplete(false, "Lỗi khi lưu mã: ${e.message}")
            }
        }
    }
}
