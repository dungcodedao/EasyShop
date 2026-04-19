package com.example.easyshop.components.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.util.ImageSaver
import kotlinx.coroutines.launch

@Composable
fun MBBankQRSection(
    totalAmount: Double,
    paymentReference: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Nhận paymentReference từ bên ngoài (thẩm thấu từ ViewModel)
    val orderId = paymentReference.ifEmpty { "ES-" + System.currentTimeMillis().toString().takeLast(6) }

    // --- CẤU HÌNH TÀI KHOẢN ---
    val bankId = "MB"
    val accountNo = "0969690132" // TODO: Thay đổi số tài khoản MBBank của bạn
    val accountName = "NGO VAN DUNG"

    // Link VietQR chuẩn
    val qrUrl = "https://img.vietqr.io/image/$bankId-$accountNo-compact2.png" +
            "?amount=${totalAmount.toLong()}" +
            "&addInfo=${android.net.Uri.encode(orderId)}" +
            "&accountName=${android.net.Uri.encode(accountName)}"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0038A8)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MB", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ngân hàng MBBank", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(accountNo, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    Text(accountName, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                }
                Surface(color = Color(0xFF0038A8).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(AppUtil.formatPrice(totalAmount), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontWeight = FontWeight.Bold, color = Color(0xFF0038A8), fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF0038A8).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(qrUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "QR MBBank",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        AppUtil.showToast(context, "Đang tải mã QR...")
                        val success = ImageSaver.saveQrToGallery(context, qrUrl)
                        if (success) {
                            AppUtil.showToast(context, context.getString(R.string.save_qr_msg))
                        } else {
                            AppUtil.showToast(context, "Lỗi khi lưu ảnh, vui lòng thử lại")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0038A8))
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF0038A8))
                Spacer(Modifier.width(8.dp))
                Text("Lưu mã", color = Color(0xFF0038A8))
            }

            Button(
                onClick = {
                    AppUtil.showToast(context, "Mở ứng dụng MB Bank...")
                    // MBBank app scheme (nếu có)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0038A8))
            ) {
                Text("Mở app MBBank", color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🏦 Hướng dẫn chuyển khoản", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0038A8))
                Text("Bước 1: Quét mã QR hoặc chuyển khoản theo số tài khoản trên.", fontSize = 12.sp, color = Color.DarkGray)
                Text("Bước 2: Đảm bảo nội dung chuyển khoản khớp với mã đơn hàng.", fontSize = 12.sp, color = Color.DarkGray)
                Text("Bước 3: Chờ 1-2 phút hệ thống SePay sẽ tự động xác nhận.", fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    }
}
