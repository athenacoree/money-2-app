package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

class MainActivity : ComponentActivity() {

    private val viewModel: MoneyViewModel by viewModels()

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
    ADVANCED_SETTINGS
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
                            onOpenAdvancedSettings = { activeSubScreen = SubScreen.ADVANCED_SETTINGS }
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
}

