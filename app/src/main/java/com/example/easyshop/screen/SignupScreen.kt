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
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun isEmailValid(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun isPasswordValid(pwd: String) = pwd.length >= 6 && pwd.any { it.isLetter() } && pwd.any { it.isDigit() }

    val isPasswordMatch = password == confirmPassword
    val isFormValid = isEmailValid(email) && name.trim().isNotBlank() && isPasswordValid(password) && isPasswordMatch

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
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
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.create_account_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Banner
            Image(
                painter = painterResource(id = R.drawable.login_banner),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.55f).height(140.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text(stringResource(id = R.string.email_address)) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = email.isNotEmpty() && !isEmailValid(email),
                        supportingText = {
                            if (email.isNotEmpty() && !isEmailValid(email)) {
                                Text(stringResource(id = R.string.email_invalid_error), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(id = R.string.full_name)) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        isError = name.isNotEmpty() && name.trim().isBlank(),
                        supportingText = {
                            if (name.isNotEmpty() && name.trim().isBlank()) {
                                Text(stringResource(id = R.string.name_empty_error), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(id = R.string.password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = password.isNotEmpty() && !isPasswordValid(password),
                        supportingText = {
                            Text(
                                stringResource(id = R.string.password_format_error),
                                color = if (password.isNotEmpty() && !isPasswordValid(password)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(id = R.string.confirm_password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = confirmPassword.isNotEmpty() && !isPasswordMatch,
                        supportingText = {
                            if (confirmPassword.isNotEmpty() && !isPasswordMatch) {
                                Text(stringResource(id = R.string.password_mismatch_error), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Signup Button
            Button(
                onClick = {
                    if (isFormValid) {
                        isLoading = true
                        val cleanedName = name.trim().replace(Regex("\\s+"), " ")
                        val cleanedEmail = email.trim().lowercase()
                        authViewModel.signup(cleanedEmail, cleanedName, password) { success, errorMessage ->
                            isLoading = false
                            if (success) {
                                navController.navigate("home") { popUpTo("auth") { inclusive = true } }
                            } else {
                                AppUtil.showToast(context, errorMessage ?: context.getString(R.string.something_went_wrong))
                            }
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
                        text = stringResource(id = R.string.create_account_btn),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(id = R.string.already_have_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { navController.navigateUp() }) {
                    Text(
                        stringResource(id = R.string.login_btn),
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