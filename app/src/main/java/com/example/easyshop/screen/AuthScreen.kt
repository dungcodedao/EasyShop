package com.example.easyshop.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.easyshop.R

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(32.dp))

        // Banner Image
        Image(
            painter = painterResource(id = R.drawable.banner),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )

        Spacer(Modifier.height(32.dp))

        // Main Heading
        Text(
            text = "Start your shopping journey now",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        Spacer(Modifier.height(12.dp))

        // Subheading
        Text(
            text = "Best ecom platform with best prices",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        // Login Button
        Button(
            onClick = { navController.navigate("login") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Login",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))

        // Signup Button
        OutlinedButton(
            onClick = { navController.navigate("signup") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Sign Up",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))

        // Guest Mode Link
        TextButton(
            onClick = { navController.navigate("home") }
        ) {
            Text("Continue as Guest")
        }

        Spacer(Modifier.height(32.dp))
    }
}