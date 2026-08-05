package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.ui.components.DoveBirdSearchFab
import com.example.ui.components.GlobalSearchDialog
import com.example.ui.components.GlassBottomBar
import com.example.ui.components.NavTab
import com.example.ui.components.SecurityAuthDialog
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.dialogs.EditProfileDialog
import com.example.ui.dialogs.HelpDialog
import com.example.ui.dialogs.LinkProductsToTransactionDialog
import com.example.ui.dialogs.TransactionDetailDialog
import com.example.ui.dialogs.TransferSuccessDialog
import com.example.ui.dialogs.OnboardingTourDialog
import com.example.ui.dialogs.PermissionsDisclosureDialog
import com.example.ui.dialogs.PermissionDetail
import com.example.ui.screens.AddProductDialog
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.AdvancedSettingsScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.IndividualChatScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.EmployeeScreen
import com.example.ui.screens.DistributorDashboardScreen
import com.example.ui.components.IPhonePeekPreviewDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TransferScreen
import com.example.ui.theme.MONEYTheme
import com.example.ui.viewmodel.MoneyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: MoneyViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "App opened, resetting launcher icon to Normal")
        setLauncherIcon("NormalAlias")
    }

    fun setLauncherIcon(aliasName: String) {
        val packageManager = packageManager
        val packageName = packageName

        val aliases = listOf(
            "NormalAlias",
            "IngresoAlias",
            "GastoAlias",
            "TendenciaPosAlias",
            "TendenciaNegAlias"
        )

        aliases.forEach { alias ->
            val compName = android.content.ComponentName(packageName, "$packageName.$alias")
            val state = if (alias == aliasName) {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                packageManager.setComponentEnabledSetting(
                    compName,
                    state,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                android.util.Log.e("LauncherIcon", "Error setting enabled state for $alias: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MONEYTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    MoneyMainApp(viewModel = viewModel)
                }
            }
        }
    }
}

enum class SubScreen {
    NONE,
    ADD_INCOME,
    ADD_EXPENSE,
    CATALOG,
    CHAT_LIST,
    CHAT_DETAIL,
    ADVANCED_SETTINGS,
    DATA_USAGE
}

