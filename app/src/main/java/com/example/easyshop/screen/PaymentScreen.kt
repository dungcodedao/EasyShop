package com.example.easyshop.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.easyshop.AppUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    totalAmount: Double = 0.0
) {
    var selectedPaymentMethod by remember { mutableStateOf("Credit Card") }
    var cardNumber by remember { mutableStateOf("") }
    var cardName by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Payment",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.size(40.dp))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            // Header

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Complete your purchase securely",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Order Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount", fontWeight = FontWeight.Medium)
                        Text(
                            "$${"%.2f".format(totalAmount)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Payment Methods
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            PaymentMethodSelector(
                selectedMethod = selectedPaymentMethod,
                onMethodSelected = { selectedPaymentMethod = it }
            )

            Spacer(Modifier.height(24.dp))

            // Card Details (if Credit Card selected)
            if (selectedPaymentMethod == "Credit Card (Mock)") {
                Text(
                    text = "Card Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )


                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = {
                        if (it.length <= 19) cardNumber = formatCardNumber(it)
                    },
                    label = { Text("Card Number") },
                    placeholder = { Text("1234 5678 9012 3456") },
                    leadingIcon = {
                        Icon(Icons.Default.CreditCard, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Cardholder Name") },
                    placeholder = { Text("JOHN DOE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = {
                            if (it.length <= 5) expiryDate = formatExpiryDate(it)
                        },
                        label = { Text("Expiry") },
                        placeholder = { Text("MM/YY") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = cvv,
                        onValueChange = {
                            if (it.length <= 3 && it.all { c -> c.isDigit() }) cvv = it
                        },
                        label = { Text("CVV") },
                        placeholder = { Text("123") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Razorpay Info
            if (selectedPaymentMethod == "Razorpay (Real)") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "💳 Real Payment Gateway",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "You will be redirected to Razorpay's secure payment page to complete your transaction.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Pay Button
            Button(
                onClick = {
                    when (selectedPaymentMethod) {
                        "Credit Card (Mock)" -> {
                            // Mock Payment Logic
                            scope.launch {
                                isProcessing = true
                                delay(2000)

                                val isSuccess = cardNumber.replace(" ", "") == "4111111111111111"
                                isProcessing = false

                                if (isSuccess) {
                                    AppUtil.clearCartAndAddToOrder(totalAmount, selectedPaymentMethod)
                                    showSuccessDialog = true
                                } else {
                                    AppUtil.showToast(context, "Payment declined. Use test card 4111 1111 1111 1111")
                                }
                            }
                        }
                        "Razorpay (Real)" -> {
                            // Razorpay Real Payment
                            AppUtil.startPayment(context, totalAmount.toFloat(), useMockPayment = false)
                        }
                        else -> {
                            // PayPal, Cash on Delivery
                            scope.launch {
                                isProcessing = true
                                delay(2000)
                                isProcessing = false
                                AppUtil.clearCartAndAddToOrder(totalAmount, selectedPaymentMethod)
                                showSuccessDialog = true
                            }
                        }
                    }
                },
                enabled = !isProcessing && isFormValid(selectedPaymentMethod, cardNumber, cardName, expiryDate, cvv),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Pay $${"%.2f".format(totalAmount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Security Notice
            Text(
                text = if (selectedPaymentMethod == "Razorpay (Real)")
                    "🔒 Secure payment powered by Razorpay"
                else
                    "🔒 This is a demo payment system. No real transactions are processed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        PaymentSuccessDialog(
            amount = totalAmount,
            onDismiss = {
                showSuccessDialog = false
                navController.navigate("home") {
                    popUpTo("payment") { inclusive = true }
                }
            }
        )
    }
}

@Composable
private fun PaymentMethodSelector(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit
) {
    val methods = listOf(
        "Credit Card (Mock)",
        "Razorpay (Real)",
        "PayPal",
        "Cash on Delivery"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        methods.forEach { method ->
            Card(
                onClick = { onMethodSelected(method) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedMethod == method) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = if (selectedMethod == method) {
                    CardDefaults.outlinedCardBorder().copy(
                        width = 2.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            MaterialTheme.colorScheme.primary
                        )
                    )
                } else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = method,
                            fontWeight = if (selectedMethod == method) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            }
                        )
                        if (method == "Credit Card (Mock)") {
                            Text(
                                text = "For testing only",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (method == "Razorpay (Real)") {
                            Text(
                                text = "Real payment gateway",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    RadioButton(
                        selected = selectedMethod == method,
                        onClick = { onMethodSelected(method) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentSuccessDialog(
    amount: Double,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text("✅", fontSize = 48.sp)
        },
        title = {
            Text("Payment Successful!")
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your payment of $${"%.2f".format(amount)} has been processed.")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Order ID: #${System.currentTimeMillis().toString().takeLast(6)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Continue Shopping")
            }
        }
    )
}

// Helper functions
private fun formatCardNumber(input: String): String {
    val digits = input.replace(" ", "").filter { it.isDigit() }
    return digits.chunked(4).joinToString(" ")
}

private fun formatExpiryDate(input: String): String {
    val digits = input.replace("/", "").filter { it.isDigit() }
    return if (digits.length >= 2) {
        "${digits.take(2)}/${digits.drop(2).take(2)}"
    } else {
        digits
    }
}

private fun isFormValid(
    method: String,
    cardNumber: String,
    cardName: String,
    expiryDate: String,
    cvv: String
): Boolean {
    return when (method) {
        "Credit Card (Mock)" -> {
            cardNumber.replace(" ", "").length == 16 &&
                    cardName.isNotBlank() &&
                    expiryDate.length == 5 &&
                    cvv.length == 3
        }
        else -> true
    }
}