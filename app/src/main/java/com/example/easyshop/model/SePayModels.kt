package com.example.easyshop.model

import com.google.gson.annotations.SerializedName

data class SePayResponse(
    val status: Int,
    val messages: String,
    val transactions: List<SePayTransaction>
)

data class SePayTransaction(
    val id: Int,
    @SerializedName("bank_brand_name") val bankBrandName: String?,
    @SerializedName("account_number") val accountNumber: String?,
    @SerializedName("transaction_date") val transactionDate: String?,
    @SerializedName("amount_in") val amountIn: String?,
    @SerializedName("amount_out") val amountOut: String?,
    @SerializedName("accumulated_balance") val accumulatedBalance: String?,
    @SerializedName("transaction_content") val transactionContent: String?,
    @SerializedName("reference_number") val referenceNumber: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("sub_account") val subAccount: String?
)
