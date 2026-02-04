package com.example.easyshop

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.easyshop.ui.theme.EasyShopTheme
import com.razorpay.PaymentResultListener

class MainActivity : ComponentActivity(), PaymentResultListener {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasyShopTheme(darkTheme = false, dynamicColor = false) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(Modifier.padding(innerPadding))
                }
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