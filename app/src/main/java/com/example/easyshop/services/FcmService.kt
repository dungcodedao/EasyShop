package com.example.easyshop.services

import android.util.Log
import com.example.easyshop.AppUtil
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FcmService", "Refreshed token: $token")
        // Luu token vao Firestore de Server/Admin co the gui thong bao
        AppUtil.saveFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FcmService", "From: ${remoteMessage.from}")

        // Truong hop 1: Co notification payload (app foreground -> tu xu ly)
        // Khi app o background, he thong tu dong hien notification nay
        val notifTitle = remoteMessage.notification?.title
        val notifBody = remoteMessage.notification?.body

        // Truong hop 2: Co data payload
        val dataTitle = remoteMessage.data["title"]
        val dataBody = remoteMessage.data["body"]
        val dataType = remoteMessage.data["type"] ?: "SYSTEM"

        // Uu tien lay du lieu tu notification, fallback sang data
        val title = notifTitle ?: dataTitle ?: "EasyShop"
        val body = notifBody ?: dataBody ?: ""

        if (title.isNotBlank() || body.isNotBlank()) {
            NotificationHelper.show(applicationContext, title, body, dataType)
            Log.d("FcmService", "Hien thong bao: $title - $body")
        }
    }
}
