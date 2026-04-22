package com.example.easyshop.model

import java.util.UUID

data class AddressModel(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Nhà riêng", // Ví dụ: Nhà riêng, Văn phòng
    val fullName: String = "",
    val phone: String = "",
    val detailedAddress: String = "",
    val isDefault: Boolean = false
)

data class UserModel(
    val name: String = "",
    val email: String = "",
    val uid: String = "",
    val phone: String = "",
    val cartItems: Map<String, Long> = emptyMap(),
    // Bạn vẫn có thể giữ 'address' cũ để tương thích ngược 
    // nhưng chúng ta sẽ ưu tiên dùng 'addressList'
    val address: String = "", 
    val addressList: List<AddressModel> = emptyList(),
    val role: String = "user",
    val profileImg: String = "",
    val fcmToken: String = "",     // Thêm để lưu Device Token nhận Push Notifications
    val savedPromoCodes: List<String> = emptyList() // Danh sách ID mã giảm giá đã sưu tầm
)
