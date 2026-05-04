package com.example.easyshop.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.easyshop.R
import com.example.easyshop.components.CartItemView
import com.example.easyshop.model.ProductModel
import com.example.easyshop.model.UserModel
import com.example.easyshop.util.GlobalNavigation
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun CartPage(
    modifier: Modifier = Modifier,
    navController: NavController? = null
) {
    val userModel = remember { mutableStateOf(UserModel()) }
    var isLoading by remember { mutableStateOf(true) }
    var showIncompleteProfileDialog by remember { mutableStateOf(false) }
    val productsMap = remember { mutableStateOf<Map<String, ProductModel>>(emptyMap()) }
    
    // Chuẩn bị chuỗi ở đầu hàm để tránh lỗi IDE trong lambda và giúp code đẹp hơn
    val profilesIncompleteTitle = stringResource(R.string.profiles_incomplete_title)
    val profilesIncompleteMsg = stringResource(R.string.profiles_incomplete_msg)
    val goToProfileLabel = stringResource(R.string.go_to_profile)
    val cancelLabel = stringResource(R.string.cancel)

    DisposableEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val listener = if (uid != null) {
            Firebase.firestore.collection("users")
                .document(uid)
                .addSnapshotListener { it, _ ->
                    it?.toObject(UserModel::class.java)?.let { user ->
                        userModel.value = user

                        val productIds = user.cartItems.keys.toList()
                        if (productIds.isNotEmpty()) {
                            Firebase.firestore.collection("data").document("stock")
                                .collection("products")
                                .whereIn(
                                    com.google.firebase.firestore.FieldPath.documentId(),
                                    productIds
                                )
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    val newMap = querySnapshot.documents.associate {
                                        it.id to (it.toObject(ProductModel::class.java)
                                            ?: ProductModel())
                                    }
                                    productsMap.value = newMap
                                    isLoading = false
                                }
                        } else {
                            productsMap.value = emptyMap()
                            isLoading = false
                        }
                    } ?: run { isLoading = false }
                }
        } else {
            isLoading = false
            null
        }
        onDispose { listener?.remove() }
    }


    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Toolbar gọn gàng nhưng vẫn có độ cao chuẩn và viền (Khôi phục)
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (navController != null && navController.previousBackStackEntry != null) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface 
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Spacer(modifier = Modifier.size(4.dp))
                }
                
                Text(
                    text = stringResource(id = R.string.your_cart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            CartPageContent(
                modifier = Modifier.weight(1f),
                model = userModel.value,
                productsMap = productsMap.value,
                showTitle = false,
                onCheckoutClick = {
                    val defaultAddr = userModel.value.addressList.find { it.isDefault } ?: userModel.value.addressList.firstOrNull()
                    if (defaultAddr == null || defaultAddr.phone.isBlank() || defaultAddr.detailedAddress.isBlank()) {
                        showIncompleteProfileDialog = true
                    } else {
                        GlobalNavigation.navController.navigate("checkout")
                    }
                }
            )
        }

        if (showIncompleteProfileDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showIncompleteProfileDialog = false },
                title = { Text(profilesIncompleteTitle) },
                text = { Text(profilesIncompleteMsg) },
                confirmButton = {
                    Button(onClick = {
                        showIncompleteProfileDialog = false
                        GlobalNavigation.navController.navigate("profile")
                    }) {
                        Text(goToProfileLabel)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showIncompleteProfileDialog = false }) {
                        Text(cancelLabel)
                    }
                }
            )
        }
    }
}

@Composable
fun CartPageContent(
    modifier: Modifier,
    model: UserModel,
    productsMap: Map<String, ProductModel>,
    showTitle: Boolean,
    onCheckoutClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (model.cartItems.isEmpty()) {
            if (showTitle) {
                Text(
                    text = stringResource(id = R.string.your_cart),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            EmptyCart()
        } else {
            CartContent(
                userModel = model,
                productsMap = productsMap,
                showTitle = showTitle,
                onCheckoutClick = onCheckoutClick
            )
        }
    }
}

@Composable
private fun EmptyCart() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.empty_cart_msg),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Dạo một vòng cửa hàng và tìm món đồ bạn yêu thích nhé!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun CartContent(
    userModel: UserModel,
    productsMap: Map<String, ProductModel>,
    showTitle: Boolean = false,
    onCheckoutClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize())   {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(userModel.cartItems.toList(), key = { it.first }) { (productId, qty) ->
                val product = productsMap[productId] ?: ProductModel()
                CartItemView(product = product, qty = qty)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.total_items),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${userModel.cartItems.values.sumOf { it }}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = onCheckoutClick,
                modifier = Modifier
                    .width(220.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5)
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.checkout),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
