package com.example.easyshop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.easyshop.model.UserModel
import com.example.easyshop.pages.AddProductPage
import com.example.easyshop.pages.CartPage
import com.example.easyshop.pages.CategoryProductsPage
import com.example.easyshop.pages.CheckoutPage
import com.example.easyshop.pages.EditProductPage
import com.example.easyshop.pages.OrdersPage
import com.example.easyshop.pages.ProductDetailsPage
import com.example.easyshop.screen.AdminHomeScreen
import com.example.easyshop.screen.PaymentScreen
import com.example.easyshop.screen.AuthScreen
import com.example.easyshop.screen.HomeScreen
import com.example.easyshop.screen.LoginScreen
import com.example.easyshop.screen.SignupScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

@Composable
fun AppNavigation(modifier: Modifier = Modifier){
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
                        "admin-home"
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

            // ✅ ADMIN ROUTES
            composable("admin-home") {
                AdminHomeScreen(modifier, navController)
            }

            composable("add-product") {
                AddProductPage(modifier, navController)
            }
            composable("add_product") {
                AddProductPage(navController = navController)
            }


            composable("category-products/{categoryId}") {
                val categoryId = it.arguments?.getString("categoryId")
                CategoryProductsPage(modifier, categoryId ?: "")
            }

            composable("product-details/{productId}") {
                val productId = it.arguments?.getString("productId")
                ProductDetailsPage(modifier, productId ?: "")
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


            // Route mới với productId
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
    }

    object GlobalNavigation{
        lateinit var navController : NavController
    }