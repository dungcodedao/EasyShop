package com.example.easyshop.admin

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.easyshop.AppUtil
import com.example.easyshop.R
import com.example.easyshop.util.LanguageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AdminProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var adminName by remember { mutableStateOf("Admin") }
    var adminEmail by remember { mutableStateOf(currentUser?.email ?: "") }
    var adminAvatar by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentAdminLang by remember { mutableStateOf(LanguageManager.getAdminLang(context)) }

    LaunchedEffect(Unit) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                adminName = doc.getString("name") ?: "Admin"
                adminAvatar = doc.getString("profileImg") ?: ""
                adminEmail = doc.getString("email") ?: currentUser.email ?: ""
            }
        }
    }

    val primaryIndigo = Color(0xFF4F46E5)
    val skyBlue = Color(0xFF0EA5E9)

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Back button (trái)
                    Box(modifier = Modifier.align(Alignment.CenterStart)) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Title (giữa)
                    Text(
                        text = stringResource(R.string.admin_profile_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Menu 3 gạch (phải) — giống bên User
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.cd_menu),
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
                            // Ngôn ngữ
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
                                        LanguageManager.displayName(currentAdminLang),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = primaryIndigo
                                    )
                                }
                            )

                            // Hỗ trợ
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

                            // Phiên bản (disabled)
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

                            // Đăng xuất
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.logout),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    auth.signOut()
                                    navController.navigate("auth") { popUpTo(0) { inclusive = true } }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.linearGradient(listOf(primaryIndigo, skyBlue))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (adminAvatar.isNotEmpty()) {
                                AsyncImage(
                                    model = adminAvatar,
                                    contentDescription = stringResource(R.string.cd_avatar),
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = adminName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = adminEmail, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Info card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            stringResource(R.string.account_info_label),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primaryIndigo
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        InfoRow(label = stringResource(R.string.full_name), value = adminName)
                        InfoRow(label = stringResource(R.string.email), value = adminEmail)
                        InfoRow(label = stringResource(R.string.role_label), value = stringResource(R.string.admin_label))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // --- Language Picker Dialog (Admin) ---
    if (showLanguageDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(stringResource(R.string.language), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
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
                                    if (tag == currentAdminLang) return@clickable
                                    currentAdminLang = tag
                                    showLanguageDialog = false
                                    val activity = (context as? Activity)
                                    LanguageManager.setAdminLang(context, tag)
                                    // Trên Android 13+ (API 33+), setApplicationLocales tự động recreate activity.
                                    // Với các phiên bản cũ hơn, ta tự gọi recreate kèm fade transition.
                                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                                        activity?.let { act ->
                                            act.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                            act.recreate()
                                        }
                                    }
                                }
                                .background(
                                    if (currentAdminLang == tag)
                                        primaryIndigo.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                            if (currentAdminLang == tag) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = primaryIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showLanguageDialog = false }
                ) { Text(stringResource(R.string.cancel)) }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF6B7280), fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
    HorizontalDivider(color = Color(0xFFF3F4F6))
}
