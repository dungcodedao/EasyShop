package com.example.easyshop.services

import com.example.easyshop.model.SePayResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SePayApiService {
    @GET("transactions/list")
    suspend fun getTransactions(
        @Header("Authorization") token: String, // Bearer <TOKEN>
        @Query("transaction_content") content: String? = null,
        @Query("account_number") accountNumber: String? = null,
        @Query("amount_in_min") amountInMin: Double? = null,
        @Query("limit") limit: Int = 10
    ): SePayResponse
}
