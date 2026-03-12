package com.example.easyshop.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.R
import com.example.easyshop.components.CartItemView
import com.example.easyshop.model.ProductModel
import com.example.easyshop.model.UserModel
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
    val productsMap = remember { mutableStateOf<Map<String, ProductModel>>(emptyMap()) }

    DisposableEffect(Unit) {
        val listener = Firebase.firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
            .addSnapshotListener { it, _ ->
                it?.toObject(UserModel::class.java)?.let { user ->
                    userModel.value = user
                    
                    // Batch fetch products details
                    val productIds = user.cartItems.keys.toList()
                    if (productIds.isNotEmpty()) {
                        // Chỉ fetch những product chưa có trong map hoặc fetch lại nếu cần
                        // Với giỏ hàng thường < 30 món, whereIn là tối ưu nhất
                        Firebase.firestore.collection("data").document("stock")
                            .collection("products")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), productIds)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                val newMap = querySnapshot.documents.associate { 
                                    it.id to (it.toObject(com.example.easyshop.model.ProductModel::class.java) ?: com.example.easyshop.model.ProductModel())
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
        onDispose { listener.remove() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (navController != null) {
            Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", Modifier.size(22.dp))
                    }
                    Text(stringResource(id = R.string.your_cart), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(40.dp))
                }
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
                showTitle = navController == null
            )
        }
    }
}

@Composable
private fun CartPageContent(
    modifier: Modifier, 
    model: UserModel, 
    productsMap: Map<String, com.example.easyshop.model.ProductModel>, 
    showTitle: Boolean
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (showTitle) {
            Text(
                text = stringResource(id = R.string.your_cart),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (model.cartItems.isEmpty()) {
            EmptyCart()
        } else {
            CartContent(userModel = model, productsMap = productsMap)
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
private fun ColumnScope.CartContent(
    userModel: UserModel, 
    productsMap: Map<String, com.example.easyshop.model.ProductModel>
) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(userModel.cartItems.toList(), key = { it.first }) { (productId, qty) ->
            val product = productsMap[productId] ?: com.example.easyshop.model.ProductModel()
            CartItemView(product = product, qty = qty)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Total
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
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

    Spacer(modifier = Modifier.height(12.dp))

    // Checkout
    Button(
        onClick = { GlobalNavigation.navController.navigate("checkout") },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.checkout),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}