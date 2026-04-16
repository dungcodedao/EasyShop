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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.components.VirtualCreditCard
import com.example.easyshop.util.ConnectivityObserver
import com.example.easyshop.util.ImageSaver
import com.example.easyshop.util.NetworkConnectivityObserver
import com.example.easyshop.viewmodel.CheckoutResult
import com.example.easyshop.viewmodel.CheckoutViewModel
import kotlinx.coroutines.launch

@Composable
fun PaymentScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    totalAmount: Double = 0.0,
    subtotal: Double = 0.0,
    discount: Double = 0.0,
    promoCode: String = "",
    viewModel: CheckoutViewModel = viewModel()
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
    
    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val networkStatus by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Available)
    val isNetworkAvailable = networkStatus == ConnectivityObserver.Status.Available

    val checkoutResult by viewModel.checkoutResult.collectAsState()
    val selectedAddress by viewModel.selectedAddress.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchData()
        // Nếu chuyển từ Checkout sang, ta cần đảm bảo address đã được set
        // Hoặc ViewModel tự lo trong fetchData (lấy default)
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

            // Payment Methods
            Text(text = stringResource(R.string.payment_method), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            PaymentMethodSelector(selectedMethod = selectedPaymentMethod, onMethodSelected = { selectedPaymentMethod = it })

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
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (!isNetworkAvailable) return@Button
                    when (selectedPaymentMethod) {
                        "MoMo QR" -> viewModel.placeOrder("MoMo QR")
                        "Credit Card" -> {
                            val isSuccess = cardNumber.replace(" ", "") == "4111111111111111"
                            if (isSuccess) viewModel.placeOrder("Credit Card")
                            else AppUtil.showToast(context, context.getString(R.string.test_card_hint))
                        }
                        else -> viewModel.placeOrder(selectedPaymentMethod)
                    }
                },
                enabled = !isProcessing && isNetworkAvailable && isFormValid(selectedPaymentMethod, cardNumber, cardName, expiryDate, cvv),
                modifier = Modifier.width(260.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text(text = stringResource(R.string.pay_with_amount, AppUtil.formatPrice(totalAmount)), fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
        "MoMo QR" to "🟣",
        "Credit Card" to "💳",
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        // --- Phần thông tin tài khoản (Card cũ giữ nguyên) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFAE2070)),
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
                Surface(color = Color(0xFFAE2070).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(AppUtil.formatPrice(totalAmount), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontWeight = FontWeight.Bold, color = Color(0xFFAE2070), fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Phần ảnh QR ---
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

        Spacer(Modifier.height(16.dp))

        // --- HÀNG NÚT MỚI THÊM VÀO ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nút Lưu Mã
            OutlinedButton(
                onClick = {
                    scope.launch {
                        AppUtil.showToast(context, "Đang tải mã QR...")
                        val success = ImageSaver.saveQrToGallery(context, qrUrl)
                        if (success) {
                            AppUtil.showToast(context, "Đã lưu mã QR vào máy")
                        } else {
                            AppUtil.showToast(context, "Lỗi khi lưu ảnh, vui lòng thử lại")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFAE2070))
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFAE2070))
                Spacer(Modifier.width(8.dp))
                Text("Lưu mã", color = Color(0xFFAE2070))
            }

            // Nút Mở Ví MoMo
            Button(
                onClick = {
                    openMoMoApp(context, accountNo, totalAmount.toLong(), orderId)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAE2070))
            ) {
                Text("Mở ví MoMo", color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Hướng dẫn (Sửa lại nội dung cho khớp) ---
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
                Text("Cách 1: Nhấn 'Mở ví MoMo' để thanh toán nhanh trên máy này.", fontSize = 12.sp, color = Color.DarkGray)
                Text("Cách 2: Lưu mã QR hoặc dùng máy khác quét mã bên trên.", fontSize = 12.sp, color = Color.DarkGray)
                Text("Sau khi thanh toán, quay lại đây nhấn 'Đã thanh toán'.", fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    }
}

private fun openMoMoApp(context: android.content.Context, phone: String, amount: Long, note: String) {
    // Deep link MoMo để chuyển tiền
    val momoLink = "momo://app/transfer?phone=$phone&amount=$amount&note=${android.net.Uri.encode(note)}"
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(momoLink))

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Nếu không có app MoMo thì báo lỗi hoặc mở Store
        android.widget.Toast.makeText(context, "Máy chưa cài ứng dụng MoMo", android.widget.Toast.LENGTH_SHORT).show()
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
        "PayPal", "Cash on Delivery", "MoMo QR" -> true
        else -> false
    }
}
