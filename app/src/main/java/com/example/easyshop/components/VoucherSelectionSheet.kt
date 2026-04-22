package com.example.easyshop.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.AppUtil
import com.example.easyshop.model.PromoCodeModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherSelectionSheet(
    vouchers: List<PromoCodeModel>,
    subtotal: Double,
    selectedCode: String,
    onDismissed: () -> Unit,
    onVoucherSelected: (PromoCodeModel) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissed,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Chọn Mã Giảm Giá",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (vouchers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(8.dp))
                        Text("Ví voucher trống", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val (validVouchers, invalidVouchers) = vouchers.partition { 
                        it.minOrder <= subtotal 
                    }

                    if (validVouchers.isNotEmpty()) {
                        item {
                            Text(
                                "Mã có thể áp dụng",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Gray
                            )
                        }
                        items(validVouchers) { voucher ->
                            VoucherItem(
                                voucher = voucher,
                                isSelected = selectedCode == voucher.code,
                                isEnabled = true,
                                onSelected = { onVoucherSelected(voucher) }
                            )
                        }
                    }

                    if (invalidVouchers.isNotEmpty()) {
                        item {
                            Text(
                                "Chưa đủ điều kiện",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                        items(invalidVouchers) { voucher ->
                            VoucherItem(
                                voucher = voucher,
                                isSelected = false,
                                isEnabled = false,
                                subtotal = subtotal,
                                onSelected = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoucherItem(
    voucher: PromoCodeModel,
    isSelected: Boolean,
    isEnabled: Boolean,
    subtotal: Double = 0.0,
    onSelected: () -> Unit
) {
    val primaryColor = Color(0xFF4F46E5)
    val opacity = if (isEnabled) 1f else 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) primaryColor.copy(alpha = 0.05f) else Color.White)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) primaryColor else Color.LightGray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = isEnabled, onClick = onSelected)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Discount Icon/Type
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isEnabled) primaryColor.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ConfirmationNumber,
                contentDescription = null,
                tint = if (isEnabled) primaryColor else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Middle: Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (voucher.type == "percentage") "Giảm ${voucher.value.toInt()}%" else "Giảm ${AppUtil.formatPrice(voucher.value.toFloat())}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isEnabled) Color.Black else Color.Gray
            )
            
            Text(
                text = "Đơn tối thiểu: ${AppUtil.formatPrice(voucher.minOrder.toFloat())}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )

            if (!isEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(12.dp), tint = Color.Red)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Còn thiếu ${AppUtil.formatPrice((voucher.minOrder - subtotal).toFloat())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (voucher.expiryDate > 0) {
                val daysLeft = ((voucher.expiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                Text(
                    text = if (daysLeft <= 3) "Sắp hết hạn: $daysLeft ngày" else "HSD: ${AppUtil.formatDate(voucher.expiryDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (daysLeft <= 3) Color.Red else Color.Gray,
                    fontWeight = if (daysLeft <= 3) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // Right: Radio button or Selection
        if (isEnabled) {
            RadioButton(
                selected = isSelected,
                onClick = onSelected,
                colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
            )
        }
    }
}
