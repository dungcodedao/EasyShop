package com.example.easyshop

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.easyshop.components.NotifBannerController
import com.example.easyshop.services.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.initialize

class EasyShopApplication : Application(), ImageLoaderFactory {
    companion object {
        lateinit var instance: EasyShopApplication
            private set
    }

    private var notifListener: ListenerRegistration? = null
    private var broadcastListener: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Khởi tạo Firebase App Check
        Firebase.initialize(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        )

        // Tạo Notification Channels (bắt buộc Android 8+)
        NotificationHelper.createChannels(this)

        // Lắng nghe Auth state để start/stop Firestore listener
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != null) {
                startNotificationListener(uid)
            } else {
                stopNotificationListener()
            }
        }
    }

    /**
     * Lắng nghe collection "notifications" của user hiện tại và broadcast.
     * Khi có document mới với isRead = false hoặc broadcast mới → bắn banner và system notification.
     *
     * Dùng Set để track những ID đã hiện thông báo, tránh spam khi
     * Firestore gửi lại snapshot cũ mỗi lần mở app.
     */
    private val shownNotifIds = mutableSetOf<String>()

    private fun startNotificationListener(uid: String) {
        stopNotificationListener()
        shownNotifIds.clear()

        val db = FirebaseFirestore.getInstance()
        val startTime = com.google.firebase.Timestamp.now()

        // --- Listener cho USER ---
        notifListener = db.collection("notifications")
            .whereEqualTo("recipientRole", "user")
            .whereEqualTo("userId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    val docId = change.document.id
                    // Chỉ hiện với document THÊM MỚI chưa từng show và mới được tạo
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED
                        && docId !in shownNotifIds
                    ) {
                        val createdAt = change.document.getTimestamp("createdAt")
                        if (createdAt != null && createdAt > startTime) {
                            shownNotifIds.add(docId)
                            val title = change.document.getString("title") ?: "EasyShop"
                            val body  = change.document.getString("body")  ?: ""
                            val type  = change.document.getString("type")  ?: "SYSTEM"
                            // 1️⃣ Banner trượt xuống trong app (kiểu Shopee/MoMo)
                            NotifBannerController.show(title, body, type)
                            // 2️⃣ System notification ngoài màn hình (khi app ở nền)
                            NotificationHelper.show(this, title, body, type)
                        }
                    }
                }
            }

        // --- Listener cho BROADCAST ---
        broadcastListener = db.collection("notifications")
            .whereEqualTo("userId", "broadcast")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    val docId = change.document.id
                    // Chỉ hiện với document THÊM MỚI chưa từng show và mới được tạo
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED
                        && docId !in shownNotifIds
                    ) {
                        val createdAt = change.document.getTimestamp("createdAt")
                        if (createdAt != null && createdAt > startTime) {
                            shownNotifIds.add(docId)
                            val title = change.document.getString("title") ?: "Khuyến mãi mới"
                            val body  = change.document.getString("body")  ?: ""
                            val type  = change.document.getString("type")  ?: "PROMO"
                            // 1️⃣ Banner trượt xuống trong app
                            NotifBannerController.show(title, body, type)
                            // 2️⃣ System notification ngoài màn hình
                            NotificationHelper.show(this, title, body, type)
                        }
                    }
                }
            }
    }

    private fun stopNotificationListener() {
        notifListener?.remove()
        notifListener = null
        broadcastListener?.remove()
        broadcastListener = null
    }

    // ── Coil ImageLoader (cấu hình cache toàn cục) ──────────────────────────
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