@Composable
fun MoneyMainApp(viewModel: MoneyViewModel) {
    var currentTab by remember { mutableStateOf<NavTab>(NavTab.Home) }
    var activeSubScreen by remember { mutableStateOf(SubScreen.NONE) }

    val appMode by viewModel.appMode.collectAsState()
    val showAddTx by viewModel.showAddTransactionDialog.collectAsState()
    val showTransferSuccess by viewModel.showTransferSuccessDialog.collectAsState()
    val lastRecipient by viewModel.lastTransferRecipient.collectAsState()
    val showEditProfile by viewModel.showEditProfileDialog.collectAsState()
    val showHelp by viewModel.showHelpDialog.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val selectedTxForDetail by viewModel.selectedTransactionForDetail.collectAsState()
    val selectedTxForReconciliation by viewModel.selectedTransactionForReconciliation.collectAsState()

    val activeChat by viewModel.activeChat.collectAsState()
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    val isQvaPayEnabled by viewModel.isQvaPayEnabled.collectAsState()

    val showOnboardingTour by viewModel.showOnboardingTour.collectAsState()
    val showPermissionsDisclosure by viewModel.showPermissionsDisclosure.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionsToExplain = remember {
        listOf(
            com.example.ui.dialogs.PermissionDetail(
                title = "Leer y Recibir SMS",
                description = "Permite detectar automáticamente tus transferencias de Transfermóvil/EnZona y mensajes de Cubacel para registrarlos sin tener que anotarlos a mano.",
                consequence = "Sin este permiso, el registro automático quedará deshabilitado y tendrás que ingresar cada transacción manualmente.",
                icon = Icons.Default.Sms,
                color = com.example.ui.theme.IncomeGreen,
                systemPermissions = listOf(
                    android.Manifest.permission.READ_SMS,
                    android.Manifest.permission.RECEIVE_SMS
                )
            ),
            com.example.ui.dialogs.PermissionDetail(
                title = "Enviar SMS",
                description = "Permite al Empleador enviar mensajes de confirmación de saldo y ventas directamente al teléfono del Empleado activo.",
                consequence = "Sin este permiso, la confirmación de transacciones multi-dispositivo automática quedará deshabilitada.",
                icon = Icons.Default.Send,
                color = com.example.ui.theme.PurplePrimary,
                systemPermissions = listOf(
                    android.Manifest.permission.SEND_SMS
                )
            ),
            com.example.ui.dialogs.PermissionDetail(
                title = "Ejecutar Llamadas (USSD)",
                description = "Permite enviar códigos USSD (*222#) directamente a la red de ETECSA para actualizar tu saldo y bonos Cubacel de forma instantánea.",
                consequence = "Sin este permiso, la consulta rápida de saldo mediante USSD quedará deshabilitada.",
                icon = Icons.Default.PhoneCallback,
                color = androidx.compose.ui.graphics.Color(0xFFFF9800),
                systemPermissions = listOf(
                    android.Manifest.permission.CALL_PHONE
                )
            ),
            com.example.ui.dialogs.PermissionDetail(
                title = "Ubicación (Sincronización P2P)",
                description = "Permite buscar e interactuar con otros dispositivos en la misma red Wi-Fi para sincronizar catálogos y propuestas entre Empleador y Empleado.",
                consequence = "Sin este permiso, la sincronización local P2P por sockets no funcionará.",
                icon = Icons.Default.WifiTethering,
                color = androidx.compose.ui.graphics.Color(0xFF3B82F6),
                systemPermissions = listOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ),
            com.example.ui.dialogs.PermissionDetail(
                title = "Estadísticas de Uso (Consumo de Datos)",
                description = "Permite calcular cuántos megabytes ha consumido tu teléfono del paquete móvil de ETECSA en tiempo real.",
                consequence = "Sin este permiso, el medidor de consumo de datos móviles no podrá mostrar el gasto real del paquete.",
                icon = Icons.Default.DataUsage,
                color = androidx.compose.ui.graphics.Color(0xFFE91E63),
                systemPermissions = emptyList()
            )
        )
    }

    val requestPermissionsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Handle results if needed
    }

    // Filter to only those permissions currently ungranted
    val ungrantedPermissions = remember(showPermissionsDisclosure) {
        if (showPermissionsDisclosure) {
            permissionsToExplain.filter { perm ->
                if (perm.systemPermissions.isEmpty()) {
                    !viewModel.hasUsageStatsPermission()
                } else {
                    perm.systemPermissions.any { sysPerm ->
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            sysPerm
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                }
            }
        } else {
            emptyList()
        }
    }

    val mainActivity = context as? MainActivity
    LaunchedEffect(Unit) {
        viewModel.activeLauncherIcon.collect { iconName ->
            if (iconName != "NormalAlias") {
                mainActivity?.setLauncherIcon(iconName)
            }
        }
    }

    // Fallback tab if QvaPay is disabled while on Transfer tab
    LaunchedEffect(isQvaPayEnabled) {
        if (!isQvaPayEnabled && currentTab == NavTab.Transfer) {
            currentTab = NavTab.Home
        }
    }

    Scaffold(
        bottomBar = {
            GlassBottomBar(
                currentRoute = currentTab.route,
                onTabSelected = { tab ->
                    activeSubScreen = SubScreen.NONE
                    currentTab = tab
                },
                isQvaPayEnabled = isQvaPayEnabled
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Check mode or subscreen overlay
            if (activeSubScreen != SubScreen.NONE) {
                when (activeSubScreen) {
                    SubScreen.ADD_INCOME -> AddTransactionScreen(
                        viewModel = viewModel,
                        initialTypeString = "ingreso",
                        onBack = { activeSubScreen = SubScreen.NONE }
                    )
                    SubScreen.ADD_EXPENSE -> AddTransactionScreen(
                        viewModel = viewModel,
                        initialTypeString = "gasto",
                        onBack = { activeSubScreen = SubScreen.NONE }
                    )
                    SubScreen.CATALOG -> CatalogScreen(
                        viewModel = viewModel,
                        onBack = { activeSubScreen = SubScreen.NONE }
                    )
                    SubScreen.CHAT_LIST -> ChatListScreen(
                        viewModel = viewModel,
                        onBack = { activeSubScreen = SubScreen.NONE }
                    )
                    SubScreen.CHAT_DETAIL -> activeChat?.let { chat ->
                        IndividualChatScreen(
                            viewModel = viewModel,
                            conversation = chat,
                            onBack = { activeSubScreen = SubScreen.CHAT_LIST }
                        )
                    }
                    SubScreen.ADVANCED_SETTINGS -> AdvancedSettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSubScreen = SubScreen.NONE }
                    )
                    SubScreen.DATA_USAGE -> com.example.ui.screens.DataUsageScreen(
                        viewModel = viewModel,
                        onBack = { activeSubScreen = SubScreen.NONE }
                    )
                    SubScreen.NONE -> {}
                }
            } else if (appMode == AppMode.WORK_EMPLOYER) {
                com.example.ui.screens.EmployerScreen(
                    viewModel = viewModel,
                    onOpenCatalog = { activeSubScreen = SubScreen.CATALOG },
                    onBackToPersonal = { viewModel.setAppMode(AppMode.PERSONAL) }
                )
            } else if (appMode == AppMode.WORK_EMPLOYEE) {
                EmployeeScreen(
                    viewModel = viewModel,
                    onOpenCatalog = { activeSubScreen = SubScreen.CATALOG },
                    onBackToPersonal = { viewModel.setAppMode(AppMode.PERSONAL) }
                )
            } else if (appMode == AppMode.WORK_DISTRIBUTOR) {
                DistributorDashboardScreen(
                    viewModel = viewModel,
                    onBackToPersonal = { viewModel.setAppMode(AppMode.PERSONAL) }
                )
            } else {
                Crossfade(
                    targetState = currentTab,
                    animationSpec = tween(durationMillis = 300),
                    label = "tabCrossfade"
                ) { tab ->
                    when (tab) {
                        NavTab.Home -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateTab = { targetTab -> currentTab = targetTab },
                            onOpenAddIncome = { activeSubScreen = SubScreen.ADD_INCOME },
                            onOpenAddExpense = { activeSubScreen = SubScreen.ADD_EXPENSE },
                            onOpenCatalog = { activeSubScreen = SubScreen.CATALOG },
                            onOpenChat = { activeSubScreen = SubScreen.CHAT_LIST }
                        )
                        NavTab.Catalog -> CatalogScreen(
                            viewModel = viewModel,
                            onBack = { currentTab = NavTab.Home }
                        )
                        NavTab.History -> HistoryScreen(viewModel = viewModel)
                        NavTab.Transfer -> TransferScreen(viewModel = viewModel)
                        NavTab.Profile -> ProfileScreen(
                            viewModel = viewModel,
                            onOpenCatalog = { activeSubScreen = SubScreen.CATALOG },
                            onOpenAdvancedSettings = { activeSubScreen = SubScreen.ADVANCED_SETTINGS },
                            onOpenDataUsage = { activeSubScreen = SubScreen.DATA_USAGE }
                        )
                    }
                }
            }

            // Floating Dove/Bird Search FAB
            DoveBirdSearchFab(
                onClick = { viewModel.showGlobalSearchDialog.value = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
            )
        }
    }

    // Modals & Dialogs
    selectedTxForDetail?.let { tx ->
        TransactionDetailDialog(
            viewModel = viewModel,
            transaction = tx,
            onDismiss = { viewModel.selectedTransactionForDetail.value = null }
        )
    }

    if (showAddTx) {
        AddTransactionDialog(
            onDismiss = { viewModel.showAddTransactionDialog.value = false },
            onConfirm = { title, category, description, amount, type ->
                val cal = Calendar.getInstance()
                val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                viewModel.addTransaction(
                    monto = amount,
                    categoria = category,
                    descripcion = description,
                    fecha = cal.timeInMillis,
                    hora = format.format(cal.time),
                    metodoPago = "Efectivo",
                    esEmpleador = (appMode == AppMode.WORK_EMPLOYER || appMode == AppMode.WORK_EMPLOYEE),
                    tipo = type
                )
                viewModel.showAddTransactionDialog.value = false
            }
        )
    }

    if (showTransferSuccess) {
        TransferSuccessDialog(
            recipient = lastRecipient,
            onDismiss = { viewModel.dismissTransferSuccessDialog() }
        )
    }

    if (showEditProfile) {
        EditProfileDialog(
            currentName = userProfile.name,
            currentPhone = userProfile.phone,
            onDismiss = { viewModel.showEditProfileDialog.value = false },
            onSave = { newName, newPhone ->
                viewModel.updateUserProfile(newName, newPhone)
            }
        )
    }

    if (showHelp) {
        HelpDialog(
            onDismiss = { viewModel.showHelpDialog.value = false }
        )
    }

    if (showAddProductDialog) {
        AddProductDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showAddProductDialog.value = false }
        )
    }

    selectedTxForReconciliation?.let { tx ->
        LinkProductsToTransactionDialog(
            viewModel = viewModel,
            transaction = tx,
            onDismiss = { viewModel.selectedTransactionForReconciliation.value = null }
        )
    }

    val activePeekPreview by viewModel.activePeekPreview.collectAsState()
    IPhonePeekPreviewDialog(
        peekType = activePeekPreview,
        onDismiss = { viewModel.closePeekPreview() }
    )

    val showGlobalSearch by viewModel.showGlobalSearchDialog.collectAsState()
    if (showGlobalSearch) {
        GlobalSearchDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showGlobalSearchDialog.value = false },
            onNavigateToCatalog = {
                activeSubScreen = SubScreen.CATALOG
            },
            onNavigateToHistory = {
                activeSubScreen = SubScreen.NONE
                currentTab = NavTab.History
            },
            onNavigateToTransfer = {
                activeSubScreen = SubScreen.NONE
                currentTab = NavTab.Transfer
            },
            onNavigateToProfile = {
                activeSubScreen = SubScreen.NONE
                currentTab = NavTab.Profile
            }
        )
    }

    val isAuthDialogVisible by viewModel.isAuthDialogVisible.collectAsState()
    val authDialogTitle by viewModel.authDialogTitle.collectAsState()
    val authDialogReason by viewModel.authDialogReason.collectAsState()
    val userSecurityPin by viewModel.userSecurityPin.collectAsState()

    if (isAuthDialogVisible) {
        SecurityAuthDialog(
            title = authDialogTitle,
            reason = authDialogReason,
            userPin = userSecurityPin,
            onSuccess = { viewModel.onSecurityAuthSuccess() },
            onCancel = { viewModel.onSecurityAuthCancel() }
        )
    }

    // Onboarding Tour Dialog
    if (showOnboardingTour) {
        com.example.ui.dialogs.OnboardingTourDialog(
            onDismiss = { viewModel.setOnboardingFinished() },
            onFinished = { viewModel.setOnboardingFinished() }
        )
    } else if (showPermissionsDisclosure && ungrantedPermissions.isNotEmpty()) {
        com.example.ui.dialogs.PermissionsDisclosureDialog(
            permissionsToExplain = ungrantedPermissions,
            onDismiss = { viewModel.setPermissionsDisclosedFinished() },
            onApproved = { perm ->
                if (perm.systemPermissions.isEmpty()) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                } else {
                    requestPermissionsLauncher.launch(perm.systemPermissions.toTypedArray())
                }
            }
        )
    }

    // Employer SMS Confirmation Pending alert
    val pendingEmployerConfirmationTx by viewModel.pendingEmployerConfirmationTx.collectAsState()
    pendingEmployerConfirmationTx?.let { tx ->
        val activeEmpId by viewModel.empleadoActivoId.collectAsState()
        val activeEmp = viewModel.activeEmployees.collectAsState().value.find { it.id == activeEmpId }
        val empName = activeEmp?.nombre ?: "Empleado Activo"

        Dialog(onDismissRequest = { viewModel.pendingEmployerConfirmationTx.value = null }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Nueva Transferencia Detectada",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "¿Deseas enviar un SMS automático de confirmación a $empName sobre este pago de ${tx.monto} ${tx.moneda}?",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.pendingEmployerConfirmationTx.value = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("No enviar", color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }

                        GlassButton(
                            text = "Enviar SMS",
                            isPrimary = true,
                            onClick = {
                                viewModel.sendConfirmationSmsToActiveEmployee(tx)
                                viewModel.pendingEmployerConfirmationTx.value = null
                            },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }
        }
    }

    // Employee Ambiguous Waits Duplicate selection alert
    val ambiguousConfirmations by viewModel.ambiguousConfirmations.collectAsState()
    val ambiguousParsedId by viewModel.ambiguousParsedId.collectAsState()

    if (ambiguousConfirmations.isNotEmpty()) {
        Dialog(onDismissRequest = { viewModel.ambiguousConfirmations.value = emptyList() }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Ambigüedad de Venta",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Se recibió un pago, pero tienes múltiples ventas en espera con el mismo monto. Selecciona cuál de ellas es la correcta para conciliar:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(ambiguousConfirmations) { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x0F7C3AED))
                                    .clickable {
                                        viewModel.selectAmbiguousConfirmation(tx)
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.descripcion.replace("[Espera] ", ""), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Monto: ${tx.monto} CUP | Hora: ${tx.hora}", fontSize = 11.sp, color = TextSecondary)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PurplePrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "ID Referencia: $ambiguousParsedId",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.ambiguousConfirmations.value = emptyList() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

