package com.example.easyshop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.easyshop.util.GlobalNavigation
import androidx.compose.runtime.collectAsState
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
import com.example.easyshop.admin.AdminDashboardScreen
import com.example.easyshop.admin.AdminHomeScreen
import com.example.easyshop.admin.AdminNotificationScreen
import com.example.easyshop.admin.AdminProfileScreen
import com.example.easyshop.admin.AnalyticsScreen
import com.example.easyshop.admin.ManageCategoriesScreen
import com.example.easyshop.admin.ManagePromoCodesScreen
import com.example.easyshop.admin.ManageUsersScreen
import com.example.easyshop.admin.OrdersManagementScreen
import com.example.easyshop.ai.ui.AIChatScreen
import com.example.easyshop.model.UserModel
import com.example.easyshop.components.LoadingView
import com.example.easyshop.pages.AddProductPage
import com.example.easyshop.pages.CartPage
import com.example.easyshop.pages.CategoryProductsPage
import com.example.easyshop.pages.CheckoutPage
import com.example.easyshop.pages.EditProductPage
import com.example.easyshop.pages.NotificationsPage
import com.example.easyshop.pages.OrderDetailsPage
import com.example.easyshop.pages.OrdersPage
import com.example.easyshop.pages.ProductDetailsPage
import com.example.easyshop.pages.ProfilePage
import com.example.easyshop.screen.AuthScreen
import com.example.easyshop.screen.HomeScreen
import com.example.easyshop.screen.LoginScreen
import com.example.easyshop.screen.PaymentScreen
import com.example.easyshop.screen.OnboardingScreen
import com.example.easyshop.screen.ReceiptScreen
import com.example.easyshop.screen.SignupScreen
import com.example.easyshop.screen.SplashScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore


@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    GlobalNavigation.navController = navController

    val context = androidx.compose.ui.platform.LocalContext.current
    val connectivityObserver = remember {
        com.example.easyshop.util.NetworkConnectivityObserver(context)
    }
    val networkStatus by connectivityObserver.observe()
        .collectAsState(initial = com.example.easyshop.util.ConnectivityObserver.Status.Available)

    val startDestination = "splash"

    com.example.easyshop.components.ResponsiveContainer {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = startDestination) {

                composable("splash") {
                    SplashScreen(navController)
                }

                composable("onboarding") {
                    OnboardingScreen(navController)
                }

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

            composable("notifications") {
                NotificationsPage(navController)
            }

            composable(
                route = "order-details/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                OrderDetailsPage(modifier, orderId)
            }

            composable(
                route = "payment/{totalAmount}/{subtotal}/{discount}/{promoCode}",
                arguments = listOf(
                    navArgument("totalAmount") { type = NavType.FloatType },
                    navArgument("subtotal") { type = NavType.FloatType },
                    navArgument("discount") { type = NavType.FloatType },
                    navArgument("promoCode") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val totalAmount = backStackEntry.arguments?.getFloat("totalAmount") ?: 0.0f
                val subtotal = backStackEntry.arguments?.getFloat("subtotal") ?: 0.0f
                val discount = backStackEntry.arguments?.getFloat("discount") ?: 0.0f
                val promoCode = backStackEntry.arguments?.getString("promoCode") ?: ""
                
                PaymentScreen(
                    modifier = modifier,
                    navController = navController,
                    totalAmount = totalAmount.toDouble(),
                    subtotal = subtotal.toDouble(),
                    discount = discount.toDouble(),
                    promoCode = promoCode
                )
            }


            // Admin Dashboard - Main screen
            composable("admin-dashboard") {
                AdminDashboardScreen(navController = navController)
            }

            // Admin Notifications
            composable("admin-notifications") {
                AdminNotificationScreen(navController = navController)
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

            // 🛡️ Admin Profile (Trang riêng cho Quản trị viên)
            composable("admin_profile") {
                AdminProfileScreen(navController = navController)
            }

            // User Profile (Giữ lại nếu cần cho luồng khác)
            composable("profile") {
                ProfilePage(modifier, navController)
            }
            }

            // 🔔 In-app notification banner (kiểu Shopee/MoMo) — hiện trên mọi màn hình
            com.example.easyshop.components.NotifBannerOverlay()
            // 🔔 Snackbar thông báo hành động
            com.example.easyshop.components.AppSnackbarHost()
            
            // 🌐 Thanh thông báo trạng thái mạng (Cố định ở trên cùng)
            com.example.easyshop.components.NetworkStatusBanner(status = networkStatus)
        }
    }
}
