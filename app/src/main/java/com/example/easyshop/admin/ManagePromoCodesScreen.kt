package com.example.easyshop.admin

import android.widget.Toast
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.easyshop.R
import com.example.easyshop.model.PromoCodeModel
import com.example.easyshop.model.isExpired
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManagePromoCodesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var promoCodes by remember { mutableStateOf<List<PromoCodeModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    val firestore = Firebase.firestore

    fun loadPromoCodes() {
        isLoading = true
        firestore.collection("promoCodes")
            .get()
            .addOnSuccessListener { result ->
                promoCodes = result.documents.mapNotNull { it.toObject(PromoCodeModel::class.java) }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    LaunchedEffect(Unit) {
        loadPromoCodes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.manage_promo_codes_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back_to_home))
                    }
                },
                actions = {
                    IconButton(onClick = { loadPromoCodes() }) {
                        Icon(Icons.Default.Refresh, stringResource(id = R.string.reset))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, stringResource(id = R.string.add_promo_code_action))
            }
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (promoCodes.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ConfirmationNumber,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(id = R.string.no_promo_codes_found), color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = promoCodes,
                        key = { it.code }
                    ) { promo ->
                        PromoCodeItem(
                            promo = promo,
                            onToggleActive = {
                                firestore.collection("promoCodes")
                                    .document(promo.code)
                                    .update("active", !promo.active)
                                    .addOnSuccessListener { loadPromoCodes() }
                            },
                            onDelete = {
                                firestore.collection("promoCodes")
                                    .document(promo.code)
                                    .delete()
                                    .addOnSuccessListener { 
                                        loadPromoCodes()
                                        Toast.makeText(context, context.getString(R.string.deleted_promo_msg, promo.code), Toast.LENGTH_SHORT).show()
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
                firestore.collection("promoCodes")
                    .document(newPromo.code)
                    .set(newPromo)
                    .addOnSuccessListener {
                        showAddDialog = false
                        loadPromoCodes()
                        Toast.makeText(context, context.getString(R.string.added_promo_msg, newPromo.code), Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, context.getString(R.string.error_adding_promo), Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }
}

@Composable
fun PromoCodeItem(
    promo: PromoCodeModel,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val isExpired = promo.isExpired()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = promo.code,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (promo.active && !isExpired) Color(0xFF4CAF50) else Color.Gray
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if (promo.type == "percentage") Color(0xFFE3F2FD) else Color(0xFFF3E5F5),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (promo.type == "percentage") "${promo.value.toInt()}%" else "$${promo.value}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (promo.type == "percentage") Color(0xFF2196F3) else Color(0xFF9C27B0)
                        )
                    }
                }
                Text(
                    text = promo.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (promo.expiryDate > 0) stringResource(id = R.string.exp_label, dateFormat.format(Date(promo.expiryDate))) else stringResource(id = R.string.no_expiry),
                        fontSize = 12.sp,
                        color = if (isExpired) Color.Red else Color.Gray
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.Group, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${promo.usedCount}/${if (promo.usageLimit > 0) promo.usageLimit else "∞"}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = promo.active,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4CAF50))
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, stringResource(id = R.string.cancel_order), tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPromoCodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (PromoCodeModel) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("percentage") }
    var value by remember { mutableStateOf("") }
    var minOrder by remember { mutableStateOf("") }
    var maxDiscount by remember { mutableStateOf("") }
    var usageLimit by remember { mutableStateOf("") }
    
    // Simplistic date picker (optional improvement)
    var expiryDays by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.new_promo_code_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text(stringResource(id = R.string.code_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.product_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(stringResource(id = R.string.discount_type_label), style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == "percentage",
                        onClick = { type = "percentage" },
                        label = { Text(stringResource(id = R.string.percentage_type)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = type == "fixed",
                        onClick = { type = "fixed" },
                        label = { Text(stringResource(id = R.string.fixed_amount_type)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text(if (type == "percentage") stringResource(id = R.string.value_percentage_label) else stringResource(id = R.string.value_fixed_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = minOrder,
                        onValueChange = { minOrder = it },
                        label = { Text(stringResource(id = R.string.min_order_label)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = maxDiscount,
                        onValueChange = { maxDiscount = it },
                        label = { Text(stringResource(id = R.string.max_discount_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = usageLimit,
                        onValueChange = { usageLimit = it },
                        label = { Text(stringResource(id = R.string.usage_limit_label)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = expiryDays,
                    onValueChange = { expiryDays = it },
                    label = { Text(stringResource(id = R.string.expires_in_days_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank() || value.isBlank()) return@Button
                    
                    val expiryLong = if (expiryDays.isNotBlank()) {
                        System.currentTimeMillis() + (expiryDays.toLong() * 24 * 60 * 60 * 1000)
                    } else 0L

                    val newPromo = PromoCodeModel(
                        code = code,
                        type = type,
                        value = value.toDoubleOrNull() ?: 0.0,
                        description = description,
                        minOrder = minOrder.toDoubleOrNull() ?: 0.0,
                        maxDiscount = maxDiscount.toDoubleOrNull() ?: 0.0,
                        expiryDate = expiryLong,
                        usageLimit = usageLimit.toIntOrNull() ?: -1,
                        active = true
                    )
                    onConfirm(newPromo)
                }
            ) {
                Text(stringResource(id = R.string.create_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}
