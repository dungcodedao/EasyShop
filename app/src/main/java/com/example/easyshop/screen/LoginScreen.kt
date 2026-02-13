package com.example.easyshop.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isFormValid = email.isNotBlank() && password.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Banner Image
            Image(
                painter = painterResource(id = R.drawable.login_banner),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(160.dp)
            )

            Spacer(Modifier.height(32.dp))

            // Title
            Text(
                text = stringResource(id = R.string.welcome_back),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.sign_in_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(id = R.string.email_address)) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(id = R.string.password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Login Button
            Button(
                onClick = {
                    isLoading = true
                    authViewModel.login(email, password) { success, errorMessage, role ->
                        isLoading = false
                        if (success) {
                            val destination = if (role == "admin") "admin-dashboard" else "home"
                            AppUtil.showToast(context, if (role == "admin") context.getString(R.string.welcome_admin_msg) else context.getString(R.string.login_success_msg))
                            navController.navigate(destination) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            AppUtil.showToast(context, errorMessage ?: context.getString(R.string.login_failed))
                        }
                    }
                },
                enabled = !isLoading && isFormValid,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        text = stringResource(id = R.string.login_btn),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.no_account), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { navController.navigate("signup") }) {
                    Text(
                        stringResource(id = R.string.signup_link),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}