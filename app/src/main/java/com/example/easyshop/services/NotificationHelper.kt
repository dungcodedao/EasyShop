package com.example.easyshop.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.easyshop.R

object NotificationHelper {

    private const val CHANNEL_ORDER   = "channel_order"
    private const val CHANNEL_PROMO   = "channel_promo"
    private const val CHANNEL_SYSTEM  = "channel_system"

    /**
     * Tạo tất cả Notification Channels (Android 8+).
     * Gọi trong Application.onCreate().
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(CHANNEL_ORDER,  "Đơn hàng",    NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Thông báo về trạng thái đơn hàng"
            },
            NotificationChannel(CHANNEL_PROMO,  "Khuyến mãi",  NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Thông báo khuyến mãi và ưu đãi"
            },
            NotificationChannel(CHANNEL_SYSTEM, "Hệ thống",    NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Thông báo hệ thống chung"
            }
        )

        channels.forEach { manager.createNotificationChannel(it) }
    }

    /**
     * Hiển thị một system notification.
     * @param type - Loại thông báo: "ORDER", "PROMO", "SYSTEM"
     */
    fun show(context: Context, title: String, body: String, type: String = "SYSTEM") {
        val channelId = when (type.uppercase()) {
            "ORDER"  -> CHANNEL_ORDER
            "PROMO"  -> CHANNEL_PROMO
            else     -> CHANNEL_SYSTEM
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notifId = System.currentTimeMillis().toInt()
        manager.notify(notifId, notification)
    }
}
