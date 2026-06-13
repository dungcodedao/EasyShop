package com.example.easyshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.example.easyshop.ui.theme.EasyShopTheme
import com.example.easyshop.util.LanguageManager

class MainActivity : ComponentActivity() {

    /** Bọc context với locale đã lưu TRƯỚC KHI Activity khởi tạo. */
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Kiểm tra xem Activity có vừa được recreation do đổi ngôn ngữ hay không
        if (LanguageManager.getAndClearLanguageChangedFlag(this)) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            // Tắt animation ngay lập tức khi vào onCreate ở chế độ bình thường
            overridePendingTransition(0, 0)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasyShopTheme(darkTheme = false) {
                // Xin quyen Notification & luu FCM Token
                RequestNotificationPermission()

                AppNavigation()
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
