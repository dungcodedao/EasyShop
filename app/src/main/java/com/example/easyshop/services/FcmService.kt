package com.example.easyshop.services

import android.util.Log
import com.example.easyshop.AppUtil
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FcmService", "Refreshed token: $token")
        // Lưu token vào Firestore để Server/Admin có thể gửi thông báo
        AppUtil.saveFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Khi app ở trạng thái foreground, ta tự hiển thị thông báo
        remoteMessage.notification?.let {
            val title = it.title ?: "EasyShop"
            val body = it.body ?: ""
            val type = remoteMessage.data["type"] ?: "SYSTEM"
            
            NotificationHelper.show(applicationContext, title, body, type)
            
            // Có thể bắn thêm Banner Controller nếu muốn (để hiện in-app banner khi đang dùng)
            // com.example.easyshop.components.NotifBannerController.show(title, body, type)
        }
    }
}
