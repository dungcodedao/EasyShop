package com.example.easyshop.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.easyshop.*
import com.example.easyshop.R.*
import com.example.easyshop.model.UserModel
import com.example.easyshop.sale.AvatarPickerDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun ProfilePage(modifier: Modifier = Modifier) {
    val userModel = remember { mutableStateOf(UserModel()) }
    var addressInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var selectedAvatar by remember { mutableStateOf(drawable.profile_nam) }
    val context = LocalContext.current

    fun saveAddress() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (addressInput.isNotEmpty() && currentUser != null) {
            Firebase.firestore.collection("users").document(currentUser.uid)
                .update("address", addressInput)
                .addOnSuccessListener { AppUtil.showToast(context, "Address updated!") }
                .addOnFailureListener { AppUtil.showToast(context, "Failed to update") }
        }
    }

    fun saveName() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val cleanedName = nameInput.trim().replace(Regex("\\s+"), " ")
        if (cleanedName.isNotEmpty() && currentUser != null) {
            Firebase.firestore.collection("users").document(currentUser.uid)
                .update("name", cleanedName)
                .addOnSuccessListener {
                    userModel.value = userModel.value.copy(name = cleanedName)
                    AppUtil.showToast(context, "Name updated!")
                }
                .addOnFailureListener { AppUtil.showToast(context, "Failed to update") }
        }
    }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            GlobalNavigation.navController.navigate("auth")
            return@LaunchedEffect
        }
        Firebase.firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                document.toObject(UserModel::class.java)?.let { user ->
                    userModel.value = user
                    addressInput = user.address
                    nameInput = user.name
                }
            }
            .addOnFailureListener { e -> AppUtil.showToast(context, "Error: ${e.message}") }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Profile Header
        Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(string.profile), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Avatar with gradient border
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { showAvatarDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(selectedAvatar),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(104.dp).clip(CircleShape)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Name + Edit
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = userModel.value.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showEditNameDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Edit name", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Text(
                text = userModel.value.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text(stringResource(string.address)) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            IconButton(onClick = { saveAddress() }) {
                                Icon(Icons.Default.Check, "Save", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { saveAddress() })
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quick links
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column {
                    // Cart
                    ListItem(
                        headlineContent = { Text(stringResource(string.cart_items), fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("${userModel.value.cartItems.values.sum()} items") },
                        leadingContent = {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                        },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        modifier = Modifier.clickable { GlobalNavigation.navController.navigate("cart") }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                    // Orders
                    ListItem(
                        headlineContent = { Text(stringResource(string.my_orders), fontWeight = FontWeight.Medium) },
                        leadingContent = {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.AutoMirrored.Filled.List, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp)) }
                        },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        modifier = Modifier.clickable { GlobalNavigation.navController.navigate("orders") }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Sign Out
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    GlobalNavigation.navController.apply { popBackStack(); navigate("auth") }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(string.sign_out), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Avatar Picker Dialog
    if (showAvatarDialog) {
        AvatarPickerDialog(
            currentAvatar = selectedAvatar,
            onDismiss = { showAvatarDialog = false },
            onAvatarSelected = { selectedAvatar = it; showAvatarDialog = false }
        )
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Name") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = { TextButton(onClick = { saveName(); showEditNameDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(24.dp)
        )
    }
}