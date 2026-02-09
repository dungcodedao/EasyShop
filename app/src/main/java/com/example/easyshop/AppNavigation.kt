package com.example.easyshop

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.easyshop.admin.AdminDashboardScreen
import com.example.easyshop.admin.AdminHomeScreen
import com.example.easyshop.admin.AnalyticsScreen
import com.example.easyshop.admin.ManageCategoriesScreen
import com.example.easyshop.admin.ManagePromoCodesScreen
import com.example.easyshop.admin.ManageUsersScreen
import com.example.easyshop.admin.OrdersManagementScreen
import com.example.easyshop.model.UserModel
import com.example.easyshop.pages.*
import com.example.easyshop.screen.*
import com.example.easyshop.ai.ui.AIChatScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore


@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    GlobalNavigation.navController = navController

    var isCheckingRole by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf("auth") }

    val currentUser = Firebase.auth.currentUser

    // Check user role to determine start destination
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            Firebase.firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(UserModel::class.java)
                    startDestination = if (user?.role == "admin") {
                        "admin-dashboard"
                    } else {
                        "home"
                    }
                    isCheckingRole = false
                }
                .addOnFailureListener {
                    startDestination = "home"
                    isCheckingRole = false
                }
        } else {
            startDestination = "auth"
            isCheckingRole = false
        }
    }

    if (!isCheckingRole) {
        NavHost(navController = navController, startDestination = startDestination) {


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
                val categoryId = it.arguments?.getString("categoryId")
                CategoryProductsPage(modifier, categoryId ?: "")
            }

            composable("product-details/{productId}") {
                val productId = it.arguments?.getString("productId")
                ProductDetailsPage(modifier, productId ?: "")
            }

            composable("cart") {
                CartPage(modifier, navController)
            }

            composable("checkout") {
                CheckoutPage(modifier)
            }

            composable("orders") {
                OrdersPage(modifier)
            }

            composable(
                route = "order-details/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                OrderDetailsPage(modifier, orderId)
            }

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


            // Admin Dashboard - Main screen
            composable("admin-dashboard") {
                AdminDashboardScreen(navController = navController)
            }

            // Manage Products - List all products
            composable("admin-products") {
                AdminHomeScreen(modifier, navController)
            }

            // Add New Product
            composable("add-product") {
                AddProductPage(modifier, navController)
            }

            // Edit Product
            composable(
                route = "edit_product/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                EditProductPage(
                    productId = productId,
                    navController = navController
                )
            }

            // Orders Management
            composable("orders-management") {
                OrdersManagementScreen(navController = navController)
            }

            // Categories
            composable("manage-categories") {
                ManageCategoriesScreen(navController, modifier)
            }
            composable("manage-promo-codes") {
                ManagePromoCodesScreen(navController, modifier)
            }

            // Analytics
            composable("analytics") {
                AnalyticsScreen(navController = navController)
            }

            // AI Chat
            composable("ai-chat") {
                AIChatScreen(navController = navController)
            }

            // Manage Users
            composable("manage-users") {
                ManageUsersScreen(navController = navController)
            }

            // Receipt
            composable(
                route = "receipt/{amount}/{orderId}",
                arguments = listOf(
                    navArgument("amount") { type = NavType.FloatType },
                    navArgument("orderId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val amount = backStackEntry.arguments?.getFloat("amount") ?: 0.0f
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                ReceiptScreen(navController, amount.toDouble(), orderId)
            }

        }
    }
}

object GlobalNavigation {
    lateinit var navController: NavController
    var pendingOrderTotal: Double = 0.0
}