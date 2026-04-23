package com.example.easyshop.admin

import com.example.easyshop.AppUtil
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.easyshop.R
import com.example.easyshop.model.PromoCodeModel
import com.example.easyshop.model.isExpired
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePromoCodesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var promoCodes by remember { mutableStateOf<List<PromoCodeModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPromo by remember { mutableStateOf<PromoCodeModel?>(null) }
    var publishingPromo by remember { mutableStateOf<PromoCodeModel?>(null) }
    val firestore = Firebase.firestore

    fun loadPromoCodes() {
        isLoading = true
        firestore.collection("promoCodes").get()
            .addOnSuccessListener { result ->
                promoCodes = result.documents.mapNotNull { it.toObject(PromoCodeModel::class.java)?.copy(docId = it.id) }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    // Tải lại dữ liệu âm thầm (không hiện loading toàn màn hình) dùng cho toggle/unpublish
    fun silentReload() {
        firestore.collection("promoCodes").get()
            .addOnSuccessListener { result ->
                promoCodes = result.documents.mapNotNull { it.toObject(PromoCodeModel::class.java)?.copy(docId = it.id) }
            }
    }

    // Hàm gửi thông báo quảng bá tới người dùng khi phát hành mã
    fun broadcastPromoNotification(promo: PromoCodeModel) {
        val title = "Ưu đãi mới từ EasyShop!"
        val body = "Mã [${promo.code}] đã sẵn sàng. Nhận ngay ưu đãi: ${promo.description}"

        val notif = hashMapOf(
            "userId" to "broadcast",
            "title" to title,
            "body" to body,
            "type" to "PROMO",
            "isRead" to false,
            "recipientRole" to "user",
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        firestore.collection("notifications").add(notif)
            .addOnSuccessListener {
                AppUtil.showSuccess("Đã phát hành và gửi thông báo tới người dùng!")
            }
            .addOnFailureListener { e ->
                AppUtil.showError("Lỗi gửi thông báo: ${e.message}")
            }

        // Gửi FCM push tới tất cả user (hoạt động kể cả khi app đã tắt)
        com.example.easyshop.services.FcmSender.sendToAllUsers(
            title = title,
            body = body,
            type = "PROMO"
        )
    }

    LaunchedEffect(Unit) { loadPromoCodes() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.manage_promo_codes_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back_to_home))
                    }
                },
                actions = {
                    IconButton(onClick = { loadPromoCodes() }) { Icon(Icons.Default.Refresh, stringResource(id = R.string.reset)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, stringResource(id = R.string.add_promo_code_action))
            }
        }
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (promoCodes.isEmpty()) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ConfirmationNumber, null, Modifier.size(64.dp), MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(id = R.string.no_promo_codes_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = promoCodes, key = { it.docId.ifBlank { it.code } }) { promo ->
                        PromoCodeItem(
                            promo = promo,
                            onToggleActive = {
                                firestore.collection("promoCodes").document(promo.docId)
                                    .update("active", !promo.active)
                                    .addOnSuccessListener { silentReload() }
                            },
                            onPublish = {
                                publishingPromo = promo
                            },
                            onUnpublish = {
                                firestore.collection("promoCodes").document(promo.docId)
                                    .update("isIssued", false)
                                    .addOnSuccessListener {
                                        silentReload()
                                        AppUtil.showSuccess("Đã thu hồi mã [${promo.code}]")
                                    }
                            },
                            onEdit = { editingPromo = promo },
                            onDelete = {
                                firestore.collection("promoCodes").document(promo.docId).delete()
                                    .addOnSuccessListener {
                                        loadPromoCodes()
                                        AppUtil.showSuccess(context.getString(R.string.deleted_promo_msg, promo.code))
                                    }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPromoCodeDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newPromo ->
                // Sử dụng chính code làm ID tài liệu cho các mã mới để dữ liệu gọn gàng
                firestore.collection("promoCodes").document(newPromo.code).set(newPromo)
                    .addOnSuccessListener {
                        showAddDialog = false; loadPromoCodes()
                        AppUtil.showSuccess(context.getString(R.string.added_promo_msg, newPromo.code))
                    }
                    .addOnFailureListener {
                        AppUtil.showError(context.getString(R.string.error_adding_promo))
                    }
            }
        )
    }

    // ── Dialog chỉnh sửa mã giảm giá ─────────────────────────────────────────
    editingPromo?.let { promo ->
        EditPromoCodeDialog(
            promo = promo,
            onDismiss = { editingPromo = null },
            onConfirm = { updated ->
                firestore.collection("promoCodes").document(promo.docId)
                    .update(
                        mapOf(
                            "description" to updated.description,
                            "type" to updated.type,
                            "value" to updated.value,
                            "minOrder" to updated.minOrder,
                            "maxDiscount" to updated.maxDiscount,
                            "usageLimit" to updated.usageLimit,
                            "expiryDate" to updated.expiryDate
                        )
                    )
                    .addOnSuccessListener {
                        editingPromo = null
                        loadPromoCodes()
                        AppUtil.showSuccess(context.getString(R.string.updated_promo_msg, promo.code))
                    }
                    .addOnFailureListener {
                        AppUtil.showError(context.getString(R.string.error_updating_promo))
                    }
            }
        )
    }

    // ── Dialog xác nhận phát hành ─────────────────────────────────────────────
    publishingPromo?.let { promo ->
        AlertDialog(
            onDismissRequest = { publishingPromo = null },
            title = { Text("Xác nhận phát hành") },
            text = { Text("Bạn có chắc chắn muốn phát hành mã giảm giá ${promo.code} tới tất cả người dùng không? Sau khi phát hành, mã sẽ xuất hiện trong Thông báo của User.") },
            confirmButton = {
                Button(
                    onClick = {
                        val p = publishingPromo ?: return@Button
                        publishingPromo = null // Đóng dialog ngay
                        firestore.collection("promoCodes").document(p.docId)
                            .update("isIssued", true, "issuedAt", System.currentTimeMillis())
                            .addOnSuccessListener {
                                loadPromoCodes()
                                broadcastPromoNotification(p)
                            }
                            .addOnFailureListener { e ->
                                AppUtil.showError("Lỗi phát hành: ${e.message}")
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Phát hành ngay", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { publishingPromo = null }) {
                    Text("Hủy")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun PromoCodeItem(
    promo: PromoCodeModel,
    onToggleActive: () -> Unit,
    onPublish: () -> Unit,
    onUnpublish: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val isExpired = promo.isExpired()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        promo.code,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (promo.active && !isExpired) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if (promo.type == "percentage") MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color(0xFF9C27B0).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (promo.type == "percentage") "${promo.value.toInt()}%" else "$${promo.value}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (promo.type == "percentage") MaterialTheme.colorScheme.primary else Color(0xFF9C27B0)
                        )
                    }
                }
                Text(promo.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, Modifier.size(14.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (promo.expiryDate > 0) stringResource(id = R.string.exp_label, dateFormat.format(Date(promo.expiryDate))) else stringResource(id = R.string.no_expiry),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.Group, null, Modifier.size(14.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${promo.usedCount}/${if (promo.usageLimit > 0) promo.usageLimit else "∞"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = promo.active,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4CAF50), checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.3f))
                )
                Row {
                    if (!promo.isIssued) {
                        IconButton(onClick = onPublish) {
                            Icon(Icons.Default.Campaign, null, tint = Color(0xFFFF9800))
                        }
                    } else {
                        IconButton(onClick = onUnpublish) { // Nút Thu hồi
                            Icon(Icons.Default.VolumeOff, null, tint = Color(0xFF757575))
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun AddPromoCodeDialog(onDismiss: () -> Unit, onConfirm: (PromoCodeModel) -> Unit) {
    var code by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("percentage") }
    var value by remember { mutableStateOf("") }
    var minOrder by remember { mutableStateOf("") }
    var maxDiscount by remember { mutableStateOf("") }
    var usageLimit by remember { mutableStateOf("") }
    var expiryDays by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.new_promo_code_title)) },
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text(stringResource(id = R.string.code_hint)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(id = R.string.product_description)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                Text(stringResource(id = R.string.discount_type_label), style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth()) {
                    FilterChip(selected = type == "percentage", onClick = { type = "percentage" }, label = { Text(stringResource(id = R.string.percentage_type)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = type == "fixed", onClick = { type = "fixed" }, label = { Text(stringResource(id = R.string.fixed_amount_type)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                }
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = value, onValueChange = { value = it },
                        label = { Text(if (type == "percentage") stringResource(id = R.string.value_percentage_label) else stringResource(id = R.string.value_fixed_label)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = minOrder, onValueChange = { minOrder = it }, label = { Text(stringResource(id = R.string.min_order_label)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                }
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = maxDiscount, onValueChange = { maxDiscount = it }, label = { Text(stringResource(id = R.string.max_discount_label)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = usageLimit, onValueChange = { usageLimit = it }, label = { Text(stringResource(id = R.string.usage_limit_label)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                }
                OutlinedTextField(value = expiryDays, onValueChange = { expiryDays = it }, label = { Text(stringResource(id = R.string.expires_in_days_label)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank() || value.isBlank()) return@Button
                    val expiryLong = if (expiryDays.isNotBlank()) System.currentTimeMillis() + (expiryDays.toLong() * 24 * 60 * 60 * 1000) else 0L
                    onConfirm(PromoCodeModel(code = code, type = type, value = value.toDoubleOrNull() ?: 0.0, description = description,
                        minOrder = minOrder.toDoubleOrNull() ?: 0.0, maxDiscount = maxDiscount.toDoubleOrNull() ?: 0.0,
                        expiryDate = expiryLong, usageLimit = usageLimit.toIntOrNull() ?: -1, active = true, 
                        isIssued = false, issuedAt = 0L))
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(id = R.string.create_btn)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } }
    )
}

// ── Dialog chỉnh sửa mã giảm giá ─────────────────────────────────────────
@Composable
fun EditPromoCodeDialog(
    promo: PromoCodeModel,
    onDismiss: () -> Unit,
    onConfirm: (PromoCodeModel) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var description by remember { mutableStateOf(promo.description) }
    var type by remember { mutableStateOf(promo.type) }
    var value by remember { mutableStateOf(promo.value.toString()) }
    var minOrder by remember { mutableStateOf(if (promo.minOrder > 0) promo.minOrder.toInt().toString() else "") }
    var maxDiscount by remember { mutableStateOf(if (promo.maxDiscount > 0) promo.maxDiscount.toInt().toString() else "") }
    var usageLimit by remember { mutableStateOf(if (promo.usageLimit > 0) promo.usageLimit.toString() else "") }
    val remainingDays = if (promo.expiryDate > 0)
        ((promo.expiryDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).coerceAtLeast(0).toString()
    else ""
    var expiryDays by remember { mutableStateOf(remainingDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(id = R.string.edit_promo_code_title), fontWeight = FontWeight.Bold)
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(promo.code, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.product_description)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                Text(stringResource(id = R.string.discount_type_label), style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth()) {
                    FilterChip(selected = type == "percentage", onClick = { type = "percentage" },
                        label = { Text(stringResource(id = R.string.percentage_type)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = type == "fixed", onClick = { type = "fixed" },
                        label = { Text(stringResource(id = R.string.fixed_amount_type)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                }
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = value, onValueChange = { value = it },
                        label = { Text(if (type == "percentage") stringResource(id = R.string.value_percentage_label) else stringResource(id = R.string.value_fixed_label)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = minOrder, onValueChange = { minOrder = it },
                        label = { Text(stringResource(id = R.string.min_order_label)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                }
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = maxDiscount, onValueChange = { maxDiscount = it },
                        label = { Text(stringResource(id = R.string.max_discount_label)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = usageLimit, onValueChange = { usageLimit = it },
                        label = { Text(stringResource(id = R.string.usage_limit_label)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                }
                OutlinedTextField(value = expiryDays, onValueChange = { expiryDays = it },
                    label = { Text(stringResource(id = R.string.extend_days_label)) },
                    supportingText = {
                        if (promo.expiryDate > 0)
                            Text(stringResource(id = R.string.current_expiry_label, dateFormat.format(Date(promo.expiryDate))), style = MaterialTheme.typography.labelSmall)
                    },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.used_count_readonly, promo.usedCount),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isBlank()) return@Button
                    val newExpiry = if (expiryDays.isNotBlank())
                        System.currentTimeMillis() + (expiryDays.toLongOrNull() ?: 0L) * 24 * 60 * 60 * 1000
                    else promo.expiryDate
                    onConfirm(promo.copy(description = description, type = type,
                        value = value.toDoubleOrNull() ?: promo.value,
                        minOrder = minOrder.toDoubleOrNull() ?: 0.0,
                        maxDiscount = maxDiscount.toDoubleOrNull() ?: 0.0,
                        usageLimit = usageLimit.toIntOrNull() ?: -1,
                        expiryDate = newExpiry,
                        isIssued = promo.isIssued,
                        issuedAt = promo.issuedAt))
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(id = R.string.save_changes_btn)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } }
    )
}
