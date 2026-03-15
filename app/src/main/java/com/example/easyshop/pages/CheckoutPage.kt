package com.example.easyshop.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.AppUtil
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.R
import com.example.easyshop.model.AddressModel
import com.example.easyshop.model.ProductModel
import com.example.easyshop.model.UserModel
import com.example.easyshop.sale.PromoCodeInput
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutPage(modifier: Modifier = Modifier) {
    val userModel = remember { mutableStateOf(UserModel()) }
    val productList = remember { mutableStateListOf<ProductModel>() }
    val subTotal = remember { mutableFloatStateOf(0f) }
    val discount = remember { mutableFloatStateOf(0f) }
    val total = remember { mutableFloatStateOf(0f) }
    
    // Quản lý địa chỉ chọn lọc
    var selectedAddress by remember { mutableStateOf<AddressModel?>(null) }
    var showAddressSheet by remember { mutableStateOf(false) }
    val addressSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun calculateAndAssign() {
        subTotal.floatValue = 0f
        productList.forEach {
            if (it.actualPrice.isNotEmpty()) {
                val qty = userModel.value.cartItems[it.id] ?: 0
                subTotal.floatValue += it.actualPrice.toFloat() * qty
            }
        }
        total.floatValue = (subTotal.floatValue - discount.floatValue).coerceAtLeast(0f)
    }

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
            .get().addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result.toObject(UserModel::class.java)?.let { result ->
                        userModel.value = result
                        // Tự động chọn địa chỉ mặc định
                        selectedAddress = result.addressList.find { addr -> addr.isDefault } 
                                         ?: result.addressList.firstOrNull()
                        
                        if (userModel.value.cartItems.isNotEmpty()) {
                            Firebase.firestore.collection("data").document("stock").collection("products")
                                .whereIn("id", userModel.value.cartItems.keys.toList())
                                .get().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        productList.clear()
                                        productList.addAll(task.result.toObjects(ProductModel::class.java))
                                        calculateAndAssign()
                                    }
                                }
                        }
                    }
                }
            }
    }

    LaunchedEffect(discount.floatValue) {
        total.floatValue = (subTotal.floatValue - discount.floatValue).coerceAtLeast(0f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.checkout), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { GlobalNavigation.navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TikTok-style Delivery Address Selector
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { showAddressSheet = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        tint = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp).align(Alignment.Top)
                    )
                    
                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        if (selectedAddress != null) {
                            Text(
                                text = "${selectedAddress!!.fullName} (+84)${selectedAddress!!.phone.removePrefix("0")}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = selectedAddress!!.detailedAddress.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black.copy(alpha = 0.8f),
                                lineHeight = 20.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                "⚠️ Chưa có địa chỉ giao hàng. Nhấn để chọn!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Order Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(stringResource(id = R.string.order_summary), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

                    PriceRow(stringResource(id = R.string.subtotal), subTotal.floatValue)
                    Spacer(Modifier.height(12.dp))

                    PromoCodeInput(
                        subtotal = subTotal.floatValue,
                        onDiscountApplied = { discount.floatValue = it }
                    )

                    Spacer(Modifier.height(12.dp))

                    if (discount.floatValue > 0) {
                        PriceRow(stringResource(id = R.string.discount), discount.floatValue, isDiscount = true)
                        Spacer(Modifier.height(8.dp))
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(id = R.string.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            AppUtil.formatPrice(total.floatValue),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Pay Button
            Button(
                onClick = { GlobalNavigation.navController.navigate("payment/${total.floatValue}") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = selectedAddress != null,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "${stringResource(id = R.string.pay_now)} - ${AppUtil.formatPrice(total.floatValue)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // TikTok-style Address Selector Sheet
    if (showAddressSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddressSheet = false },
            sheetState = addressSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = Color.White,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(bottom = 32.dp)) {
                Text("Địa chỉ của bạn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                
                if (userModel.value.addressList.isEmpty()) {
                    Text("Chưa có địa chỉ nào. Hãy vào Profile để thêm.")
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        userModel.value.addressList.sortedByDescending { it.isDefault }.forEachIndexed { index, addr ->
                            val isSelected = selectedAddress?.id == addr.id
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        selectedAddress = addr
                                        showAddressSheet = false 
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        null,
                                        tint = if (isSelected) Color(0xFFFF4867) else Color.Gray,
                                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${addr.fullName} (+84)${addr.phone.removePrefix("0")}",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isSelected) Color(0xFFFF4867) else Color.Black
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = addr.detailedAddress.trim(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) Color(0xFFFF4867).copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
                                            lineHeight = 20.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (addr.isDefault) {
                                            Spacer(Modifier.height(6.dp))
                                            Surface(
                                                color = Color(0xFFFF4867).copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(2.dp),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFF4867))
                                            ) {
                                                Text(
                                                    "Mặc định",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFFF4867),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { 
                        showAddressSheet = false
                        // Chuyển hướng đến Profile để quản lý
                        AppUtil.showToast(Firebase.auth.app.applicationContext, "Vào Profile để thêm/sửa địa chỉ")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Thay đổi/Quản lý địa chỉ trong Profile")
                }
            }
        }
    }
}

@Composable
fun PriceRow(title: String, value: Float, isDiscount: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "${if (isDiscount) "-" else ""}${AppUtil.formatPrice(value)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (isDiscount) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}