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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.util.ImageSaver
import kotlinx.coroutines.launch

@Composable
fun MoMoQRSection(totalAmount: Double) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bankId = "MOMO"
    val accountNo = "0969690132"
    val accountName = "NGO VAN DUNG"
    val orderId = remember { "EasyShop-" + System.currentTimeMillis().toString().takeLast(6) }
    val saveQrMsg = stringResource(id = R.string.save_qr_msg)

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
                    Text("Ví điện tử MoMo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(accountNo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(accountName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Surface(color = Color(0xFFAE2070).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(AppUtil.formatPrice(totalAmount), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontWeight = FontWeight.Bold, color = Color(0xFFAE2070), fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

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
                            AppUtil.showToast(context, saveQrMsg)
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
                Text("Cách 1: Nhấn 'Mở ví MoMo' để thanh toán nhanh.", fontSize = 12.sp, color = Color.DarkGray)
                Text("Cách 2: Quét mã QR bằng ứng dụng MoMo của bạn.", fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    }
}

private fun openMoMoApp(context: android.content.Context, phone: String, amount: Long, note: String) {
    val momoLink = "momo://app/transfer?phone=$phone&amount=$amount&note=${android.net.Uri.encode(note)}"
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(momoLink))

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Máy chưa cài ứng dụng MoMo", android.widget.Toast.LENGTH_SHORT).show()
    }
}
