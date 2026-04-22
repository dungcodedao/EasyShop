package com.example.easyshop.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easyshop.ScreenState
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OnboardingViewModel : ViewModel() {
    private val firestore = Firebase.firestore

    private val _onboardingUrls = MutableStateFlow<List<String>>(emptyList())
    val onboardingUrls: StateFlow<List<String>> = _onboardingUrls.asStateFlow()

    private val _screenState = MutableStateFlow(ScreenState.LOADING)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    init {
        fetchOnboardingImages()
    }

    private fun fetchOnboardingImages() {
        viewModelScope.launch {
            _screenState.value = ScreenState.LOADING
            try {
                val docSnapshot = firestore.collection("data").document("onbroad").get().await()
                @Suppress("UNCHECKED_CAST")
                val urls = docSnapshot.get("url") as? List<String> ?: emptyList()
                _onboardingUrls.value = urls
                _screenState.value = ScreenState.SUCCESS
            } catch (e: Exception) {
                e.printStackTrace()
                _screenState.value = ScreenState.ERROR
            }
        }
    }
}
