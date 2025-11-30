package com.example.easyshop.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object MapUtils {

    /**
     * Mở Google Maps để user chọn địa chỉ (Place Picker)
     */
    fun openMapToPickAddress(context: Context, currentAddress: String = "") {
        // Mở Google Maps ở chế độ tìm kiếm tại Việt Nam
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = if (currentAddress.isNotEmpty()) {
                Uri.parse("geo:0,0?q=${Uri.encode(currentAddress)}")
            } else {
                Uri.parse("geo:21.0285,105.8542?z=15") // Hà Nội
            }
            setPackage("com.google.android.apps.maps")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: mở browser
            val url = if (currentAddress.isNotEmpty()) {
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(currentAddress)}"
            } else {
                "https://www.google.com/maps/@21.0285,105.8542,15z"
            }
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}