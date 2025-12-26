package com.example.easyshop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.easyshop.pages.CartPage
import com.example.easyshop.pages.CategoryProductsPage
import com.example.easyshop.pages.CheckoutPage
import com.example.easyshop.pages.OrdersPage
import com.example.easyshop.pages.ProductDetailsPage
import com.example.easyshop.screen.PaymentScreen
import com.example.easyshop.screen.AuthScreen
import com.example.easyshop.screen.HomeScreen
import com.example.easyshop.screen.LoginScreen
import com.example.easyshop.screen.SignupScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun AppNavigation(modifier: Modifier = Modifier){
    val navController = rememberNavController()
    GlobalNavigation.navController = navController

    val isLoggedIn = Firebase.auth.currentUser != null
    val firstPage = if (isLoggedIn) "home" else "auth"

    NavHost(navController = navController, startDestination = firstPage){

        composable("auth") {
            AuthScreen(modifier, navController)
        }

        composable("login") {
            LoginScreen(modifier, navController)
        }

        composable("signup") {
            SignupScreen(modifier, navController)
        }

        composable("home") {
            HomeScreen(modifier, navController)
        }

        composable("category-products/{categoryId}") {
            var categoryId = it.arguments?.getString("categoryId")
            CategoryProductsPage(modifier, categoryId?:"")
        }

        composable("product-details/{productId}") {
            var productId = it.arguments?.getString("productId")
            ProductDetailsPage(modifier, productId?:"")
        }

        composable("checkout"){
            CheckoutPage(modifier)
        }

        composable("orders") {
            OrdersPage(modifier)
        }

        composable("cart") {
            CartPage(modifier)
        }


        // ✅ THÊM ROUTE PAYMENT
        composable(
            route = "payment/{totalAmount}",
            arguments = listOf(
                navArgument("totalAmount") {
                    type = NavType.FloatType
                    defaultValue = 0.0f
                }
            )
        ) { backStackEntry ->
            val totalAmount = backStackEntry.arguments?.getFloat("totalAmount") ?: 0.0f

            PaymentScreen(
                modifier = modifier,
                navController = navController,
                totalAmount = totalAmount.toDouble()
            )
        }
    }
}

object GlobalNavigation{
    lateinit var navController : NavController
}