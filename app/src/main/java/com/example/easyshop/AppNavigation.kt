package com.example.easyshop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.easyshop.screen.AuthScreen
import com.example.easyshop.screen.HomeScreen
import com.example.easyshop.screen.LoginScreen
import com.example.easyshop.screen.SignupScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier){
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "auth"){

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
            HomeScreen(modifier)
        }
    }
}