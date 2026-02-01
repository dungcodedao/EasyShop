package com.example.easyshop.pages

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.AppUtil
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.sale.PromoCodeInput
import com.example.easyshop.model.ProductModel
import com.example.easyshop.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun CheckoutPage(modifier: Modifier = Modifier) {
    val userModel = remember { mutableStateOf(UserModel()) }
    val productList = remember { mutableStateListOf<ProductModel>() }
    val subTotal = remember { mutableFloatStateOf(0f) }
    val discount = remember { mutableFloatStateOf(0f) }
    val tax = remember { mutableFloatStateOf(0f) }
    val total = remember { mutableFloatStateOf(0f) }

    fun calculateAndAssign() {
        subTotal.floatValue = 0f
        productList.forEach {
            if (it.actualPrice.isNotEmpty()) {
                val qty = userModel.value.cartItems[it.id] ?: 0
                subTotal.floatValue += it.actualPrice.toFloat() * qty
            }
        }
        tax.floatValue = subTotal.floatValue * (AppUtil.getTaxPercentage() / 100)
        total.floatValue = subTotal.floatValue - discount.floatValue + tax.floatValue
    }

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
            .get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val result = it.result.toObject(UserModel::class.java)
                    if (result != null) {
                        userModel.value = result
                        if (userModel.value.cartItems.isNotEmpty()) {
                            Firebase.firestore.collection("data")
                                .document("stock").collection("products")
                                .whereIn("id", userModel.value.cartItems.keys.toList())
                                .get().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val resultProducts =
                                            task.result.toObjects(ProductModel::class.java)
                                        productList.clear()
                                        productList.addAll(resultProducts)
                                        calculateAndAssign()
                                    }
                                }
                        }
                    }
                }
            }
    }

    // Recalculate when discount changes
    LaunchedEffect(discount.floatValue) {
        total.floatValue = subTotal.floatValue - discount.floatValue + tax.floatValue
    }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding().background(Color(0xFFF5F5F5))) {
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
                    onClick = { GlobalNavigation.navController.navigateUp() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Checkout",
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

            // Delivery Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delivery Address",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = userModel.value.name, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = userModel.value.address,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Price Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Order Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    PriceRow("Subtotal", subTotal.floatValue)
                    Spacer(modifier = Modifier.height(12.dp))

                    // ✅ PROMO CODE INPUT COMPONENT
                    PromoCodeInput(
                        subtotal = subTotal.floatValue,
                        onDiscountApplied = { discountAmount ->
                            discount.floatValue = discountAmount
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (discount.floatValue > 0) {
                        PriceRow("Discount", discount.floatValue, isDiscount = true)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    PriceRow("Tax", tax.floatValue)
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$${"%.2f".format(total.floatValue)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pay Button
            Button(
                onClick = {
                    GlobalNavigation.navController.navigate("payment/${total.floatValue}")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = "Pay Now - $${"%.2f".format(total.floatValue)}",
                    fontSize = 18.sp,
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
        Text(text = title, fontSize = 15.sp, color = Color.Gray)
        Text(
            text = "${if (isDiscount) "-" else ""}$${"%.2f".format(value)}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDiscount) Color(0xFFFF5252) else Color.Black
        )
    }
}