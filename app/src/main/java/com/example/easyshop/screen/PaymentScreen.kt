package com.example.easyshop.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.components.VirtualCreditCard
import com.example.easyshop.components.payment.MBBankQRSection
import com.example.easyshop.components.payment.MoMoQRSection
import com.example.easyshop.util.ConnectivityObserver
import com.example.easyshop.util.NetworkConnectivityObserver
import com.example.easyshop.viewmodel.CheckoutResult
import com.example.easyshop.viewmodel.CheckoutViewModel

@Composable
fun PaymentScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    totalAmount: Double = 0.0,
    subtotal: Double = 0.0,
    discount: Double = 0.0,
    promoCode: String = "",
    note: String = "",
    viewModel: CheckoutViewModel = viewModel()
) {
    var selectedPaymentMethod by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardName by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Đưa chuỗi ra ngoài Composable để tránh lỗi IDE trong lambda
    val testCardHint = stringResource(R.string.test_card_hint)
    val paymentLabel = stringResource(R.string.payment)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val networkStatus by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Available)
    val isNetworkAvailable = networkStatus == ConnectivityObserver.Status.Available

    val checkoutResult by viewModel.checkoutResult.collectAsState()
    val selectedAddress by viewModel.selectedAddress.collectAsState()
    val paymentReference by viewModel.paymentReference.collectAsState()
    val isPaymentChecking by viewModel.isPaymentChecking.collectAsState()
    val isPaymentConfirmed by viewModel.isPaymentConfirmed.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchData()
        if (promoCode.isNotEmpty() && discount > 0) {
            viewModel.setDiscountInfo(promoCode, discount)
        }
    }

    // Tự động bật/tắt quét thanh toán SePay
    LaunchedEffect(selectedPaymentMethod) {
        if (selectedPaymentMethod == "MB") {
            viewModel.startPaymentPolling(totalAmount)
        } else {
            viewModel.stopPaymentPolling()
        }
    }

    LaunchedEffect(checkoutResult) {
        when (val result = checkoutResult) {
            is CheckoutResult.Success -> {
                isProcessing = false
                navController.navigate("receipt/${totalAmount.toFloat()}/${result.orderId}") {
                    popUpTo("payment") { inclusive = true }
                }
            }
            is CheckoutResult.Error -> {
                isProcessing = false
                AppUtil.showError("Lỗi thanh toán", result.message)
            }
            is CheckoutResult.Loading -> {
                isProcessing = true
            }
            else -> {}
        }
    }

    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
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
                .weight(1f)
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.payable_amount), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(AppUtil.formatPrice(totalAmount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(text = stringResource(R.string.secure), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // MBBank QR Section
            if (selectedPaymentMethod == "MB") {
                MBBankQRSection(totalAmount = totalAmount, paymentReference = paymentReference)
                Spacer(Modifier.height(24.dp))
            }

            // MoMo QR Section
            if (selectedPaymentMethod == "MoMo QR") {
                MoMoQRSection(totalAmount = totalAmount)
                Spacer(Modifier.height(24.dp))
            }

            // Virtual Card Section
            if (selectedPaymentMethod == "Credit Card") {
                VirtualCreditCard(number = cardNumber, name = cardName, expiry = expiryDate, cvv = cvv, isVisa = cardNumber.startsWith("4") || cardNumber.isEmpty())
                Spacer(Modifier.height(24.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Chế độ hiển thị linh hoạt: Ẩn danh sách khi đang quét QR
            if (selectedPaymentMethod != "MB" && selectedPaymentMethod != "MoMo QR") {
                Text(text = stringResource(R.string.payment_method), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                PaymentMethodSelector(selectedMethod = selectedPaymentMethod, onMethodSelected = { selectedPaymentMethod = it })
            } else {
                // Hiển thị nút thay đổi nếu muốn chọn lại
                OutlinedButton(
                    onClick = { selectedPaymentMethod = "" },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Text("Thay đổi phương thức thanh toán", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Card Details (if Credit Card selected)
            if (selectedPaymentMethod == "Credit Card") {
                Text(text = stringResource(R.string.card_details), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = cardNumber, onValueChange = { if (it.length <= 19) cardNumber = formatCardNumber(it) }, label = { Text(stringResource(R.string.card_number)) }, placeholder = { Text("1234 5678 9012 3456") }, leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = cardName, onValueChange = { cardName = it }, label = { Text(stringResource(R.string.card_holder_name)) }, placeholder = { Text("JOHN DOE") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = expiryDate, onValueChange = { if (it.length <= 5) expiryDate = formatExpiryDate(it) }, label = { Text(stringResource(R.string.expiry)) }, placeholder = { Text("MM/YY") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = cvv, onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) cvv = it }, label = { Text(stringResource(R.string.cvv)) }, placeholder = { Text("123") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // --- Pinned Footer ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Luôn hiện nút thanh toán khi đã chọn phương thức
            if (selectedPaymentMethod != "") {
                Button(
                    onClick = {
                        if (!isNetworkAvailable) return@Button
                        when (selectedPaymentMethod) {
                            "MB" -> {
                                if (isPaymentConfirmed) {
                                    viewModel.placeOrder(selectedPaymentMethod, note)
                                } else {
                                    // Chủ động kiểm tra thanh toán ngay lập tức
                                    viewModel.verifySePayPayment(totalAmount)
                                }
                            }
                            "MoMo QR" -> viewModel.placeOrder(selectedPaymentMethod, note)
                            "Credit Card" -> {
                                val isSuccess = cardNumber.replace(" ", "") == "4111111111111111"
                                if (isSuccess) viewModel.placeOrder("Credit Card", note)
                                else AppUtil.showToast(context, testCardHint)
                            }
                            else -> viewModel.placeOrder(selectedPaymentMethod, note)
                        }
                    },
                    enabled = !isProcessing && !isPaymentChecking && isNetworkAvailable && 
                        isFormValid(selectedPaymentMethod, cardNumber, cardName, expiryDate, cvv),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPaymentMethod == "MB" && !isPaymentConfirmed) 
                            Color(0xFF6366F1) else Color(0xFF4F46E5),
                        disabledContainerColor = Color(0xFFE2E8F0),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isProcessing || isPaymentChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        val buttonText = when (selectedPaymentMethod) {
                            "MB" -> if (isPaymentConfirmed) "Đã nhận tiền - Nhấn để Hoàn tất" else "Kiểm tra thanh toán"
                            "MoMo QR" -> "Xác nhận đã chuyển tiền"
                            else -> paymentLabel + " " + AppUtil.formatPrice(totalAmount)
                        }
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!isNetworkAvailable) {
                Text(text = stringResource(R.string.lost_internet), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp).fillMaxWidth(), textAlign = TextAlign.Center)
            }
            
            Spacer(Modifier.height(8.dp))
            Text(text = stringResource(R.string.secure_notice_mock), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
        }
    }

}

@Composable
private fun PaymentMethodSelector(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit
) {
    val methods = listOf(
        Triple("MB", "https://vietqr.net/portal-static/img/bank_logo/mbb.png", "Ngân hàng MB"),
        Triple("MoMo QR", "https://img.vietqr.io/image/momo.png", "Ví điện tử MoMo"),
        Triple("Credit Card", "💳", "Thẻ quốc tế (Visa/Master)"),
        Triple("Cash on Delivery", "💵", "Thanh toán khi nhận hàng")
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        methods.forEach { (method, logo, subtitle) ->
            val isSelected = selectedMethod == method
            Surface(
                onClick = { onMethodSelected(method) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF4F46E5) else Color.LightGray.copy(alpha = 0.5f)
                ),
                tonalElevation = if (isSelected) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thiết kế Icon "xịn" tự vẽ
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (method) {
                                    "MB" -> Color(0xFF003BAB) // Màu xanh MB chuẩn
                                    "MoMo QR" -> Color(0xFFD82D8B) // Màu hồng MoMo chuẩn
                                    else -> Color.White
                                }
                            )
                            .then(
                                if (method != "MB" && method != "MoMo QR") 
                                    Modifier.border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (method) {
                            "MB" -> {
                                Text(
                                    text = "MB",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            "MoMo QR" -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "mo",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp
                                    )
                                    Text(
                                        text = "mo",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                            else -> {
                                Text(text = logo.toString(), fontSize = 24.sp)
                            }
                        }
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = method,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF1F2937) else Color.Unspecified
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    
                    RadioButton(
                        selected = isSelected,
                        onClick = { onMethodSelected(method) },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF4F46E5)
                        )
                    )
                }
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
        "PayPal", "Cash on Delivery", "MoMo QR", "MB" -> true
        else -> false
    }
}
