package com.example.easyshop.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.easyshop.util.clickableOnce
import com.example.easyshop.util.rememberDebouncedClick

@Composable
fun SignupScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var adminCode by remember { mutableStateOf("") }
    var isAdminFieldVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun isEmailValid(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun isPasswordValid(pwd: String) = pwd.length >= 6 && pwd.any { it.isLetter() } && pwd.any { it.isDigit() }

    val isPasswordMatch = password == confirmPassword
    val isFormValid = isEmailValid(email) && name.trim().isNotBlank() && isPasswordValid(password) && isPasswordMatch

    val primaryIndigo = Color(0xFF4F46E5)

    Box(
        modifier = modifier
            .fillMaxSize()
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Header
            Text(
                text = stringResource(id = R.string.hello_there),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.create_account_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(24.dp))

            // Banner
            Image(
                painter = painterResource(id = R.drawable.login_banner),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.5f).height(130.dp)
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
                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text(stringResource(id = R.string.email_address)) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = primaryIndigo) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = email.isNotEmpty() && !isEmailValid(email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryIndigo,
                            focusedLabelColor = primaryIndigo
                        )
                    )

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(id = R.string.full_name)) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = primaryIndigo) },
                        singleLine = true,
                        isError = name.isNotEmpty() && name.trim().isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryIndigo,
                            focusedLabelColor = primaryIndigo
                        )
                    )

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(id = R.string.password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = primaryIndigo) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = if (passwordVisible) primaryIndigo else Color.Gray)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = password.isNotEmpty() && !isPasswordValid(password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryIndigo,
                            focusedLabelColor = primaryIndigo
                        )
                    )

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(id = R.string.confirm_password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = primaryIndigo) },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = if (confirmPasswordVisible) primaryIndigo else Color.Gray)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = confirmPassword.isNotEmpty() && !isPasswordMatch,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryIndigo,
                            focusedLabelColor = primaryIndigo
                        )
                    )

                    // Admin Code Toggle
                    Text(
                        text = if (isAdminFieldVisible) "Tôi là khách hàng" else "Bạn là Quản trị viên?",
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickableOnce { isAdminFieldVisible = !isAdminFieldVisible },
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryIndigo,
                        fontWeight = FontWeight.Bold
                    )

                    if (isAdminFieldVisible) {
                        OutlinedTextField(
                            value = adminCode,
                            onValueChange = { adminCode = it },
                            label = { Text("Mã xác thực Admin") },
                            leadingIcon = { Icon(Icons.Default.Shield, null, tint = primaryIndigo) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryIndigo,
                                focusedLabelColor = primaryIndigo
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    isLoading = true
                    val cleanedName = name.trim().replace(Regex("\\s+"), " ")
                    val cleanedEmail = email.trim().lowercase()
                    authViewModel.signup(cleanedEmail, cleanedName, password, adminCode) { success, errorMessage, role ->
                        isLoading = false
                        if (success) {
                            val destination = if (role == "admin") "admin-dashboard" else "home"
                            navController.navigate(destination) { popUpTo("auth") { inclusive = true } }
                        } else {
                            AppUtil.showToast(context, errorMessage ?: context.getString(R.string.something_went_wrong))
                        }
                    }
                },
                enabled = !isLoading && isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryIndigo)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = stringResource(id = R.string.create_account_btn),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(id = R.string.already_have_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = " ${stringResource(id = R.string.login_btn)}",
                    modifier = Modifier.clickableOnce { navController.navigateUp() },
                    color = primaryIndigo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
