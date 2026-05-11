package com.example.easyshop.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.model.UserModel
import com.example.easyshop.sale.AvatarPickerDialog
import com.example.easyshop.util.GlobalNavigation
import com.example.easyshop.util.LanguageManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(
    modifier: Modifier = Modifier,
    navController: NavController? = null
) {
    val userModel = remember { mutableStateOf(UserModel()) }
    var addressInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditPhoneDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val deleteAccountSuccess = stringResource(R.string.delete_account_success)
    val reauthRequiredMsg = stringResource(R.string.reauth_required_msg)
    val somethingWentWrong = stringResource(R.string.something_went_wrong)
    val homeLabel = stringResource(R.string.home_label)
    val officeLabel = stringResource(R.string.office_label)
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentUserLang by remember { mutableStateOf(LanguageManager.getUserLang(context)) }

    fun saveName() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val cleanedName = nameInput.trim().replace(Regex("\\s+"), " ")
        if (cleanedName.isNotEmpty() && currentUser != null) {
            Firebase.firestore.collection("users").document(currentUser.uid)
                .update("name", cleanedName)
                .addOnSuccessListener {
                    userModel.value = userModel.value.copy(name = cleanedName)
                    AppUtil.showToast(context, context.getString(R.string.name_updated_success))
                    showEditNameDialog = false
                }
                .addOnFailureListener { AppUtil.showToast(context, context.getString(R.string.name_update_error)) }
        }
    }

    fun savePhone() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (phoneInput.isNotEmpty() && currentUser != null) {
            Firebase.firestore.collection("users").document(currentUser.uid)
                .update("phone", phoneInput)
                .addOnSuccessListener {
                    userModel.value = userModel.value.copy(phone = phoneInput)
                    AppUtil.showToast(context, context.getString(R.string.phone_updated_success))
                    showEditPhoneDialog = false
                }
                .addOnFailureListener { AppUtil.showToast(context, context.getString(R.string.phone_update_error)) }
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
                    phoneInput = user.phone
                    nameInput = user.name
                }
            }
            .addOnFailureListener { e -> AppUtil.showToast(context, context.getString(R.string.profile_load_error, e.message ?: "")) }
    }

    // --- Address Management Logic ---
    var showAddressDialog by remember { mutableStateOf(false) }
    var showAddressListSheet by remember { mutableStateOf(false) }
    val addressSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingAddress by remember { mutableStateOf<com.example.easyshop.model.AddressModel?>(null) }

    fun deleteAddress(addressId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val newList = userModel.value.addressList.filter { it.id != addressId }
        Firebase.firestore.collection("users").document(currentUser.uid)
            .update("addressList", newList)
            .addOnSuccessListener {
                userModel.value = userModel.value.copy(addressList = newList)
                AppUtil.showToast(context, context.getString(R.string.address_deleted))
            }
    }

    fun saveAddressList(newAddress: com.example.easyshop.model.AddressModel) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentList = userModel.value.addressList.toMutableList()
        val index = currentList.indexOfFirst { it.id == newAddress.id }

        if (newAddress.isDefault) {
            currentList.replaceAll { it.copy(isDefault = false) }
        }

        if (index != -1) currentList[index] = newAddress
        else currentList.add(newAddress)

        if (currentList.size == 1) {
            currentList[0] = currentList[0].copy(isDefault = true)
        }

        Firebase.firestore.collection("users").document(currentUser.uid)
            .set(mapOf("addressList" to currentList), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                userModel.value = userModel.value.copy(addressList = currentList)
                AppUtil.showToast(context, context.getString(R.string.address_saved))
                showAddressDialog = false
            }
            .addOnFailureListener { e -> AppUtil.showToast(context, context.getString(R.string.address_save_error, e.message ?: "")) }
    }

    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val authViewModel: com.example.easyshop.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Profile Header
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.profile),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.width(240.dp),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.language), fontWeight = FontWeight.Medium) },
                            onClick = {
                                showMenu = false
                                showLanguageDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Language,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingIcon = {
                                Text(
                                    LanguageManager.displayName(currentUserLang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.support_center), fontWeight = FontWeight.Medium) },
                            onClick = {
                                showMenu = false
                                AppUtil.showToast(context, context.getString(R.string.support_contact, "support@easyshop.com"))
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Info,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.version_format, "1.0.0"), fontWeight = FontWeight.Medium) },
                            onClick = { showMenu = false },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Info,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Gray
                                )
                            },
                            enabled = false
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sign_out), fontWeight = FontWeight.Medium) },
                            onClick = {
                                showMenu = false
                                FirebaseAuth.getInstance().signOut()
                                GlobalNavigation.navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )

                        if (userModel.value.role != "admin") {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.delete_account),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showDeleteAccountDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            val compactHeight = maxHeight < 620.dp
            val avatarOuterSize = if (compactHeight) 88.dp else 110.dp
            val avatarInnerSize = if (compactHeight) 82.dp else 104.dp
            val topSpacing = if (compactHeight) 8.dp else 16.dp
            val sectionSpacing = if (compactHeight) 10.dp else 16.dp

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(topSpacing))

                    Box(
                        modifier = Modifier
                            .size(avatarOuterSize)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { showAvatarDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userModel.value.profileImg.isNotEmpty()) {
                            val imageRequest = coil.request.ImageRequest.Builder(LocalContext.current)
                                .data(userModel.value.profileImg)
                                .crossfade(true)
                                .memoryCacheKey(userModel.value.profileImg)
                                .diskCacheKey(userModel.value.profileImg)
                                .size(300)
                                .build()

                            AsyncImage(
                                model = imageRequest,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(avatarInnerSize).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(avatarInnerSize)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(if (compactHeight) 8.dp else 16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userModel.value.name,
                            style = if (compactHeight) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
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
                }

                if (userModel.value.role != "admin") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val defaultAddr = userModel.value.addressList.find { it.isDefault } ?: userModel.value.addressList.firstOrNull()

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddressListSheet = true },
                            color = Color.Transparent
                        ) {
                            Row(modifier = Modifier.padding(vertical = sectionSpacing), verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    null,
                                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (defaultAddr != null) "${defaultAddr.fullName} (+84)${defaultAddr.phone.removePrefix("0")}" else stringResource(R.string.no_address_set),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                    if (defaultAddr != null) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = defaultAddr.detailedAddress.trim(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            lineHeight = 20.sp,
                                            maxLines = if (compactHeight) 1 else 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(stringResource(R.string.add_address_hint), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.cart_items), fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text(stringResource(R.string.products_count, userModel.value.cartItems.values.sum())) },
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

                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.my_orders), fontWeight = FontWeight.Medium) },
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

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.chat_with_shop), fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text(stringResource(R.string.order_support_desc)) },
                                    leadingContent = {
                                        Box(
                                            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFFF9800).copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) { Icon(Icons.AutoMirrored.Filled.Message, null, tint = Color(0xFFFF9800), modifier = Modifier.size(22.dp)) }
                                    },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                                    modifier = Modifier.clickable { GlobalNavigation.navController.navigate("chat-with-shop") }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        var isDeactivating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isDeactivating) showDeleteAccountDialog = false },
            title = { Text(stringResource(R.string.delete_account_confirm), color = MaterialTheme.colorScheme.error) },
            text = { Text(stringResource(R.string.delete_account_warning_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        isDeactivating = true
                        authViewModel.deleteAccount { success, error ->
                            isDeactivating = false
                            if (success) {
                                showDeleteAccountDialog = false
                                AppUtil.showToast(context, deleteAccountSuccess)
                                GlobalNavigation.navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                if (error?.contains("recent-login", ignoreCase = true) == true) {
                                    AppUtil.showToast(context, reauthRequiredMsg)
                                } else {
                                    AppUtil.showToast(context, error ?: somethingWentWrong)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isDeactivating
                ) {
                    if (isDeactivating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.delete_action))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    enabled = !isDeactivating
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(26.dp)
        )
    }

    // Avatar Picker Dialog
    if (showAvatarDialog) {
        AvatarPickerDialog(
            currentAvatarUrl = userModel.value.profileImg,
            onDismiss = { showAvatarDialog = false },
            onAvatarSelected = { newUrl ->
                val currentUser = FirebaseAuth.getInstance().currentUser ?: return@AvatarPickerDialog
                Firebase.firestore.collection("users").document(currentUser.uid)
                    .update("profileImg", newUrl)
                    .addOnSuccessListener {
                        userModel.value = userModel.value.copy(profileImg = newUrl)
                        showAvatarDialog = false
                        AppUtil.showToast(context, context.getString(R.string.update_avatar_success))
                    }
                    .addOnFailureListener { e -> AppUtil.showToast(context, context.getString(R.string.generic_error, e.message ?: "")) }
            }
        )
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(stringResource(R.string.edit_name)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.full_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = { TextButton(onClick = { saveName() }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text(stringResource(R.string.cancel)) } },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Edit Phone Dialog
    if (showEditPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showEditPhoneDialog = false },
            title = { Text(stringResource(R.string.change_phone)) },
            text = {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text(stringResource(R.string.phone_number)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )
            },
            confirmButton = { TextButton(onClick = { savePhone() }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { showEditPhoneDialog = false }) { Text(stringResource(R.string.cancel)) } },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // --- Address Edit Dialog ---
    if (showAddressDialog) {
        var label by remember { mutableStateOf(editingAddress?.label ?: homeLabel) }
        var name by remember { mutableStateOf(editingAddress?.fullName ?: userModel.value.name) }
        var phone by remember { mutableStateOf(editingAddress?.phone ?: userModel.value.phone) }
        var details by remember { mutableStateOf(editingAddress?.detailedAddress ?: "") }
        var isDefault by remember { mutableStateOf(editingAddress?.isDefault ?: false) }

        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text(if (editingAddress == null) stringResource(R.string.add_new_address) else stringResource(R.string.edit_address)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (l in listOf(homeLabel, officeLabel)) {
                            FilterChip(
                                selected = label == l,
                                onClick = { label = l },
                                label = { Text(l) }
                            )
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.receiver_name)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.phone_number)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text(stringResource(R.string.detail_address_hint)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                        Text(stringResource(R.string.set_default_hint))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    saveAddressList(com.example.easyshop.model.AddressModel(
                        id = editingAddress?.id ?: java.util.UUID.randomUUID().toString(),
                        label = label,
                        fullName = name,
                        phone = phone,
                        detailedAddress = details,
                        isDefault = isDefault
                    ))
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showAddressDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    // --- TikTok style Address List Sheet ---
    if (showAddressListSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddressListSheet = false },
            sheetState = addressSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = Color.White,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(bottom = 32.dp)) {
                Text(stringResource(R.string.your_addresses), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))

                // "Thêm địa chỉ" Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingAddress = null; showAddressDialog = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.add_address), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

                // List of Addresses
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    for ((index, addr) in userModel.value.addressList.sortedByDescending { it.isDefault }.withIndex()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(addr.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text("(+84)${addr.phone.removePrefix("0")}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        addr.detailedAddress.trim(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray,
                                        lineHeight = 18.sp,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    if (addr.isDefault) {
                                        Spacer(Modifier.height(6.dp))
                                        Surface(
                                            color = Color(0xFF4F46E5).copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(2.dp),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF4F46E5))
                                        ) {
                                            Text(
                                                stringResource(R.string.default_label),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF4F46E5),
                                                fontSize = 10.sp
                                            )
                                    }
                                }
                            }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            stringResource(R.string.edit_action),
                                            modifier = Modifier.clickable { editingAddress = addr; showAddressDialog = true },
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (!addr.isDefault) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                stringResource(R.string.set_default),
                                                modifier = Modifier.clickable {
                                                    saveAddressList(addr.copy(isDefault = true))
                                                },
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }

                    if (userModel.value.addressList.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.address_update_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.1f)).padding(10.dp)
                            )
                        }
                }
            }
        }
    }

    // --- Language Picker Dialog (User) ---
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        LanguageManager.LANG_VI to "🇻🇳  Tiếng Việt",
                        LanguageManager.LANG_EN to "🇬🇧  English"
                    ).forEach { (tag, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (tag == currentUserLang) return@clickable
                                    currentUserLang = tag
                                    showLanguageDialog = false
                                    val activity = (context as? Activity)
                                    LanguageManager.setUserLang(context, tag)
                                    activity?.overridePendingTransition(0, 0)
                                    activity?.recreate()
                                }
                                .background(
                                    if (currentUserLang == tag)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                            if (currentUserLang == tag) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
