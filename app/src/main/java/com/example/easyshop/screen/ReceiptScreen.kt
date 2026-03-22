package com.example.easyshop.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.model.OrderModel
import com.example.easyshop.model.ProductModel
import com.example.easyshop.ui.components.DashedDivider
import com.example.easyshop.ui.components.SawtoothEdge
import com.example.easyshop.util.PrintHelper
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReceiptScreen(
    navController: NavController,
    amount: Double,
    orderId: String
) {
    val scrollState = rememberScrollState()
    val firestore = Firebase.firestore
    
    // Fetch Order Data
    val orderData by produceState<OrderModel?>(initialValue = null) {
        try {
            val doc = firestore.collection("orders").document(orderId).get().await()
            value = doc.toObject(OrderModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Fetch Products in Order
    val productsData by produceState<Map<String, ProductModel>>(initialValue = emptyMap(), orderData) {
        orderData?.items?.let { itemsMap ->
            val productIds = itemsMap.keys.toList()
            if (productIds.isNotEmpty()) {
                try {
                    val result = firestore.collection("data").document("stock").collection("products")
                        .whereIn("id", productIds)
                        .get()
                        .await()
                    value = result.documents.mapNotNull { it.toObject(ProductModel::class.java) }
                        .associateBy { it.id }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.invoice_title_promax), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            val context = LocalContext.current
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val currentOrder = orderData ?: return@OutlinedButton
                            val itemsHtml = currentOrder.items.map { (pid, qty) ->
                                val p = productsData[pid]
                                val priceValue = p?.actualPrice?.toDoubleOrNull() ?: p?.price?.toDoubleOrNull() ?: 0.0
                                """
                                <tr>
                                    <td>${p?.title ?: "Sản phẩm"}</td>
                                    <td style="text-align:center">$qty</td>
                                    <td style="text-align:right">${AppUtil.formatPrice(priceValue * qty)}</td>
                                </tr>
                                """.trimIndent()
                            }.joinToString("")

                            val html = PrintHelper.generateReceiptHtml(
                                orderId = currentOrder.id,
                                total = AppUtil.formatPrice(currentOrder.total),
                                subtotal = AppUtil.formatPrice(currentOrder.subtotal),
                                discount = AppUtil.formatPrice(currentOrder.discount),
                                date = AppUtil.formatData(currentOrder.date),
                                itemsHtml = itemsHtml
                            )
                            PrintHelper.printHtml(context, html, "EasyShop_Invoice_${currentOrder.id}")
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.dp)
                    ) {
                        Icon(Icons.Default.Print, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.print_save_pdf))
                    }

                    Button(
                        onClick = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Home, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.back_to_home))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF2F4F7)) // Màu nền nhẹ nhàng
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // The Main Receipt Paper
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.92f)
                    .shadow(12.dp, RoundedCornerShape(4.dp))
                    .background(Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Sawtooth
                    SawtoothEdge(color = Color(0xFFF2F4F7), isBottom = false)

                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Brand Identity
                        Text(
                            "EasyShop",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Chuyên linh kiện & Phụ kiện PC cao cấp",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            "www.easyshop.com.vn | Hotline: 1900 8888",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )

                        Spacer(Modifier.height(24.dp))
                        DashedDivider()
                        Spacer(Modifier.height(20.dp))

                        // Receipt Header
                        Text(
                            "HÓA ĐƠN BÁN LẺ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(16.dp))

                        // Metadata
                        InvoiceMetaRow("Mã đơn hàng", "#${orderId.uppercase()}")
                        InvoiceMetaRow("Ngày giờ", formatTimestamp(orderData?.date))
                        InvoiceMetaRow("Khách hàng", orderData?.userName ?: "Khách hàng lẻ")
                        InvoiceMetaRow("Thanh toán", orderData?.paymentMethod ?: "N/A")

                        Spacer(Modifier.height(24.dp))
                        DashedDivider()
                        Spacer(Modifier.height(20.dp))

                        // Items Table Header
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Sản phẩm", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text("SL", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                            Text("Thành tiền", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.width(90.dp), textAlign = TextAlign.End)
                        }
                        
                        Spacer(Modifier.height(12.dp))

                        // Items
                        orderData?.items?.forEach { (productId, qty) ->
                            val product = productsData[productId]
                            val price = product?.actualPrice?.toDoubleOrNull() ?: product?.price?.toDoubleOrNull() ?: 0.0
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    product?.title ?: "Sản phẩm ID: $productId",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                                Text(
                                    "x$qty",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(30.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    AppUtil.formatPrice(price * qty),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(90.dp),
                                    textAlign = TextAlign.End,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        DashedDivider()
                        Spacer(Modifier.height(20.dp))

                        // Summary
                        SummaryRow("Tạm tính", AppUtil.formatPrice(orderData?.subtotal ?: amount))
                        SummaryRow("Phí vận chuyển", "Miễn phí")
                        if ((orderData?.discount ?: 0.0) > 0) {
                            SummaryRow("Giảm giá Promo", "- ${AppUtil.formatPrice(orderData?.discount ?: 0.0)}")
                            if (orderData?.promoCode?.isNotEmpty() == true) {
                                Text(
                                    "Mã áp dụng: ${orderData?.promoCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TỔNG CỘNG", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text(
                                AppUtil.formatPrice(orderData?.total ?: amount),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(32.dp))

                        // QR & Footer
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Mock QR Code
                            Text("QR SCAN", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Cảm ơn quý khách đã mua hàng!",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Text(
                            "Vui lòng quét QR để tra cứu bảo hành điện tử",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }

                    // Bottom Sawtooth
                    SawtoothEdge(color = Color(0xFFF2F4F7), isBottom = true)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun InvoiceMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "N/A"
    return SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(timestamp.toDate())
}

