package com.example.easyshop.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyshop.AppUtil
import com.example.easyshop.utils.MapUtils
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.R
import com.example.easyshop.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun ProfilePage(modifier: Modifier = Modifier) {
    val userModel = remember { mutableStateOf(UserModel()) }
    var addressInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
            .get().addOnCompleteListener {
                it.result.toObject(UserModel::class.java)?.let { user ->
                    userModel.value = user
                    addressInput = user.address
                }
            }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar & Name
        Image(
            painter = painterResource(R.drawable.profile_iconn),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )

        Text(
            text = userModel.value.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Address
                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    label = { Text("Address") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            modifier = Modifier.clickable {
                                MapUtils.openMapToPickAddress(context, addressInput)
                            },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                        if (addressInput.isNotEmpty()) {
                            Firebase.firestore.collection("users")
                                .document(FirebaseAuth.getInstance().currentUser?.uid!!)
                                .update("address", addressInput)
                                .addOnCompleteListener {
                                    if (it.isSuccessful) AppUtil.showToast(context, "Đã cập nhật!")
                                }
                        }
                    })
                )

                Spacer(Modifier.height(12.dp))

                // Email
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, null, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Email", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(userModel.value.email, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Cart
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text("Cart Items", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(userModel.value.cartItems.values.sum().toString(), fontSize = 14.sp)
                    }
                }
            }
        }

        // My Orders
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { GlobalNavigation.navController.navigate("orders") },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.List, null)
                Text("My Orders", modifier = Modifier.padding(start = 12.dp))
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowRight, null)
            }
        }

        Spacer(Modifier.weight(1f))

        // Sign Out
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                GlobalNavigation.navController.apply {
                    popBackStack()
                    navigate("auth")
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.ExitToApp, null)
            Spacer(Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold)
        }
    }
}