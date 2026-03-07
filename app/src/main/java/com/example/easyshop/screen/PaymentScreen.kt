package com.example.easyshop.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.components.VirtualCreditCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PaymentScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    totalAmount: Double = 0.0
) {
    var selectedPaymentMethod by remember { mutableStateOf("") }
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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.payment),
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
                text = stringResource(R.string.payment_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Order Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.payable_amount),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            AppUtil.formatPrice(totalAmount),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.secure),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // MoMo QR Section
            if (selectedPaymentMethod == "MoMo QR") {
                MoMoQRSection(totalAmount = totalAmount)
                Spacer(Modifier.height(24.dp))
            }

            // Virtual Card Section
            if (selectedPaymentMethod == "Credit Card") {
                VirtualCreditCard(
                    number = cardNumber,
                    name = cardName,
                    expiry = expiryDate,
                    cvv = cvv,
                    isVisa = cardNumber.startsWith("4") || cardNumber.isEmpty()
                )
                Spacer(Modifier.height(24.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Payment Methods
            Text(
                text = stringResource(R.string.payment_method),
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
            if (selectedPaymentMethod == "Credit Card") {
                Text(
                    text = stringResource(R.string.card_details),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )


                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = {
                        if (it.length <= 19) cardNumber = formatCardNumber(it)
                    },
                    label = { Text(stringResource(R.string.card_number)) },
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
                    label = { Text(stringResource(R.string.card_holder_name)) },
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
                        label = { Text(stringResource(R.string.expiry)) },
                        placeholder = { Text("MM/YY") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = cvv,
                        onValueChange = {
                            if (it.length <= 3 && it.all { c -> c.isDigit() }) cvv = it
                        },
                        label = { Text(stringResource(R.string.cvv)) },
                        placeholder = { Text("123") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Razorpay Info
            if (selectedPaymentMethod == "Razorpay") {
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
                            text = stringResource(R.string.redirect_notice),
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
                        "MoMo QR" -> {
                    scope.launch {
                        isProcessing = true
                        delay(1500)
                        isProcessing = false
                        val orderId = "ORD" + java.util.UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
                        AppUtil.clearCartAndAddToOrder(totalAmount, "MoMo QR", orderId)
                        navController.navigate("receipt/${totalAmount.toFloat()}/$orderId") {
                            popUpTo("payment") { inclusive = true }
                        }
                    }
                }
                "Credit Card" -> {
                            // Mock Payment Logic
                            scope.launch {
                                isProcessing = true
                                delay(2000)

                                val isSuccess = cardNumber.replace(" ", "") == "4111111111111111"
                                isProcessing = false

                                if (isSuccess) {
                                    val orderId = "ORD" + java.util.UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
                                    AppUtil.clearCartAndAddToOrder(totalAmount, selectedPaymentMethod, orderId)
                                    navController.navigate("receipt/${totalAmount.toFloat()}/$orderId") {
                                        popUpTo("payment") { inclusive = true }
                                    }
                                } else {
                                    AppUtil.showToast(context, context.getString(R.string.test_card_hint))
                                }
                            }
                        }
                        "Razorpay" -> {
                            // Razorpay Real Payment
                            AppUtil.startPayment(context, totalAmount.toFloat(), useMockPayment = false)
                        }
                        else -> {
                            // PayPal, Cash on Delivery
                            scope.launch {
                                isProcessing = true
                                delay(2000)
                                isProcessing = false
                                val orderId = "ORD" + java.util.UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
                                AppUtil.clearCartAndAddToOrder(totalAmount, selectedPaymentMethod, orderId)
                                navController.navigate("receipt/${totalAmount.toFloat()}/$orderId") {
                                    popUpTo("payment") { inclusive = true }
                                }
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
                        text = stringResource(R.string.pay_with_amount, AppUtil.formatPrice(totalAmount)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Security Notice
            Text(
                text = if (selectedPaymentMethod == "Razorpay")
                    stringResource(R.string.secure_notice_real)
                else
                    stringResource(R.string.secure_notice_mock),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

}

@Composable
private fun PaymentMethodSelector(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit
) {
    val methods = listOf(
        "MoMo QR" to "🟣",
        "Credit Card" to "💳",
        "Razorpay" to "🚀",
        "PayPal" to "🅿️",
        "Cash on Delivery" to "💵"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        methods.forEach { (method, icon) ->
            val isSelected = selectedMethod == method
            Surface(
                onClick = { onMethodSelected(method) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) 
                        else MaterialTheme.colorScheme.surface,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) 
                         else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = icon, fontSize = 20.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = method,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                    if (isSelected) {
                        RadioButton(
                            selected = true,
                            onClick = { onMethodSelected(method) }
                        )
                    }
                }
            }
        }
    }
}

// ── VietQR MoMo QR Section ────────────────────────────────────────────────
@Composable
private fun MoMoQRSection(totalAmount: Double) {
    // Thông tin tài khoản MoMo
    val bankId = "MOMO"
    val accountNo = "0969690132"
    val accountName = "NGO VAN DUNG"
    val orderId = remember { "EasyShop-" + System.currentTimeMillis().toString().takeLast(6) }

    val qrUrl = "https://img.vietqr.io/image/$bankId-$accountNo-compact2.png" +
            "?amount=${totalAmount.toLong()}" +
            "&addInfo=${android.net.Uri.encode(orderId)}" +
            "&accountName=${android.net.Uri.encode(accountName)}"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF0F0F0)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MoMo logo badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFAE2070)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Mo\nMo", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 11.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("MoMo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(accountNo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(accountName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                // Amount badge
                Surface(
                    color = Color(0xFFAE2070).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        AppUtil.formatPrice(totalAmount),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFAE2070),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // QR Image (dynamic via VietQR)
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFFAE2070).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(qrUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "QR MoMo",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(12.dp))

        // Instruction
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F8))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("📲 Hướng dẫn thanh toán", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFAE2070))
                Text("1. Mở app MoMo → Chọn \"Quét QR\"", fontSize = 12.sp, color = Color.DarkGray)
                Text("2. Quét mã QR bên trên", fontSize = 12.sp, color = Color.DarkGray)
                Text("3. Số tiền ${AppUtil.formatPrice(totalAmount)} đã được điền sẵn", fontSize = 12.sp, color = Color.DarkGray)
                Text("4. Nhấn \"Xác nhận thanh toán\" trên app MoMo", fontSize = 12.sp, color = Color.DarkGray)
                Text("5. Quay lại đây và nhấn nút \"Đã thanh toán\"", fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    }
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
    if (method.isEmpty()) return false
    return when (method) {
        "Credit Card" -> {
            cardNumber.replace(" ", "").length == 16 &&
                    cardName.isNotBlank() &&
                    expiryDate.length == 5 &&
                    cvv.length == 3
        }
        "Razorpay", "PayPal", "Cash on Delivery", "MoMo QR" -> true
        else -> false
    }
}