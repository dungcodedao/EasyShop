package com.example.easyshop.pages

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.model.ProductModel
import com.example.easyshop.sale.PromoCodeInput
import com.example.easyshop.util.GlobalNavigation
import com.example.easyshop.util.clickableOnce
import com.example.easyshop.util.rememberDebouncedClick
import com.example.easyshop.viewmodel.CheckoutViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutPage(
    modifier: Modifier = Modifier,
    viewModel: CheckoutViewModel = viewModel()
) {
    val context = LocalContext.current
    
    val userModel by viewModel.userModel.collectAsState()
    val productList by viewModel.cartProducts.collectAsState()
    val subTotal by viewModel.subtotal.collectAsState()
    val discount by viewModel.discount.collectAsState()
    val promoCodeUsed by viewModel.promoCode.collectAsState()
    val selectedAddress by viewModel.selectedAddress.collectAsState()
    
    val total = (subTotal - discount).coerceAtLeast(0.0)
    
    var showAddressSheet by remember { mutableStateOf(false) }
    val addressSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val note by viewModel.note.collectAsState()

    LaunchedEffect(Unit) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            GlobalNavigation.navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
            return@LaunchedEffect
        }
        viewModel.fetchData()
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
                .padding(top = padding.calculateTopPadding())
        ) {
            // --- Scrollable Content Section ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableOnce { showAddressSheet = true },
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
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp).align(Alignment.Top)
                        )
                        
                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (selectedAddress != null && selectedAddress!!.phone.isNotBlank() && selectedAddress!!.detailedAddress.isNotBlank()) {
                                Text(
                                    text = "${selectedAddress!!.fullName} (+84)${selectedAddress!!.phone.removePrefix("0")}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = selectedAddress!!.detailedAddress.trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    lineHeight = 20.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = if (selectedAddress == null) "⚠️ Chưa có địa chỉ giao hàng. Nhấn để chọn!" 
                                           else if (selectedAddress!!.phone.isBlank()) "⚠️ Thiếu số điện thoại giao hàng"
                                           else "⚠️ Địa chỉ chi tiết không được để trống",
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
                
                // Products Section
                Text(
                    text = stringResource(id = R.string.your_products),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    productList.forEach { product ->
                        val qty = userModel?.cartItems?.get(product.id) ?: 0L
                        if (qty > 0) {
                            CheckoutProductItem(product = product, qty = qty)
                        }
                    }
                }

                // Customer Note Section
                androidx.compose.material3.OutlinedTextField(
                    value = note,
                    onValueChange = { viewModel.updateNote(it) },
                    label = { Text(stringResource(R.string.order_note_label)) },
                    placeholder = { Text(stringResource(R.string.order_note_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    maxLines = 3,
                    minLines = 2
                )
            }

            // --- Fixed Footer Section (Cart-style Pinned) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Order Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(id = R.string.order_summary), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        PriceRow(stringResource(id = R.string.subtotal), subTotal.toFloat())
                        Spacer(Modifier.height(8.dp))

                        PromoCodeInput(
                            subtotal = subTotal.toFloat(),
                            onDiscountApplied = { _, code -> 
                                 if (code.isNotEmpty()) {
                                     com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                         .collection("promoCodes").document(code).get()
                                         .addOnSuccessListener { doc ->
                                             doc.toObject(com.example.easyshop.model.PromoCodeModel::class.java)?.let {
                                                 viewModel.applyPromoCode(code, it)
                                             }
                                         }
                                 } else {
                                     viewModel.removePromoCode()
                                 }
                            }
                        )

                        if (discount > 0) {
                            Spacer(Modifier.height(8.dp))
                            PriceRow(stringResource(id = R.string.discount), discount.toFloat(), isDiscount = true)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(id = R.string.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                AppUtil.formatPrice(total),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Button(
                    onClick = rememberDebouncedClick(
                        enabled = (selectedAddress != null && 
                                  selectedAddress!!.phone.isNotBlank() && 
                                  selectedAddress!!.detailedAddress.isNotBlank())
                    ) {
                        if (!AppUtil.isNetworkAvailable(context)) {
                            AppUtil.showToast(context, context.getString(R.string.no_internet))
                        } else {
                            val pCode = promoCodeUsed.ifEmpty { "NONE" }
                            val encodedNote = java.net.URLEncoder.encode(note, "UTF-8")
                            GlobalNavigation.navController.navigate("payment/${total.toFloat()}/${subTotal.toFloat()}/${discount.toFloat()}/$pCode?note=$encodedNote")
                        }
                    },
                    modifier = Modifier
                        .width(220.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        stringResource(id = R.string.pay_now),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
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
                
                val addressList = userModel?.addressList ?: emptyList()
                if (addressList.isEmpty()) {
                    Text("Chưa có địa chỉ nào. Hãy vào Profile để thêm.")
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        addressList.sortedByDescending { it.isDefault }.forEachIndexed { index, addr ->
                            val isSelected = selectedAddress?.id == addr.id
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickableOnce { 
                                        viewModel.setSelectedAddress(addr)
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
                                        tint = if (isSelected) Color(0xFF4F46E5) else Color.Gray,
                                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${addr.fullName} (+84)${addr.phone.removePrefix("0")}",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isSelected) Color(0xFF4F46E5) else Color.Black
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = addr.detailedAddress.trim(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) Color(0xFF4F46E5).copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
                                            lineHeight = 20.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (addr.isDefault) {
                                            Spacer(Modifier.height(6.dp))
                                            Surface(
                                                color = Color(0xFF4F46E5).copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(2.dp),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF4F46E5))
                                            ) {
                                                Text(
                                                    "Mặc định",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF4F46E5),
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
                    Text("Thay đổi/Quản lý địa chỉ")
                }
            }
        }
    }
}

@Composable
fun CheckoutProductItem(product: ProductModel, qty: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        AsyncImage(
            model = product.images.firstOrNull(),
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${AppUtil.formatPrice(product.actualPrice)} x $qty",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val itemTotal = (product.actualPrice.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 0.0) * qty
        Text(
            text = AppUtil.formatPrice(itemTotal.toFloat()),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
