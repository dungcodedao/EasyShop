package com.example.easyshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.example.easyshop.ui.theme.EasyShopTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasyShopTheme(darkTheme = false) {
                // Xin quyen Notification & luu FCM Token
                RequestNotificationPermission()

                // Khoi tao bo lang nghe thong bao toan cuc (ca nhan + broadcast)
                LaunchedEffect(Unit) {
                    startNotificationListener(this@MainActivity)
                }

                AppNavigation()
            }
        }
    }

    /**
     * Lang nghe tat ca thong bao moi (ca nhan + broadcast) tu Firestore.
     * Khi co thong bao moi, hien thi push notification qua NotificationHelper.
     */
    private fun startNotificationListener(context: android.content.Context) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val startTime = com.google.firebase.Timestamp.now()

        // Lang nghe thong bao ca nhan cua user (don hang, he thong...)
        db.collection("notifications")
            .whereEqualTo("recipientRole", "user")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    android.util.Log.e("MainActivity", "User notification listener error: ${e.message}")
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { dc ->
                    if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val createdAt = dc.document.getTimestamp("createdAt")
                        // Chi hien thong bao moi (sau khi app khoi dong)
                        if (createdAt != null && createdAt > startTime) {
                            val title = dc.document.getString("title") ?: "Thong bao moi"
                            val body = dc.document.getString("body") ?: ""
                            val type = dc.document.getString("type") ?: "SYSTEM"
                            com.example.easyshop.services.NotificationHelper.show(
                                context = context, title = title, body = body, type = type
                            )
                        }
                    }
                }
            }

        // Lang nghe thong bao broadcast (khuyen mai chung)
        db.collection("notifications")
            .whereEqualTo("userId", "broadcast")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    android.util.Log.e("MainActivity", "Broadcast listener error: ${e.message}")
                    return@addSnapshotListener
                }
                snapshots?.documentChanges?.forEach { dc ->
                    if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val createdAt = dc.document.getTimestamp("createdAt")
                        if (createdAt != null && createdAt > startTime) {
                            val title = dc.document.getString("title") ?: "Khuyen mai moi"
                            val body = dc.document.getString("body") ?: ""
                            com.example.easyshop.services.NotificationHelper.show(
                                context = context, title = title, body = body, type = "PROMO"
                            )
                        }
                    }
                }
            }
    }
}

@androidx.compose.runtime.Composable
fun RequestNotificationPermission() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            retrieveAndSaveFcmToken()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            when (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)) {
                android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    retrieveAndSaveFcmToken()
                }
                else -> {
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            retrieveAndSaveFcmToken()
        }
    }
}

fun retrieveAndSaveFcmToken() {
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val token = task.result
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener { android.util.Log.d("FCM_TOKEN", "Lưu token thành công: $token") }
        }
    }
}
