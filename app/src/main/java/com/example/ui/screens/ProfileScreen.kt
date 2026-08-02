package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.BackgroundGradientCanvas
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSecondary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MoneyViewModel

@Composable
fun ProfileScreen(
    viewModel: MoneyViewModel,
    onOpenCatalog: () -> Unit = {},
    onOpenAdvancedSettings: () -> Unit = {},
    onOpenRanking: () -> Unit = {},
    onOpenContactRequests: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val allTx by viewModel.allTransactions.collectAsState()

    val showSettings by viewModel.showSettingsDialog.collectAsState()
    val showLogout by viewModel.showLogoutDialog.collectAsState()

    val isEmployerModeEnabled by viewModel.isEmployerModeEnabled.collectAsState()
    val isEmployeeModeEnabled by viewModel.isEmployeeModeEnabled.collectAsState()
    val isDistributorModeEnabled by viewModel.isDistributorModeEnabled.collectAsState()
    val pendingModeReactivation by viewModel.pendingModeReactivation.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateProfilePhoto(it.toString())
        }
    }

    BackgroundGradientCanvas(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Mi Perfil",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.testTag("profile_screen_title")
                    )
                }
            }

            // User Header Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    backgroundColor = Color(0xF5FFFFFF),
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .clickable { photoPickerLauncher.launch("image/*") }
                                .testTag("profile_photo_avatar")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.5.dp, PurplePrimary, CircleShape)
                                    .padding(3.dp)
                                    .clip(CircleShape)
                            ) {
                                AsyncImage(
                                    model = userProfile.photoUri ?: R.drawable.img_profile_avatar,
                                    contentDescription = "Foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(PurplePrimary)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Cambiar Foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.testTag("change_profile_photo_btn")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cambiar Foto de Perfil", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = userProfile.name.ifBlank { "Configurar Nombre" },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TextPrimary
                            ),
                            modifier = Modifier.testTag("user_profile_name")
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = userProfile.phone.ifBlank { "Sin teléfono configurado" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1F7C3AED))
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${allTx.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PurplePrimary
                                )
                                Text(text = "Transacciones", fontSize = 11.sp, color = TextSecondary)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "VIP Gold",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = PurplePrimary
                                )
                                Text(text = "Nivel", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Options List
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfileOptionCard(
                        title = "Mis Datos",
                        icon = Icons.Default.Person,
                        iconTint = PurplePrimary,
                        onClick = { viewModel.showEditProfileDialog.value = true },
                        onLongClick = {
                            viewModel.openPeekPreview(
                                com.example.ui.components.PeekPreviewType.FunctionInfo(
                                    title = "Mis Datos de Perfil",
                                    description = "Gestiona tu nombre público, número de teléfono y fotografía de perfil para la red MONEY.",
                                    icon = Icons.Default.Person,
                                    tips = listOf("Actualiza tu foto para que otros usuarios te reconozcan", "Permite vinculación rápida con empleadores y empleados")
                                )
                            )
                        },
                        testTag = "option_my_data"
                    )

                    ProfileOptionCard(
                        title = "Catálogo de Productos",
                        icon = Icons.Default.Store,
                        iconTint = PurplePrimary,
                        onClick = onOpenCatalog,
                        onLongClick = {
                            viewModel.openPeekPreview(
                                com.example.ui.components.PeekPreviewType.FunctionInfo(
                                    title = "Catálogo de Productos e Inventario",
                                    description = "Gestiona productos, precios, costos, stock actual y auditorías de inventario.",
                                    icon = Icons.Default.Store,
                                    tips = listOf("En Modo Empleador administra el stock principal", "En Modo Empleado permite enviar propuestas de cambios")
                                )
                            )
                        },
                        testTag = "option_catalog"
                    )

                    ProfileOptionCard(
                        title = "Ranking & Verificación",
                        icon = Icons.Default.Leaderboard,
                        iconTint = PurplePrimary,
                        onClick = onOpenRanking,
                        onLongClick = {
                            viewModel.openPeekPreview(
                                com.example.ui.components.PeekPreviewType.FunctionInfo(
                                    title = "Ranking & Insignias de Confianza",
                                    description = "Consulta tu reputación crediticia, volumen de ventas y nivel de verificación VIP.",
                                    icon = Icons.Default.Leaderboard,
                                    tips = listOf("Sube de nivel completando más de 10 transacciones mensuales", "Aumenta la velocidad de aprobación de tu cuenta")
                                )
                            )
                        },
                        testTag = "option_ranking"
                    )

                    ProfileOptionCard(
                        title = "Solicitudes de Contacto",
                        icon = Icons.Default.ContactMail,
                        iconTint = PurplePrimary,
                        onClick = onOpenContactRequests,
                        onLongClick = {
                            viewModel.openPeekPreview(
                                com.example.ui.components.PeekPreviewType.FunctionInfo(
                                    title = "Solicitudes de Contacto",
                                    description = "Administra mensajes entrantes y solicitudes de nuevos contactos o clientes.",
                                    icon = Icons.Default.ContactMail,
                                    tips = listOf("Acepta solicitudes para habilitar transferencias directas", "Filtra contactos spam o desconocidos")
                                )
                            )
                        },
                        testTag = "option_contact_requests"
                    )

                    ProfileOptionCard(
                        title = "Ajustes Avanzados",
                        icon = Icons.Default.Tune,
                        iconTint = PurplePrimary,
                        onClick = onOpenAdvancedSettings,
                        onLongClick = {
                            viewModel.openPeekPreview(
                                com.example.ui.components.PeekPreviewType.FunctionInfo(
                                    title = "Ajustes Avanzados P2P",
                                    description = "Configuración de red mesh Wi-Fi Direct, Bluetooth LE y respaldos encriptados de la base de datos.",
                                    icon = Icons.Default.Tune,
                                    tips = listOf("Exporta o importa archivos JSON de tu historial", "Configura la frecuencia de sincronización local")
                                )
                            )
                        },
                        testTag = "option_advanced_settings"
                    )

                    ProfileOptionCard(
                        title = "Configuración Rápida",
                        icon = Icons.Default.Settings,
                        iconTint = PurplePrimary,
                        onClick = { viewModel.showSettingsDialog.value = true },
                        onLongClick = {
                            viewModel.openPeekPreview(
                                com.example.ui.components.PeekPreviewType.FunctionInfo(
                                    title = "Configuración & Modos de Trabajo",
                                    description = "Activa o desactiva el Modo Empleador, Modo Empleado o Modo Distribuidor.",
                                    icon = Icons.Default.Settings,
                                    tips = listOf("Elige entre restaurar datos antiguos o comenzar con datos nuevos al re-activar modos", "Gestiona notificaciones y biometría")
                                )
                            )
                        },
                        testTag = "option_settings"
                    )

                    ProfileOptionCard(
                        title = "Ayuda y Soporte",
                        icon = Icons.Default.HelpOutline,
                        iconTint = PurplePrimary,
                        onClick = { viewModel.showHelpDialog.value = true },
                        testTag = "option_help"
                    )

                    ProfileOptionCard(
                        title = "Cerrar Sesión",
                        icon = Icons.Default.ExitToApp,
                        iconTint = ExpenseRed,
                        textColor = ExpenseRed,
                        onClick = { viewModel.showLogoutDialog.value = true },
                        testTag = "option_logout"
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }

    // Settings Modal
    if (showSettings) {
        Dialog(onDismissRequest = { viewModel.showSettingsDialog.value = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Configuración",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Seguridad Biométrica", color = TextPrimary)
                        Switch(
                            checked = userProfile.isBiometricsEnabled,
                            onCheckedChange = { viewModel.toggleBiometrics(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PurplePrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Notificaciones Push", color = TextPrimary)
                        Switch(
                            checked = userProfile.isNotificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PurplePrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Ajustes de Modos de Trabajo",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary,
                            fontSize = 15.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0F7C3AED))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Activar Modo Empleador",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Habilita gestión de empleados, inventario y aprobación de ramas.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isEmployerModeEnabled,
                                onCheckedChange = { viewModel.requestToggleEmployerMode(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PurplePrimary)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Activar Modo Empleado",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Habilita terminal POS, ventas y envío de propuestas de inventario.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isEmployeeModeEnabled,
                                onCheckedChange = { viewModel.requestToggleEmployeeMode(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PurplePrimary)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Activar Modo Distribuidor",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Habilita despacho de mercancía y vinculación de proveedor P2P.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isDistributorModeEnabled,
                                onCheckedChange = { viewModel.requestToggleDistributorMode(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PurplePrimary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    GlassButton(
                        text = "Cerrar",
                        onClick = { viewModel.showSettingsDialog.value = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Mode Reactivation Dialog (iOS Style)
    if (pendingModeReactivation != null) {
        val targetMode = pendingModeReactivation
        val modeName = when (targetMode) {
            com.example.data.model.AppMode.WORK_EMPLOYER -> "Modo Empleador"
            com.example.data.model.AppMode.WORK_EMPLOYEE -> "Modo Empleado"
            com.example.data.model.AppMode.WORK_DISTRIBUTOR -> "Modo Distribuidor"
            else -> "Modo"
        }
        Dialog(onDismissRequest = { viewModel.pendingModeReactivation.value = null }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = PurplePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Activar $modeName",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "¿Deseas volver a comenzar con los datos antiguos de este modo o prefieres iniciar con datos nuevos (limpieza)?",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 13.sp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    GlassButton(
                        text = "Mantener Datos Antiguos",
                        icon = Icons.Default.History,
                        isPrimary = false,
                        onClick = { viewModel.confirmModeReactivation(resetData = false) },
                        modifier = Modifier.fillMaxWidth().testTag("keep_old_data_btn")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GlassButton(
                        text = "Iniciar con Datos Nuevos",
                        icon = Icons.Default.Refresh,
                        isPrimary = true,
                        onClick = { viewModel.confirmModeReactivation(resetData = true) },
                        modifier = Modifier.fillMaxWidth().testTag("start_new_data_btn")
                    )
                }
            }
        }
    }
    // Logout Dialog
    if (showLogout) {
        AlertDialog(
            onDismissRequest = { viewModel.showLogoutDialog.value = false },
            title = { Text(text = "Cerrar Sesión", fontWeight = FontWeight.Bold) },
            text = { Text(text = "¿Estás seguro que deseas salir de tu cuenta MONEY?") },
            confirmButton = {
                TextButton(onClick = { viewModel.showLogoutDialog.value = false }) {
                    Text(text = "Salir", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showLogoutDialog.value = false }) {
                    Text(text = "Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileOptionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    textColor: Color = TextPrimary,
    testTag: String
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            ),
        cornerRadius = 18.dp,
        testTag = testTag
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    fontSize = 15.sp
                ),
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

