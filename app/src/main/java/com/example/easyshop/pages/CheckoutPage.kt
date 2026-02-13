package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.easyshop.R
import com.example.easyshop.AppUtil
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.sale.PromoCodeInput
import com.example.easyshop.model.ProductModel
import com.example.easyshop.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutPage(modifier: Modifier = Modifier) {
    val userModel = remember { mutableStateOf(UserModel()) }
    val productList = remember { mutableStateListOf<ProductModel>() }
    val subTotal = remember { mutableFloatStateOf(0f) }
    val discount = remember { mutableFloatStateOf(0f) }
    val total = remember { mutableFloatStateOf(0f) }

    fun calculateAndAssign() {
        subTotal.floatValue = 0f
        productList.forEach {
            if (it.actualPrice.isNotEmpty()) {
                val qty = userModel.value.cartItems[it.id] ?: 0
                subTotal.floatValue += it.actualPrice.toFloat() * qty
            }
        }
        total.floatValue = subTotal.floatValue - discount.floatValue
    }

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
            .get().addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result.toObject(UserModel::class.java)?.let { result ->
                        userModel.value = result
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
        total.floatValue = subTotal.floatValue - discount.floatValue
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
            // Delivery Address
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.delivery_address), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(userModel.value.name, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        userModel.value.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 28.dp)
                    )
                }
            }

            // Order Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp)
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