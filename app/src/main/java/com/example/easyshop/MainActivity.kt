package com.example.easyshop

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.easyshop.ui.theme.EasyShopTheme
import com.razorpay.PaymentResultListener

class MainActivity : ComponentActivity(), PaymentResultListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasyShopTheme(darkTheme = false) {
                // Xin quyền Notification & lưu FCM Token
                RequestNotificationPermission()

                AppNavigation()
            }
        }
    }

    override fun onPaymentSuccess(p0:String?){
        AppUtil.clearCartAndAddToOrder(GlobalNavigation.pendingOrderTotal, "Razorpay")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Payment Successful")
            .setMessage("Thank you! Your payment was completed successfully and your order has been placed")
            .setPositiveButton("OK"){ _ , _ ->
                val navController = GlobalNavigation.navController
                navController.popBackStack()
                navController.navigate("home")
            }
            .setCancelable(false)
            .show()
    }

    override fun onPaymentError(p0: Int, p1: String?){
        AppUtil.showToast(this, "Payment Failed")
    }

}

@Composable
fun RequestNotificationPermission() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Đã cấp quyền -> Lấy bộ mã điện thoại đăng kí lên thư viện FCM Google
            retrieveAndSaveFcmToken()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            when (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)) {
                android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    // Nếu đã từng cấp quyền rồi
                    retrieveAndSaveFcmToken()
                }
                else -> {
                    // Nảy lên bản popup để hỏi người dùng
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 trở xuống tự động có quyền
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
                .addOnSuccessListener { android.util.Log.d("FCM_TOKEN", "Lưu token thành công vào tài khoản User: $token") }
        }
    }
}