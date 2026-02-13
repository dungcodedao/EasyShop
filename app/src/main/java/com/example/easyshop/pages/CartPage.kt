package com.example.easyshop.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.components.CartItemView
import com.example.easyshop.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.example.easyshop.R

@Composable
fun CartPage(
    modifier: Modifier = Modifier,
    navController: NavController? = null
) {
    val userModel = remember { mutableStateOf(UserModel()) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = Firebase.firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
            .addSnapshotListener { it, _ ->
                it?.toObject(UserModel::class.java)?.let { user ->
                    userModel.value = user
                }
                isLoading = false
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
            CartPageContent(modifier = Modifier.weight(1f), model = userModel.value, showTitle = navController == null)
        }
    }
}

@Composable
private fun CartPageContent(modifier: Modifier, model: UserModel, showTitle: Boolean) {
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
            CartContent(userModel = model)
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
                Text(text = "🛒", style = androidx.compose.ui.text.TextStyle(fontSize = 50.sp))
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
private fun ColumnScope.CartContent(userModel: UserModel) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(userModel.cartItems.toList(), key = { it.first }) { (productId, qty) ->
            CartItemView(productId = productId, qty = qty)
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