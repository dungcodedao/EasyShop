package com.example.easyshop.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easyshop.ScreenState
import com.example.easyshop.model.CategoryModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {
    private val firestore = Firebase.firestore

    private val _banners = MutableStateFlow<List<String>>(emptyList())
    val banners: StateFlow<List<String>> = _banners.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryModel>>(emptyList())
    val categories: StateFlow<List<CategoryModel>> = _categories.asStateFlow()

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

    private fun initData() {
        viewModelScope.launch {
            _screenState.value = ScreenState.LOADING
            try {
                // Fetch banners and categories in parallel using coroutines
                val bannersJob = firestore.collection("data").document("banners").get()
                val categoriesJob = firestore.collection("data").document("stock").collection("categories").get()
                
                val bannersResult = bannersJob.await()
                val categoriesResult = categoriesJob.await()
                
                @Suppress("UNCHECKED_CAST")
                val bannerUrls = bannersResult.get("urls") as? List<String> ?: emptyList()
                val categoryList = categoriesResult.documents.mapNotNull { it.toObject(CategoryModel::class.java) }
                
                _banners.value = bannerUrls
                _categories.value = categoryList
                _screenState.value = ScreenState.SUCCESS
            } catch (e: Exception) {
                _screenState.value = ScreenState.ERROR
            }
        }
    }
}
