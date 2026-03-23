package com.example.easyshop.model

import com.google.firebase.Timestamp

/**
 * Mô hình thông báo cho cả admin và user.
 *
 * Luồng:
 * - User đặt đơn → ghi notification type=NEW_ORDER cho admin
 * - Admin đổi trạng thái đơn → ghi notification type=ORDER_STATUS cho user
 * - Admin hủy đơn → ghi notification type=CANCELLED cho user
 */
data class NotificationModel(
    val id: String = "",
    val userId: String = "",            // "admin" = hiển thị cho admin, uid = hiển thị cho user cụ thể
    val title: String = "",
    val body: String = "",
    val type: String = "SYSTEM",
    // NEW_ORDER | ORDER_STATUS | CANCELLED | SHIPPING | DELIVERED | PROMO | SYSTEM
    val orderId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val recipientRole: String = "user"  // "admin" hoặc "user"
)
