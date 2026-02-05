package com.example.easyshop.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Function save address
    fun saveAddress() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (addressInput.isNotEmpty() && currentUser != null) {
            Firebase.firestore.collection("users")
                .document(currentUser.uid)
                .update("address", addressInput)
                .addOnSuccessListener {
                    AppUtil.showToast(context, "Address updated!")
                }
                .addOnFailureListener {
                    AppUtil.showToast(context, "Failed to update")
                }
        }
    }

    // Function save name
    fun saveName() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val cleanedName = nameInput.trim().replace(Regex("\\s+"), " ")
        if (cleanedName.isNotEmpty() && currentUser != null) {
            Firebase.firestore.collection("users")
                .document(currentUser.uid)
                .update("name", cleanedName)
                .addOnSuccessListener {
                    userModel.value = userModel.value.copy(name = cleanedName)
                    AppUtil.showToast(context, "Name updated!")
                }
                .addOnFailureListener {
                    AppUtil.showToast(context, "Failed to update")
                }
        }
    }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            GlobalNavigation.navController.navigate("auth")
            return@LaunchedEffect
        }

        Firebase.firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(UserModel::class.java)?.let { user ->
                    userModel.value = user
                    addressInput = user.address
                    nameInput = user.name
                }
            }
            .addOnFailureListener { e ->
                AppUtil.showToast(context, "Error: ${e.message}")
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(string.profile),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar - Clickable
            Image(
                painter = painterResource(selectedAvatar),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { showAvatarDialog = true }
            )

            // Name với icon Edit
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = userModel.value.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { showEditNameDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit name",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Address
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text(stringResource(string.address)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { saveAddress() }) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { saveAddress() })
                    )

                    Spacer(Modifier.height(12.dp))

                    // Email
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, null, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                stringResource(string.email),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(userModel.value.email, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Language Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Language, null)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            stringResource(string.language),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val currentLang = LocaleHelper.getLanguage(context)
                        Text(
                            if (currentLang == "vi") "Tiếng Việt" else "English",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    
                    var showLangDialog by remember { mutableStateOf(false) }
                    TextButton(onClick = { showLangDialog = true }) {
                        Text("Change")
                    }

                    if (showLangDialog) {
                        AlertDialog(
                            onDismissRequest = { showLangDialog = false },
                            title = { Text(stringResource(string.select_language)) },
                            text = {
                                Column {
                                    val languages = listOf("en" to "English", "vi" to "Tiếng Việt")
                                    languages.forEach { (code, name) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    LocaleHelper.setLocale(context, code)
                                                    (context as? android.app.Activity)?.recreate()
                                                    showLangDialog = false
                                                }
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = LocaleHelper.getLanguage(context) == code,
                                                onClick = null
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Text(name)
                                        }
                                    }
                                }
                            },
                            confirmButton = {}
                        )
                    }
                }
            }

            // Cart Items Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { GlobalNavigation.navController.navigate("cart") },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ShoppingCart, null)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            stringResource(string.cart_items),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            userModel.value.cartItems.values.sum().toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.KeyboardArrowRight, null)
                }
            }

            // My Orders Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { GlobalNavigation.navController.navigate("orders") },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.List, null)
                    Text(stringResource(string.my_orders), modifier = Modifier.padding(start = 12.dp))
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.ExitToApp, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(string.sign_out), fontWeight = FontWeight.Bold)
            }
        }
    }

    // Avatar Picker Dialog
    if (showAvatarDialog) {
        AvatarPickerDialog(
            currentAvatar = selectedAvatar,
            onDismiss = { showAvatarDialog = false },
            onAvatarSelected = { avatarRes ->
                selectedAvatar = avatarRes
                showAvatarDialog = false
            }
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
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveName()
                        showEditNameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}