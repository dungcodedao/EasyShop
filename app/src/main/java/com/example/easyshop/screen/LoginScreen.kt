package com.example.easyshop.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.util.clickableOnce
import com.example.easyshop.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

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
    val loginFailedMsg = stringResource(id = R.string.login_failed)
    
    val primaryIndigo = Color(0xFF4F46E5)

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primaryIndigo.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            Spacer(Modifier.height(16.dp))

            // Banner Image
            Image(
                painter = painterResource(id = R.drawable.login_banner),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(130.dp)
            )

            Spacer(Modifier.height(20.dp))

            // Title
            Text(
                text = stringResource(id = R.string.welcome_back),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.sign_in_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(24.dp))

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(id = R.string.email_address)) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = primaryIndigo) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryIndigo,
                            focusedLabelColor = primaryIndigo
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(id = R.string.password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = primaryIndigo) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = if (passwordVisible) primaryIndigo else Color.Gray
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryIndigo,
                            focusedLabelColor = primaryIndigo
                        )
                    )
                    
                    Text(
                        text = "Quên mật khẩu?",
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickableOnce {
                                if (email.isBlank()) {
                                    AppUtil.showToast(context, "Vui lòng nhập email")
                                } else {
                                    authViewModel.resetPassword(email) { success, _ ->
                                        AppUtil.showToast(context, if (success) "Đã gửi mail đặt lại mật khẩu" else "Lỗi gửi mail")
                                    }
                                }
                            },
                        color = primaryIndigo,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = {
                    isLoading = true
                    authViewModel.login(email, password) { success, errorMessage, role ->
                        isLoading = false
                        if (success) {
                            val destination = if (role == "admin") "admin-dashboard" else "home"
                            navController.navigate(destination) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            AppUtil.showToast(context, errorMessage ?: loginFailedMsg)
                        }
                    }
                },
                enabled = !isLoading && isFormValid,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryIndigo)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = stringResource(id = R.string.login_btn),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Divider
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Text(" Hoặc đăng nhập bằng ", modifier = Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }

            Spacer(Modifier.height(20.dp))

            // Google Login Button (Modern Style)
            val scope = rememberCoroutineScope()
            val credentialManager = CredentialManager.create(context)
            
            Surface(
                onClick = {
                    if (isLoading) return@Surface
                    scope.launch {
                        try {
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId("896897641883-mmo5cr61ksvndrq61g6j8bgenltkfm12.apps.googleusercontent.com")
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            val result = credentialManager.getCredential(context = context, request = request)
                            val credential = result.credential
                            if (credential is GoogleIdTokenCredential) {
                                isLoading = true
                                authViewModel.signInWithGoogle(credential.idToken) { success, error, role ->
                                    isLoading = false
                                    if (success) {
                                        val destination = if (role == "admin") "admin-dashboard" else "home"
                                        navController.navigate(destination) {
                                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        AppUtil.showToast(context, error ?: "Lỗi xác thực")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            AppUtil.showToast(context, "Lỗi: ${e.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(27.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                color = Color.White,
                tonalElevation = 1.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Tiếp tục với Google", style = MaterialTheme.typography.titleSmall, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.no_account), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(
                    text = " ${stringResource(id = R.string.signup_link)}",
                    modifier = Modifier.clickableOnce { navController.navigate("signup") },
                    color = primaryIndigo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(16.dp))
            }
        }
    }
}
