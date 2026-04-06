package com.example.easyshop.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.AppUtil
import com.example.easyshop.GlobalNavigation
import com.example.easyshop.R
import com.example.easyshop.model.UserModel
import com.example.easyshop.sale.AvatarPickerDialog
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

    fun saveName() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val cleanedName = nameInput.trim().replace(Regex("\\s+"), " ")
        if (cleanedName.isNotEmpty() && currentUser != null) {
            Firebase.firestore.collection("users").document(currentUser.uid)
                .update("name", cleanedName)
                .addOnSuccessListener {
                    userModel.value = userModel.value.copy(name = cleanedName)
                    AppUtil.showToast(context, "Đã cập nhật tên!")
                    showEditNameDialog = false
                }
                .addOnFailureListener { AppUtil.showToast(context, "Lỗi khi cập nhật tên") }
        }
    }

    fun savePhone() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (phoneInput.isNotEmpty() && currentUser != null) {
            Firebase.firestore.collection("users").document(currentUser.uid)
                .update("phone", phoneInput)
                .addOnSuccessListener {
                    userModel.value = userModel.value.copy(phone = phoneInput)
                    AppUtil.showToast(context, "Đã cập nhật số điện thoại!")
                    showEditPhoneDialog = false
                }
                .addOnFailureListener { AppUtil.showToast(context, "Lỗi khi cập nhật số điện thoại") }
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
            .addOnFailureListener { e -> AppUtil.showToast(context, "Lỗi: ${e.message}") }
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
                AppUtil.showToast(context, "Đã xóa địa chỉ")
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
            .update("addressList", currentList)
            .addOnSuccessListener {
                userModel.value = userModel.value.copy(addressList = currentList)
                AppUtil.showToast(context, "Đã lưu địa chỉ")
                showAddressDialog = false
            }
    }

    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val authViewModel: com.example.easyshop.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Profile Header
        Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.profile), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Avatar 
            Box(
                modifier = Modifier
                    .size(110.dp)
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
                        modifier = Modifier.size(104.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder nếu chưa có ảnh
                    Box(
                        modifier = Modifier.size(104.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
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

            Spacer(Modifier.height(16.dp))

            if (userModel.value.role != "admin") {
                // --- Primary Address Summary (TikTok Style) ---
                val defaultAddr = userModel.value.addressList.find { it.isDefault } ?: userModel.value.addressList.firstOrNull()
                
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showAddressListSheet = true },
                    color = Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
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
                                    text = if (defaultAddr != null) "${defaultAddr.fullName} (+84)${defaultAddr.phone.removePrefix("0")}" else "Chưa có địa chỉ",
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
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            } else {
                                Text("Chạm để thêm địa chỉ giao hàng", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Quick links
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column {
                        // Cart
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.cart_items), fontWeight = FontWeight.Medium) },
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
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Sign Out and Delete Account
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp)
                ) {
                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            GlobalNavigation.navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.sign_out), fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (userModel.value.role != "admin") {
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = { showDeleteAccountDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.delete_account), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
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
                                AppUtil.showToast(context, context.getString(R.string.delete_account_success))
                                GlobalNavigation.navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                if (error?.contains("recent-login", ignoreCase = true) == true) {
                                    AppUtil.showToast(context, context.getString(R.string.reauth_required_msg))
                                } else {
                                    AppUtil.showToast(context, error ?: context.getString(R.string.something_went_wrong))
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
                        Text("Xóa vĩnh viễn")
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
                        AppUtil.showToast(context, "Đã cập nhật ảnh đại diện")
                    }
                    .addOnFailureListener { e -> AppUtil.showToast(context, "Lỗi: ${e.message}") }
            }
        )
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Chỉnh sửa tên") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Họ và tên") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = { TextButton(onClick = { saveName() }) { Text("Lưu") } },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text("Hủy") } },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Edit Phone Dialog
    if (showEditPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showEditPhoneDialog = false },
            title = { Text("Chỉnh sửa số điện thoại") },
            text = {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Số điện thoại") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )
            },
            confirmButton = { TextButton(onClick = { savePhone() }) { Text("Lưu") } },
            dismissButton = { TextButton(onClick = { showEditPhoneDialog = false }) { Text("Hủy") } },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // --- Address Edit Dialog ---
    if (showAddressDialog) {
        var label by remember { mutableStateOf(editingAddress?.label ?: "Nhà riêng") }
        var name by remember { mutableStateOf(editingAddress?.fullName ?: userModel.value.name) }
        var phone by remember { mutableStateOf(editingAddress?.phone ?: userModel.value.phone) }
        var details by remember { mutableStateOf(editingAddress?.detailedAddress ?: "") }
        var isDefault by remember { mutableStateOf(editingAddress?.isDefault ?: false) }

        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text(if (editingAddress == null) "Thêm địa chỉ mới" else "Sửa địa chỉ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Nhà riêng", "Văn phòng").forEach { l ->
                            FilterChip(
                                selected = label == l,
                                onClick = { label = l },
                                label = { Text(l) }
                            )
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Họ tên người nhận") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Địa chỉ chi tiết") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                        Text("Đặt làm địa chỉ mặc định")
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
                }) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { showAddressDialog = false }) { Text("Hủy") } }
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
                Text("Địa chỉ của bạn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    Text("Thêm địa chỉ", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))

                // List of Addresses
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    userModel.value.addressList.sortedByDescending { it.isDefault }.forEachIndexed { index, addr ->
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
                                            color = Color(0xFFFF4867).copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(2.dp),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFF4867))
                                        ) {
                                            Text(
                                                "Mặc định",
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFFF4867),
                                                fontSize = 10.sp
                                            )
                                    }
                                }
                            }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Chỉnh sửa",
                                        modifier = Modifier.clickable { editingAddress = addr; showAddressDialog = true },
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (!addr.isDefault) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Đặt mặc định",
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
                    
                    if (userModel.value.addressList.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Đã cập nhật danh sách khu vực. Chỉnh sửa địa chỉ của bạn để giao hàng chính xác.",
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
}