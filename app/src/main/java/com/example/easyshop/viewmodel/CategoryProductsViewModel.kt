package com.example.easyshop.viewmodel

import androidx.lifecycle.ViewModel
import com.example.easyshop.ScreenState
import com.example.easyshop.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CategoryProductsViewModel : ViewModel() {
    private val firestore = Firebase.firestore

    private val _products = MutableStateFlow<List<ProductModel>>(emptyList())
    val products: StateFlow<List<ProductModel>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _screenState = MutableStateFlow(ScreenState.LOADING)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    fun fetchProductsByCategory(categoryId: String) {
        _isLoading.value = true
        _screenState.value = ScreenState.LOADING
        
        firestore.collection("data").document("stock").collection("products")
            .whereEqualTo("category", categoryId)
            .get()
            .addOnSuccessListener { result ->
                val productList = result.documents.mapNotNull { it.toObject(ProductModel::class.java).apply { this?.id = it.id } }
                _products.value = productList
                _isLoading.value = false
                _screenState.value = if (productList.isEmpty()) ScreenState.EMPTY else ScreenState.SUCCESS
            }
            .addOnFailureListener {
                _isLoading.value = false
                _screenState.value = ScreenState.ERROR
            }
    }
}
