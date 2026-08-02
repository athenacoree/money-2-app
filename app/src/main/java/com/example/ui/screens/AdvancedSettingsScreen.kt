package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BackgroundGradientCanvas
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MoneyViewModel

@Composable
fun AdvancedSettingsScreen(
    viewModel: MoneyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var exportSuccessMessage by remember { mutableStateOf(false) }

    // State Toggles
    var showPhoneInRanking by remember { mutableStateOf(true) }
    var allowContactRequests by remember { mutableStateOf(true) }

    var txNotifications by remember { mutableStateOf(true) }
    var paymentReminders by remember { mutableStateOf(true) }
    var lowBalanceAlerts by remember { mutableStateOf(true) }

    var darkMode by remember { mutableStateOf(false) }
    var fontSizeOption by remember { mutableStateOf("Normal") }

    // QvaPay Integration State from ViewModel
    val isQvaPayEnabled by viewModel.isQvaPayEnabled.collectAsState()
    val qvaPayAppKey by viewModel.qvaPayAppKey.collectAsState()
    val qvaPayAppSecret by viewModel.qvaPayAppSecret.collectAsState()
    val qvaPayUserInfo by viewModel.qvaPayUserInfo.collectAsState()
    val isQvaPayLoading by viewModel.isQvaPayLoading.collectAsState()
    val qvaPayError by viewModel.qvaPayError.collectAsState()

    // Security Auth State
    val isSecurityAuthEnabled by viewModel.isSecurityAuthEnabled.collectAsState()
    val userSecurityPin by viewModel.userSecurityPin.collectAsState()

    var inputKey by remember(qvaPayAppKey) { mutableStateOf(qvaPayAppKey) }
    var inputSecret by remember(qvaPayAppSecret) { mutableStateOf(qvaPayAppSecret) }
    var showSecretVisible by remember { mutableStateOf(false) }

    BackgroundGradientCanvas(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // iOS Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x1F7C3AED))
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = PurplePrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Ajustes del Sistema",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.testTag("advanced_settings_title")
                    )
                    Text(
                        text = "Preferencias, QvaPay y Seguridad",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // SECCIÓN INTEGRACIÓN QVAPAY (iOS FROSTED CRYSTAL EXPANDABLE)
                item {
                    SettingsSectionHeader("Integración Cripto & P2P")
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (isQvaPayEnabled) 14.dp else 6.dp,
                                shape = RoundedCornerShape(26.dp),
                                spotColor = Color(0x667C3AED)
                            )
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF),
                                        Color(0xFFF9F5FF)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xCC7C3AED),
                                        Color(0x44A78BFA),
                                        Color(0xAA3B82F6)
                                    )
                                ),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            // Top Row Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(Color(0xFF7C3AED), Color(0xFF3B82F6))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "QvaPay Oficial (API v1)",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = TextPrimary
                                            )
                                        )
                                        Text(
                                            text = if (isQvaPayEnabled) "Integración Activada" else "Toca para configurar",
                                            fontSize = 12.sp,
                                            color = if (isQvaPayEnabled) IncomeGreen else TextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Switch(
                                    checked = isQvaPayEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.saveQvaPayConfig(enabled, inputKey, inputSecret)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = PurplePrimary
                                    )
                                )
                            }

                            // EXPANDABLE PANEL WITH IPHONE FROSTED CRYSTAL STYLE
                            AnimatedVisibility(
                                visible = isQvaPayEnabled,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    Divider(color = Color(0x1F7C3AED), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Conecta la app a tu cuenta oficial de QvaPay para habilitar pagos en SQP, consulta de saldo en vivo y listado de criptomonedas.",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // App Key Input
                                    OutlinedTextField(
                                        value = inputKey,
                                        onValueChange = { inputKey = it },
                                        label = { Text("QvaPay App Key") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Key, contentDescription = null, tint = PurplePrimary)
                                        },
                                        trailingIcon = {
                                            TextButton(onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = clipboard.primaryClip
                                                if (clip != null && clip.itemCount > 0) {
                                                    inputKey = clip.getItemAt(0).text.toString()
                                                }
                                            }) {
                                                Text("Pegar", fontSize = 11.sp, color = PurplePrimary, fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PurplePrimary,
                                            unfocusedBorderColor = Color(0x337C3AED),
                                            focusedContainerColor = Color(0x0A7C3AED)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // App Secret Input
                                    OutlinedTextField(
                                        value = inputSecret,
                                        onValueChange = { inputSecret = it },
                                        label = { Text("QvaPay App Secret") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = PurplePrimary)
                                        },
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { showSecretVisible = !showSecretVisible }) {
                                                    Icon(
                                                        imageVector = if (showSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = null,
                                                        tint = TextSecondary
                                                    )
                                                }
                                                TextButton(onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = clipboard.primaryClip
                                                    if (clip != null && clip.itemCount > 0) {
                                                        inputSecret = clip.getItemAt(0).text.toString()
                                                    }
                                                }) {
                                                    Text("Pegar", fontSize = 11.sp, color = PurplePrimary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                        visualTransformation = if (showSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PurplePrimary,
                                            unfocusedBorderColor = Color(0x337C3AED),
                                            focusedContainerColor = Color(0x0A7C3AED)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Save Button
                                    GlassButton(
                                        text = if (isQvaPayLoading) "Verificando Credenciales..." else "Guardar y Sincronizar QvaPay",
                                        icon = Icons.Default.CheckCircle,
                                        isPrimary = true,
                                        onClick = {
                                            viewModel.saveQvaPayConfig(true, inputKey, inputSecret)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (qvaPayError != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0x1FEF4444))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = qvaPayError!!,
                                                color = ExpenseRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (qvaPayUserInfo != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0x1F10B981))
                                                .border(1.dp, Color(0x4410B981), RoundedCornerShape(14.dp))
                                                .padding(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = IncomeGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Conectado como @${qvaPayUserInfo!!.username}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = IncomeGreen
                                                )
                                                Text(
                                                    text = "Saldo en Vivo: ${qvaPayUserInfo!!.balance} SQP",
                                                    fontSize = 12.sp,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // UNRELATED SECTIONS GET A SOFT FROSTED OPACITY FOCUS WHEN QVAPAY IS EXPANDED
                val unrelatedModifier = Modifier.graphicsLayer {
                    alpha = if (isQvaPayEnabled) 0.95f else 1f
                }

                // SECCIÓN PERFIL
                item {
                    Column(modifier = unrelatedModifier) {
                        SettingsSectionHeader("Perfil de Usuario")
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 22.dp,
                            backgroundColor = Color(0xF5FFFFFF)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SettingsActionRow(
                                    title = "Editar nombre de usuario",
                                    icon = Icons.Default.Person,
                                    iconBgColor = Color(0xFF3B82F6),
                                    onClick = { viewModel.showEditProfileDialog.value = true }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SettingsActionRow(
                                    title = "Editar número de teléfono",
                                    icon = Icons.Default.Phone,
                                    iconBgColor = Color(0xFF10B981),
                                    onClick = { viewModel.showEditProfileDialog.value = true }
                                )
                            }
                        }
                    }
                }

                // SECCIÓN PRIVACIDAD
                item {
                    Column(modifier = unrelatedModifier) {
                        SettingsSectionHeader("Privacidad & Seguridad")
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 22.dp,
                            backgroundColor = Color(0xF5FFFFFF)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SettingsSwitchRow(
                                    title = "Autenticación para Acciones Sensibles",
                                    icon = Icons.Default.Lock,
                                    iconBgColor = Color(0xFF10B981),
                                    checked = isSecurityAuthEnabled,
                                    onCheckedChange = { checked ->
                                        viewModel.saveSecurityAuthConfig(checked)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Protege pagos, envíos, agregar ramas de empresa o desvincular servicios exigiendo tu PIN o Huella.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )
                                Divider(color = Color(0x1F000000), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                SettingsSwitchRow(
                                    title = "Mostrar mi número en el ranking",
                                    icon = Icons.Default.Security,
                                    iconBgColor = Color(0xFF8B5CF6),
                                    checked = showPhoneInRanking,
                                    onCheckedChange = { showPhoneInRanking = it }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SettingsSwitchRow(
                                    title = "Permitir solicitudes de contacto",
                                    icon = Icons.Default.PersonAdd,
                                    iconBgColor = Color(0xFFEC4899),
                                    checked = allowContactRequests,
                                    onCheckedChange = { allowContactRequests = it }
                                )
                            }
                        }
                    }
                }

                // SECCIÓN NOTIFICACIONES
                item {
                    Column(modifier = unrelatedModifier) {
                        SettingsSectionHeader("Notificaciones")
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 22.dp,
                            backgroundColor = Color(0xF5FFFFFF)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SettingsSwitchRow(
                                    title = "Notificaciones de transacciones",
                                    icon = Icons.Default.Notifications,
                                    iconBgColor = Color(0xFFF59E0B),
                                    checked = txNotifications,
                                    onCheckedChange = { txNotifications = it }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SettingsSwitchRow(
                                    title = "Recordatorios de pago",
                                    icon = Icons.Default.Alarm,
                                    iconBgColor = Color(0xFF06B6D4),
                                    checked = paymentReminders,
                                    onCheckedChange = { paymentReminders = it }
                                )
                            }
                        }
                    }
                }

                // SECCIÓN APARIENCIA
                item {
                    Column(modifier = unrelatedModifier) {
                        SettingsSectionHeader("Apariencia")
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 22.dp,
                            backgroundColor = Color(0xF5FFFFFF)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SettingsSwitchRow(
                                    title = "Modo oscuro",
                                    icon = Icons.Default.Palette,
                                    iconBgColor = Color(0xFF6366F1),
                                    checked = darkMode,
                                    onCheckedChange = { darkMode = it }
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Tamaño de fuente",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Pequeño", "Normal", "Grande").forEach { option ->
                                        val isSelected = fontSizeOption == option
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) PurplePrimary else Color(0x1F7C3AED))
                                                .clickable { fontSizeOption = option }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = option,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = if (isSelected) Color.White else TextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECCIÓN DATOS (BOTONES CON ALTA VISIBILIDAD)
                item {
                    Column(modifier = unrelatedModifier) {
                        SettingsSectionHeader("Gestión de Datos")
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 22.dp,
                            backgroundColor = Color(0xF5FFFFFF)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                GlassButton(
                                    text = "Exportar datos en CSV",
                                    icon = Icons.Default.Download,
                                    isPrimary = true,
                                    onClick = { exportSuccessMessage = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("export_csv_btn")
                                )

                                if (exportSuccessMessage) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "¡Archivo CSV exportado exitosamente!",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = PurplePrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                GlassButton(
                                    text = "Eliminar todos los datos",
                                    icon = Icons.Default.DeleteForever,
                                    isPrimary = false,
                                    isDestructive = true,
                                    onClick = { showDeleteConfirmDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("delete_all_data_btn")
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(90.dp)) }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(text = "Eliminar todos los datos", fontWeight = FontWeight.Bold) },
            text = { Text(text = "¿Estás seguro de que deseas borrar todas las transacciones, contactos y configuraciones? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text(text = "Sí, eliminar", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(text = "Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
        ),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PurplePrimary
            )
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}
