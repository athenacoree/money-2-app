package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AppMode
import com.example.data.model.Transaction
import com.example.data.model.Producto
import com.example.data.model.Empleado
import com.example.data.model.Contacto
import com.example.data.model.Mensaje
import com.example.data.model.Configuracion
import com.example.data.model.AuditoriaStock
import com.example.data.model.PropuestaCambio
import com.example.data.model.DespachoDistribuidor
import com.example.data.model.BranchInfo
import com.example.data.model.EtecsaMobileBalance
import com.example.data.qvapay.QvaPayApiService
import com.example.data.qvapay.QvaPayCoin
import com.example.data.qvapay.QvaPayUserInfo
import com.example.data.qvapay.QvaPayTransaction
import com.example.data.qvapay.QvaPayInvoice
import com.example.ui.components.PeekPreviewType
import com.example.data.model.DynamicIconBadge
import com.example.data.model.DynamicIconCatalog
import com.example.data.repository.MoneyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UserProfileState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val photoUri: String? = null,
    val memberSince: String = "",
    val isBiometricsEnabled: Boolean = true,
    val isNotificationsEnabled: Boolean = true
)

data class EmployerInfo(
    val name: String = "Carlos (Empleador)",
    val phone: String = "+53 5234 5678",
    val photoUri: String? = null
)

enum class HistoryFilter {
    ALL, TODAY, THIS_WEEK, THIS_MONTH, INCOME, EXPENSE
}

class MoneyViewModel(application: Application) : AndroidViewModel(application) {

    val repository: MoneyRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = MoneyRepository(
            transactionDao = db.transactionDao(),
            productoDao = db.productoDao(),
            empleadoDao = db.empleadoDao(),
            contactoDao = db.contactoDao(),
            mensajeDao = db.mensajeDao(),
            configuracionDao = db.configuracionDao(),
            auditoriaStockDao = db.auditoriaStockDao(),
            propuestaCambioDao = db.propuestaCambioDao()
        )
        viewModelScope.launch {
            repository.checkAndSeedData()
            // Initial load of config values
            loadConfigurations()
            // Ensure some products exist in work catalog
            checkAndSeedProducts()

            // AUTOMATIC REAL-TIME DYNAMIC ICON DETECTION ENGINE
            launch {
                combine(allTransactions, _appMode, despachosDistribuidor) { txs, mode, despachos ->
                    Triple(txs, mode, despachos)
                }.collect { (txs, mode, despachos) ->
                    evaluateAutomaticDynamicIcon(txs, mode, despachos)
                }
            }
        }
    }

    private fun evaluateAutomaticDynamicIcon(
        txList: List<Transaction>,
        mode: AppMode,
        despachos: List<DespachoDistribuidor>
    ) {
        val now = System.currentTimeMillis()
        val latestTx = txList.maxByOrNull { it.fecha }

        val detectedBadge = when {
            // Event 1: Recent QvaPay payment or SQP currency (within 48h)
            latestTx != null && (now - latestTx.fecha <= 172800000L) &&
            (latestTx.metodo_pago.contains("QvaPay", ignoreCase = true) || latestTx.moneda == "SQP") -> {
                DynamicIconCatalog.ALL_ICONS.find { it.id == "QVAPAY_SQP" }
            }
            // Event 2: Recent SMS or Transfermóvil transaction
            latestTx != null && (now - latestTx.fecha <= 172800000L) &&
            (latestTx.metodo_pago.contains("Transfermóvil", ignoreCase = true) || latestTx.metodo_pago.contains("SMS", ignoreCase = true)) -> {
                DynamicIconCatalog.ALL_ICONS.find { it.id == "TRANSFERMOVIL_CUP" }
            }
            // Event 3: Recent Tether USDT or Cripto transaction
            latestTx != null && (now - latestTx.fecha <= 172800000L) &&
            (latestTx.moneda == "USDT" || latestTx.categoria.contains("Cripto", ignoreCase = true)) -> {
                DynamicIconCatalog.ALL_ICONS.find { it.id == "CRYPTO_USDT" }
            }
            // Event 4: Recent Bitcoin transaction
            latestTx != null && (now - latestTx.fecha <= 172800000L) &&
            (latestTx.moneda == "BTC") -> {
                DynamicIconCatalog.ALL_ICONS.find { it.id == "CRYPTO_BTC" }
            }
            // Event 5: Recent Business Sale or Employer mode transaction
            latestTx != null && (now - latestTx.fecha <= 172800000L) &&
            (latestTx.categoria.contains("Ventas", ignoreCase = true) || latestTx.es_empleador) -> {
                DynamicIconCatalog.ALL_ICONS.find { it.id == "BUSINESS_SALE" }
            }
            // Event 6: Recent Distributor cargo dispatch
            despachos.isNotEmpty() && (now - (despachos.maxOfOrNull { it.timestamp } ?: 0L) <= 172800000L) -> {
                DynamicIconCatalog.ALL_ICONS.find { it.id == "DISTRIBUTOR_DISPATCH" }
            }
            // Event 7: Financial Trend calculation if transactions exist
            txList.isNotEmpty() -> {
                val inc = txList.filter { it.tipo == "ingreso" }.sumOf { it.monto }
                val exp = txList.filter { it.tipo == "gasto" }.sumOf { it.monto }
                val net = inc - exp
                when {
                    net >= 10000 -> DynamicIconCatalog.ALL_ICONS.find { it.id == "TREND_UP_10" }
                    net >= 2500 -> DynamicIconCatalog.ALL_ICONS.find { it.id == "TREND_UP_5" }
                    net > 0 -> DynamicIconCatalog.ALL_ICONS.find { it.id == "TREND_UP_1" }
                    net <= -2500 -> DynamicIconCatalog.ALL_ICONS.find { it.id == "TREND_DOWN_5" }
                    net < 0 -> DynamicIconCatalog.ALL_ICONS.find { it.id == "TREND_DOWN_1" }
                    else -> DynamicIconCatalog.ALL_ICONS.find { it.id == "STABLE_BALANCE" }
                }
            }
            else -> DynamicIconCatalog.ALL_ICONS.find { it.id == "STABLE_BALANCE" }
        } ?: DynamicIconCatalog.ALL_ICONS.first()

        activeIconBadge.value = detectedBadge
    }

    private suspend fun checkAndSeedProducts() {
        // App starts clean with empty product catalog
    }

    // App Mode (PERSONAL vs WORK_EMPLOYER vs WORK_EMPLOYEE)
    private val _appMode = MutableStateFlow(AppMode.PERSONAL)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    // Dual Profile states: filter transactions dynamically
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter by es_empleador based on AppMode
    val filteredByProfileTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        appMode
    ) { txList, mode ->
        val workOnly = (mode == AppMode.WORK_EMPLOYER || mode == AppMode.WORK_EMPLOYEE)
        txList.filter { it.es_empleador == workOnly }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History & Period Filters
    private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        filteredByProfileTransactions,
        _historyFilter
    ) { txList, filter ->
        val now = Calendar.getInstance()
        val todayYear = now.get(Calendar.YEAR)
        val todayDay = now.get(Calendar.DAY_OF_YEAR)

        txList.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.fecha }
            when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.TODAY -> {
                    txCal.get(Calendar.YEAR) == todayYear && txCal.get(Calendar.DAY_OF_YEAR) == todayDay
                }
                HistoryFilter.THIS_WEEK -> {
                    val diffMs = System.currentTimeMillis() - tx.fecha
                    diffMs <= 7 * 24 * 60 * 60 * 1000L
                }
                HistoryFilter.THIS_MONTH -> {
                    txCal.get(Calendar.YEAR) == todayYear && txCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                }
                HistoryFilter.INCOME -> tx.tipo == "ingreso"
                HistoryFilter.EXPENSE -> tx.tipo == "gasto"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-Time Balance & Statistics Computations
    val totalIncome: StateFlow<Double> = filteredByProfileTransactions.map { list ->
        list.filter { it.tipo == "ingreso" }.sumOf { it.monto }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = filteredByProfileTransactions.map { list ->
        list.filter { it.tipo == "gasto" }.sumOf { it.monto }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val availableBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Multi-Currency Breakdown (Desglose CUP, MLC y USD/SQP)
    val balanceCUP: StateFlow<Double> = filteredByProfileTransactions.map { list ->
        val inc = list.filter { it.tipo == "ingreso" && (it.moneda == "CUP" || it.moneda.isEmpty()) }.sumOf { it.monto }
        val exp = list.filter { it.tipo == "gasto" && (it.moneda == "CUP" || it.moneda.isEmpty()) }.sumOf { it.monto }
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balanceMLC: StateFlow<Double> = filteredByProfileTransactions.map { list ->
        val inc = list.filter { it.tipo == "ingreso" && it.moneda == "MLC" }.sumOf { it.monto }
        val exp = list.filter { it.tipo == "gasto" && it.moneda == "MLC" }.sumOf { it.monto }
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balanceUSD: StateFlow<Double> = filteredByProfileTransactions.map { list ->
        val inc = list.filter { it.tipo == "ingreso" && (it.moneda == "USD" || it.moneda == "SQP") }.sumOf { it.monto }
        val exp = list.filter { it.tipo == "gasto" && (it.moneda == "USD" || it.moneda == "SQP") }.sumOf { it.monto }
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Last 7 days trend percentage
    val trendPercentage: StateFlow<String> = filteredByProfileTransactions.map { list ->
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        val last7Days = list.filter { now - it.fecha <= 7 * oneDay }
        val previous7Days = list.filter { (now - it.fecha > 7 * oneDay) && (now - it.fecha <= 14 * oneDay) }

        val last7Sum = last7Days.filter { it.tipo == "ingreso" }.sumOf { it.monto } - last7Days.filter { it.tipo == "gasto" }.sumOf { it.monto }
        val prev7Sum = previous7Days.filter { it.tipo == "ingreso" }.sumOf { it.monto } - previous7Days.filter { it.tipo == "gasto" }.sumOf { it.monto }

        if (last7Sum == 0.0 && prev7Sum == 0.0) {
            "0.0%"
        } else if (prev7Sum <= 0.0) {
            if (last7Sum >= 0.0) "+100.0%" else "-100.0%"
        } else {
            val diff = ((last7Sum - prev7Sum) / prev7Sum) * 100.0
            val sign = if (diff >= 0) "+" else ""
            String.format(Locale.US, "%s%.1f%%", sign, diff)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0.0%")

    // Category Distribution (Donut Chart) -> Map of Category to Percentage
    val categoryDistribution: StateFlow<Map<String, Double>> = filteredByProfileTransactions.map { list ->
        val expenses = list.filter { it.tipo == "gasto" }
        val totalExp = expenses.sumOf { it.monto }
        if (totalExp == 0.0) emptyMap()
        else {
            expenses.groupBy { it.categoria }
                .mapValues { (_, txs) -> (txs.sumOf { it.monto } / totalExp) * 100.0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Weekly Daily Income/Expense Bars (e.g., list of pairs of Income vs Expense for each of last 7 days)
    val weeklyDailyBars: StateFlow<List<Pair<Double, Double>>> = filteredByProfileTransactions.map { list ->
        val now = Calendar.getInstance()
        val result = mutableListOf<Pair<Double, Double>>()
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayTxs = list.filter {
                val txCal = Calendar.getInstance().apply { timeInMillis = it.fecha }
                txCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                        txCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
            }
            val inc = dayTxs.filter { it.tipo == "ingreso" }.sumOf { it.monto }
            val exp = dayTxs.filter { it.tipo == "gasto" }.sumOf { it.monto }
            result.add(Pair(inc, exp))
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { Pair(0.0, 0.0) })

    // Real Daily Bar Data for History Screen
    val weeklyBarData: StateFlow<List<com.example.ui.components.DailyBarData>> = filteredByProfileTransactions.map { list ->
        val result = mutableListOf<com.example.ui.components.DailyBarData>()
        val dayFormat = SimpleDateFormat("EEE", Locale("es", "ES"))
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayTxs = list.filter {
                val txCal = Calendar.getInstance().apply { timeInMillis = it.fecha }
                txCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                        txCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
            }
            val inc = dayTxs.filter { it.tipo == "ingreso" }.sumOf { it.monto }
            val exp = dayTxs.filter { it.tipo == "gasto" }.sumOf { it.monto }
            val label = dayFormat.format(dayCal.time).replace(".", "").replaceFirstChar { it.uppercase() }
            result.add(com.example.ui.components.DailyBarData(label, inc, exp))
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User Profile persistence simulated over Configurations
    private val _userProfile = MutableStateFlow(UserProfileState())
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()

    // Employer profile info for Employee view
    private val _employerInfo = MutableStateFlow(EmployerInfo())
    val employerInfo: StateFlow<EmployerInfo> = _employerInfo.asStateFlow()

    // Screen states
    val selectedTransactionForDetail = MutableStateFlow<Transaction?>(null)
    val showAddTransactionDialog = MutableStateFlow(false)
    val showEditProfileDialog = MutableStateFlow(false)
    val showHelpDialog = MutableStateFlow(false)
    val showSettingsDialog = MutableStateFlow(false)
    val showLogoutDialog = MutableStateFlow(false)
    val showGlobalSearchDialog = MutableStateFlow(false)

    fun setAppMode(mode: AppMode) {
        _appMode.value = mode
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    // CRUD Transactions
    fun addTransaction(
        monto: Double,
        categoria: String,
        descripcion: String,
        fecha: Long,
        hora: String,
        metodoPago: String,
        esEmpleador: Boolean,
        tipo: String,
        moneda: String = "CUP"
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                tipo = tipo,
                monto = monto,
                moneda = moneda,
                categoria = categoria,
                descripcion = descripcion,
                fecha = fecha,
                hora = hora,
                metodo_pago = metodoPago,
                es_empleador = esEmpleador
            )
            repository.insertTransaction(tx)
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            selectedTransactionForDetail.value = null
        }
    }

    // Mode visibility flags (Ajustes de visibilidad de modos Empleador, Empleado y Distribuidor)
    val isEmployerModeEnabled = MutableStateFlow(false)
    val isEmployeeModeEnabled = MutableStateFlow(false)
    val isDistributorModeEnabled = MutableStateFlow(false)

    // QvaPay Integration State
    val isQvaPayEnabled = MutableStateFlow(false)
    val qvaPayAppKey = MutableStateFlow("")
    val qvaPayAppSecret = MutableStateFlow("")
    val qvaPayUserInfo = MutableStateFlow<QvaPayUserInfo?>(null)
    val qvaPayCoins = MutableStateFlow<List<QvaPayCoin>>(emptyList())
    val qvaPayTransactions = MutableStateFlow<List<QvaPayTransaction>>(emptyList())
    val qvaPayInvoices = MutableStateFlow<List<QvaPayInvoice>>(emptyList())
    val isQvaPayLoading = MutableStateFlow(false)
    val qvaPayError = MutableStateFlow<String?>(null)
    val qvaPaySuccessMessage = MutableStateFlow<String?>(null)

    // QvaPay Cache State (Guardado local para ver sin conexión)
    val isQvaPayOfflineCache = MutableStateFlow(false)
    val qvaPayCacheTimestamp = MutableStateFlow("")

    // Sistema de Autenticación de Seguridad (PIN / Biometría por defecto activada)
    val isSecurityAuthEnabled = MutableStateFlow(true)
    val userSecurityPin = MutableStateFlow("1234")
    val isAuthDialogVisible = MutableStateFlow(false)
    val authDialogTitle = MutableStateFlow("Autenticación Requerida")
    val authDialogReason = MutableStateFlow("Confirma tu identidad para continuar.")
    private var pendingAuthSuccessCallback: (() -> Unit)? = null

    fun requestSecurityAuth(title: String, reason: String, onApproved: () -> Unit) {
        if (!isSecurityAuthEnabled.value) {
            onApproved()
            return
        }
        authDialogTitle.value = title
        authDialogReason.value = reason
        pendingAuthSuccessCallback = onApproved
        isAuthDialogVisible.value = true
    }

    fun onSecurityAuthSuccess() {
        isAuthDialogVisible.value = false
        pendingAuthSuccessCallback?.invoke()
        pendingAuthSuccessCallback = null
    }

    fun onSecurityAuthCancel() {
        isAuthDialogVisible.value = false
        pendingAuthSuccessCallback = null
    }

    fun saveSecurityAuthConfig(enabled: Boolean, newPin: String = userSecurityPin.value) {
        requestSecurityAuth(
            title = "Ajustes de Seguridad",
            reason = "Confirma tu identidad para modificar la autenticación de acciones sensibles."
        ) {
            viewModelScope.launch {
                isSecurityAuthEnabled.value = enabled
                userSecurityPin.value = newPin
                repository.insertConfiguracion(Configuracion(clave = "security_auth_enabled", valor = enabled.toString()))
                repository.insertConfiguracion(Configuracion(clave = "user_security_pin", valor = newPin))
            }
        }
    }

    // Ramas de la Empresa (Carga desde 0 para el usuario)
    private val _companyBranches = MutableStateFlow<List<BranchInfo>>(
        listOf(
            BranchInfo(1, "Sede Central (Principal)", "Sede Central", isMain = true, managerName = "")
        )
    )
    val companyBranches: StateFlow<List<BranchInfo>> = _companyBranches.asStateFlow()

    fun addCompanyBranch(name: String, address: String, managerName: String) {
        requestSecurityAuth(
            title = "Agregar Nueva Rama / Sucursal",
            reason = "Autentícate con PIN o Huella para vincular la rama '$name' a la principal."
        ) {
            val nextId = (_companyBranches.value.maxOfOrNull { it.id } ?: 0) + 1
            val newBranch = BranchInfo(
                id = nextId,
                name = name,
                address = address,
                isMain = false,
                managerName = managerName
            )
            _companyBranches.value = _companyBranches.value + newBranch
        }
    }

    // Onboarding & Permission Explanations & Dynamic Icons
    val hasSeenOnboarding = MutableStateFlow(false)
    val showOnboardingDialog = MutableStateFlow(false)

    val hasAcceptedPermissionsDisclosure = MutableStateFlow(false)
    val showPermissionsDisclosureDialog = MutableStateFlow(false)

    val activeIconBadge = MutableStateFlow<DynamicIconBadge>(DynamicIconCatalog.ALL_ICONS.first())
    val showIconGalleryDialog = MutableStateFlow(false)

    fun completeOnboarding() {
        hasSeenOnboarding.value = true
        showOnboardingDialog.value = false
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "has_seen_onboarding", valor = "true"))
        }
        // Show permissions explanation right after onboarding if not accepted
        if (!hasAcceptedPermissionsDisclosure.value) {
            showPermissionsDisclosureDialog.value = true
        }
    }

    fun acceptPermissionsDisclosure() {
        hasAcceptedPermissionsDisclosure.value = true
        showPermissionsDisclosureDialog.value = false
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "accepted_permissions_disclosure", valor = "true"))
        }
    }

    fun setActiveIconBadge(badge: DynamicIconBadge) {
        activeIconBadge.value = badge
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "active_icon_badge_id", valor = badge.id))
        }
    }

    // Monitor Oficial de Saldo Línea Móvil ETECSA (*222#)
    val etecsaMobileBalance = MutableStateFlow(EtecsaMobileBalance())
    val showEtecsaUssdDialog = MutableStateFlow(false)

    // --- 10 NEW FEATURES STATE FLOWS & MANAGERS ---

    // Feature 1: USSD Log History Tracker
    val ussdLogHistory = MutableStateFlow<List<com.example.data.model.UssdLogItem>>(emptyList())

    // Feature 2: Informal Currency Exchange Calculator (CUP <-> USD / MLC / SQP)
    val customExchangeRateUSD = MutableStateFlow(320.0) // 1 USD = 320 CUP
    val customExchangeRateMLC = MutableStateFlow(270.0) // 1 MLC = 270 CUP

    fun updateCustomExchangeRates(usdRate: Double, mlcRate: Double) {
        customExchangeRateUSD.value = usdRate
        customExchangeRateMLC.value = mlcRate
        viewModelScope.launch {
            repository.insertConfiguracion(com.example.data.model.Configuracion(clave = "custom_exchange_rate_usd", valor = usdRate.toString()))
            repository.insertConfiguracion(com.example.data.model.Configuracion(clave = "custom_exchange_rate_mlc", valor = mlcRate.toString()))
        }
    }

    // Feature 3: Digital Receipt Data Generator
    fun generateTransactionReceipt(tx: Transaction): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return """
            =================================
            🧾 RECIBO DIGITAL COMPROBANTE DE PAGO
            =================================
            ID Transacción: TX-${tx.id}
            Fecha: ${sdf.format(Date(tx.fecha))}
            Categoría: ${tx.categoria}
            Monto: ${tx.monto} ${tx.moneda}
            Método de Pago: ${tx.metodo_pago}
            Tipo: ${tx.tipo.uppercase(Locale.getDefault())}
            Detalles: ${tx.descripcion}
            Estado: CONFIRMADO ✅
            =================================
            Generado por AURA Financial System Cuba
        """.trimIndent()
    }

    // Feature 4: Financial Goals & Savings Targets
    val financialGoals = MutableStateFlow<List<com.example.data.model.FinancialGoal>>(
        listOf(
            com.example.data.model.FinancialGoal(1, "Comprar Teléfono", 150000.0, 45000.0, "Tecnología", "#7C3AED"),
            com.example.data.model.FinancialGoal(2, "Fondo Emergencia", 50000.0, 30000.0, "Ahorro", "#10B981")
        )
    )

    fun addFinancialGoal(name: String, target: Double, category: String) {
        val goal = com.example.data.model.FinancialGoal(
            id = System.currentTimeMillis(),
            name = name,
            targetAmount = target,
            currentAmount = 0.0,
            category = category
        )
        financialGoals.value = financialGoals.value + goal
    }

    fun addFundsToGoal(goalId: Long, amount: Double) {
        financialGoals.value = financialGoals.value.map { g ->
            if (g.id == goalId) g.copy(currentAmount = (g.currentAmount + amount).coerceAtMost(g.targetAmount)) else g
        }
    }

    // Feature 5: Category Spending Limits & Budget Warnings
    val categoryBudgets = MutableStateFlow<List<com.example.data.model.CategoryBudget>>(
        listOf(
            com.example.data.model.CategoryBudget("Comida", 20000.0),
            com.example.data.model.CategoryBudget("Servicios", 8000.0),
            com.example.data.model.CategoryBudget("Entretenimiento", 5000.0)
        )
    )

    fun setCategoryBudget(categoryName: String, limitAmount: Double) {
        val existing = categoryBudgets.value.find { it.categoryName == categoryName }
        if (existing != null) {
            categoryBudgets.value = categoryBudgets.value.map {
                if (it.categoryName == categoryName) it.copy(limitAmount = limitAmount) else it
            }
        } else {
            categoryBudgets.value = categoryBudgets.value + com.example.data.model.CategoryBudget(categoryName, limitAmount)
        }
    }

    // Feature 6: Transfermóvil Quick Favorite Contacts
    val favoriteTransferContacts = MutableStateFlow<List<Contacto>>(emptyList())

    fun toggleFavoriteContact(contact: Contacto) {
        if (favoriteTransferContacts.value.any { it.id == contact.id }) {
            favoriteTransferContacts.value = favoriteTransferContacts.value.filter { it.id != contact.id }
        } else {
            favoriteTransferContacts.value = favoriteTransferContacts.value + contact
        }
    }

    // Feature 7: Advanced Search Query with Tag Filter
    val searchQuery = MutableStateFlow("")
    val searchMinAmount = MutableStateFlow("")
    val searchMaxAmount = MutableStateFlow("")

    val advancedFilteredTransactions: StateFlow<List<Transaction>> = combine(
        filteredTransactions,
        searchQuery,
        searchMinAmount,
        searchMaxAmount
    ) { list, q, minStr, maxStr ->
        val min = minStr.toDoubleOrNull() ?: 0.0
        val max = maxStr.toDoubleOrNull() ?: Double.MAX_VALUE

        list.filter { tx ->
            val matchesQ = q.isBlank() || tx.descripcion.contains(q, ignoreCase = true) || tx.categoria.contains(q, ignoreCase = true) || tx.metodo_pago.contains(q, ignoreCase = true)
            val matchesAmt = tx.monto in min..max
            matchesQ && matchesAmt
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feature 8: Recurring Expenses & Reminders
    val recurringExpenses = MutableStateFlow<List<com.example.data.model.RecurringExpense>>(
        listOf(
            com.example.data.model.RecurringExpense(1, "Pago Nauta Hogar", 1250.0, "CUP", "Servicios", 30, System.currentTimeMillis() + 864000000L),
            com.example.data.model.RecurringExpense(2, "Factura Electricidad", 850.0, "CUP", "Servicios", 30, System.currentTimeMillis() + 432000000L)
        )
    )

    fun addRecurringExpense(title: String, amount: Double, category: String, days: Int) {
        val rec = com.example.data.model.RecurringExpense(
            title = title,
            amount = amount,
            category = category,
            intervalDays = days,
            nextDueDate = System.currentTimeMillis() + (days * 86400000L)
        )
        recurringExpenses.value = recurringExpenses.value + rec
    }

    // Feature 9: Debt & Loan Ledger (Cuentas por Cobrar / Pagar)
    val debtLoanItems = MutableStateFlow<List<com.example.data.model.DebtLoanItem>>(
        listOf(
            com.example.data.model.DebtLoanItem(1, "Alejandro", 2500.0, "CUP", isOwedToMe = true, "Préstamo almuerzo", "15/12/2026"),
            com.example.data.model.DebtLoanItem(2, "Bodega Barrio", 1200.0, "CUP", isOwedToMe = false, "Insumos fiados", "30/11/2026")
        )
    )

    fun addDebtLoanItem(personName: String, amount: Double, isOwedToMe: Boolean, description: String, dueDateStr: String) {
        val item = com.example.data.model.DebtLoanItem(
            personName = personName,
            amount = amount,
            isOwedToMe = isOwedToMe,
            description = description,
            dueDateStr = dueDateStr
        )
        debtLoanItems.value = debtLoanItems.value + item
    }

    fun settleDebtLoanItem(item: com.example.data.model.DebtLoanItem) {
        debtLoanItems.value = debtLoanItems.value.map {
            if (it.id == item.id) it.copy(isSettled = true) else it
        }
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        addTransaction(
            monto = item.amount,
            categoria = if (item.isOwedToMe) "Cobros" else "Pagos Deuda",
            descripcion = "Liquidación Deuda con ${item.personName}: ${item.description}",
            fecha = cal.timeInMillis,
            hora = format.format(cal.time),
            metodoPago = "Efectivo",
            esEmpleador = false,
            tipo = if (item.isOwedToMe) "ingreso" else "gasto"
        )
    }

    // Feature 10: Dashboard Quick Action Shortcuts Customizer
    val dashboardShortcuts = MutableStateFlow<List<com.example.data.model.DashboardShortcut>>(
        listOf(
            com.example.data.model.DashboardShortcut("sc_ussd", "Consultar *222#", "Refresh", "action_ussd"),
            com.example.data.model.DashboardShortcut("sc_transfer", "Transferir SQP", "Send", "action_qvapay"),
            com.example.data.model.DashboardShortcut("sc_calc", "Cambio Divisas", "SwapHoriz", "action_calc"),
            com.example.data.model.DashboardShortcut("sc_debts", "Cuentas Cobrar/Pagar", "Receipt", "action_debts")
        )
    )

    fun toggleDashboardShortcut(shortcutId: String) {
        dashboardShortcuts.value = dashboardShortcuts.value.map {
            if (it.id == shortcutId) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun updateEtecsaMobileBalance(
        saldoCup: Double,
        datosMb: Double,
        datosLteMb: Double,
        minutosVoz: Int,
        mensajesSms: Int,
        fechaVencimiento: String
    ) {
        val logItem = com.example.data.model.UssdLogItem(
            rawText = "Saldo: $saldoCup CUP, Datos: $datosMb MB, Min: $minutosVoz, SMS: $mensajesSms",
            parsedSaldo = saldoCup,
            parsedDatosMb = datosMb,
            parsedMinutos = minutosVoz,
            parsedSms = mensajesSms
        )
        ussdLogHistory.value = listOf(logItem) + ussdLogHistory.value
        val updated = EtecsaMobileBalance(
            saldoCup = saldoCup,
            fechaVencimiento = fechaVencimiento,
            datosMb = datosMb,
            datosLteMb = datosLteMb,
            minutosVoz = minutosVoz,
            mensajesSms = mensajesSms,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        etecsaMobileBalance.value = updated
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "etecsa_saldo_cup", valor = saldoCup.toString()))
            repository.insertConfiguracion(Configuracion(clave = "etecsa_datos_mb", valor = datosMb.toString()))
            repository.insertConfiguracion(Configuracion(clave = "etecsa_datos_lte_mb", valor = datosLteMb.toString()))
            repository.insertConfiguracion(Configuracion(clave = "etecsa_minutos", valor = minutosVoz.toString()))
            repository.insertConfiguracion(Configuracion(clave = "etecsa_sms", valor = mensajesSms.toString()))
            repository.insertConfiguracion(Configuracion(clave = "etecsa_vencimiento", valor = fechaVencimiento))
        }
    }

    fun parseAndProcessUssdText(input: String) {
        var saldo = etecsaMobileBalance.value.saldoCup
        var datos = etecsaMobileBalance.value.datosMb
        var lte = etecsaMobileBalance.value.datosLteMb
        var min = etecsaMobileBalance.value.minutosVoz
        var sms = etecsaMobileBalance.value.mensajesSms
        var vencimiento = etecsaMobileBalance.value.fechaVencimiento

        val saldoRegex = Regex("(?i)(?:saldo(?: principal)?(?: es)?:?\\s*)(\\d+(?:[\\.,]\\d+)?)\\s*(?:CUP|$)")
        val fallbackCupRegex = Regex("(\\d+(?:[\\.,]\\d+)?)\\s*CUP")
        val datosGbRegex = Regex("(?i)(\\d+(?:[\\.,]\\d+)?)\\s*GB")
        val datosMbRegex = Regex("(?i)(\\d+(?:[\\.,]\\d+)?)\\s*MB")
        val lteGbRegex = Regex("(?i)(\\d+(?:[\\.,]\\d+)?)\\s*GB\\s*(?:LTE|4G)")
        val lteMbRegex = Regex("(?i)(\\d+(?:[\\.,]\\d+)?)\\s*MB\\s*(?:LTE|4G)")
        val minRegex = Regex("(?i)(\\d+)\\s*(?:Min|Minutos|Voz)")
        val smsRegex = Regex("(?i)(\\d+)\\s*(?:SMS|Mensajes)")
        val dateRegex = Regex("(\\d{2}[/\\-]\\d{2}[/\\-]\\d{2,4})")

        saldoRegex.find(input)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()?.let { saldo = it }
            ?: fallbackCupRegex.find(input)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()?.let { saldo = it }

        lteGbRegex.find(input)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()?.let { lte = it * 1024.0 }
            ?: lteMbRegex.find(input)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()?.let { lte = it }

        datosGbRegex.find(input)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()?.let { datos = it * 1024.0 }
            ?: datosMbRegex.find(input)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()?.let { datos = it }

        minRegex.find(input)?.groupValues?.get(1)?.toIntOrNull()?.let { min = it }
        smsRegex.find(input)?.groupValues?.get(1)?.toIntOrNull()?.let { sms = it }
        dateRegex.find(input)?.groupValues?.get(1)?.let { vencimiento = it }

        updateEtecsaMobileBalance(saldo, datos, lte, min, sms, vencimiento)
    }

    // Pending mode reactivation dialog (Empleador, Empleado, Distribuidor)
    val pendingModeReactivation = MutableStateFlow<AppMode?>(null)

    // iOS Extended Peek & Pop Preview state
    val activePeekPreview = MutableStateFlow<PeekPreviewType?>(null)

    // Distributor Dispatches (Carga desde 0)
    private val _despachosDistribuidor = MutableStateFlow<List<DespachoDistribuidor>>(emptyList())
    val despachosDistribuidor: StateFlow<List<DespachoDistribuidor>> = _despachosDistribuidor.asStateFlow()

    // Configuration-backed User profile loading/saving
    private suspend fun loadConfigurations() {
        val name = repository.getConfiguracionByKey("nombre_usuario")?.valor ?: ""
        val phone = repository.getConfiguracionByKey("telefono_usuario")?.valor ?: ""
        val photoUri = repository.getConfiguracionByKey("foto_perfil")?.valor
        val biometrics = repository.getConfiguracionByKey("biometria")?.valor?.toBoolean() ?: true
        val notifications = repository.getConfiguracionByKey("notificaciones")?.valor?.toBoolean() ?: true
        val empActive = repository.getConfiguracionByKey("modo_empleador_activo")?.valor?.toBoolean() ?: false
        val workerActive = repository.getConfiguracionByKey("modo_empleado_activo")?.valor?.toBoolean() ?: false
        val distActive = repository.getConfiguracionByKey("modo_distribuidor_activo")?.valor?.toBoolean() ?: false

        val empName = repository.getConfiguracionByKey("nombre_empleador")?.valor ?: ""
        val empPhone = repository.getConfiguracionByKey("telefono_empleador")?.valor ?: ""
        val empPhoto = repository.getConfiguracionByKey("foto_empleador")?.valor

        val seenOnboarding = repository.getConfiguracionByKey("has_seen_onboarding")?.valor?.toBoolean() ?: false
        val accPerms = repository.getConfiguracionByKey("accepted_permissions_disclosure")?.valor?.toBoolean() ?: false
        val badgeId = repository.getConfiguracionByKey("active_icon_badge_id")?.valor ?: "TREND_UP_1"

        hasSeenOnboarding.value = seenOnboarding
        hasAcceptedPermissionsDisclosure.value = accPerms
        activeIconBadge.value = DynamicIconCatalog.ALL_ICONS.find { it.id == badgeId } ?: DynamicIconCatalog.ALL_ICONS.first()

        if (!seenOnboarding) {
            showOnboardingDialog.value = true
        }

        isEmployerModeEnabled.value = empActive
        isEmployeeModeEnabled.value = workerActive
        isDistributorModeEnabled.value = distActive

        _userProfile.value = UserProfileState(
            name = name,
            phone = phone,
            photoUri = photoUri,
            isBiometricsEnabled = biometrics,
            isNotificationsEnabled = notifications
        )

        _employerInfo.value = EmployerInfo(
            name = empName,
            phone = empPhone,
            photoUri = empPhoto
        )

        // Security Authentication settings loading
        val secAuth = repository.getConfiguracionByKey("security_auth_enabled")?.valor?.toBoolean() ?: true
        val secPin = repository.getConfiguracionByKey("user_security_pin")?.valor ?: "1234"
        isSecurityAuthEnabled.value = secAuth
        userSecurityPin.value = secPin

        // ETECSA mobile balance persistent loading
        val savedSaldo = repository.getConfiguracionByKey("etecsa_saldo_cup")?.valor?.toDoubleOrNull() ?: 0.0
        val savedDatos = repository.getConfiguracionByKey("etecsa_datos_mb")?.valor?.toDoubleOrNull() ?: 0.0
        val savedLte = repository.getConfiguracionByKey("etecsa_datos_lte_mb")?.valor?.toDoubleOrNull() ?: 0.0
        val savedMin = repository.getConfiguracionByKey("etecsa_minutos")?.valor?.toIntOrNull() ?: 0
        val savedSms = repository.getConfiguracionByKey("etecsa_sms")?.valor?.toIntOrNull() ?: 0
        val savedVenc = repository.getConfiguracionByKey("etecsa_vencimiento")?.valor ?: "Sin fecha"

        etecsaMobileBalance.value = EtecsaMobileBalance(
            saldoCup = savedSaldo,
            datosMb = savedDatos,
            datosLteMb = savedLte,
            minutosVoz = savedMin,
            mensajesSms = savedSms,
            fechaVencimiento = savedVenc,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )

        // QvaPay config loading
        val qvapayActive = repository.getConfiguracionByKey("qvapay_activo")?.valor?.toBoolean() ?: false
        val qvapayKey = repository.getConfiguracionByKey("qvapay_app_key")?.valor ?: ""
        val qvapaySecret = repository.getConfiguracionByKey("qvapay_app_secret")?.valor ?: ""

        isQvaPayEnabled.value = qvapayActive
        qvaPayAppKey.value = qvapayKey
        qvaPayAppSecret.value = qvapaySecret

        // QvaPay Cache loading for Offline viewing
        val cachedBalance = repository.getConfiguracionByKey("qvapay_cache_balance")?.valor
        val cachedUsername = repository.getConfiguracionByKey("qvapay_cache_username")?.valor ?: "usuario"
        val cachedName = repository.getConfiguracionByKey("qvapay_cache_name")?.valor ?: "Usuario QvaPay"
        val cachedTimestamp = repository.getConfiguracionByKey("qvapay_cache_timestamp")?.valor ?: ""

        if (!cachedBalance.isNullOrBlank()) {
            qvaPayUserInfo.value = QvaPayUserInfo(
                name = cachedName,
                username = cachedUsername,
                email = "",
                balance = cachedBalance.toDoubleOrNull() ?: 0.0,
                logo = null,
                bio = null
            )
            qvaPayCacheTimestamp.value = cachedTimestamp
            isQvaPayOfflineCache.value = true
        }

        if (qvapayActive && qvapayKey.isNotBlank() && qvapaySecret.isNotBlank()) {
            refreshQvaPayData()
        }
    }

    fun saveQvaPayConfig(enabled: Boolean, appKey: String, appSecret: String) {
        requestSecurityAuth(
            title = if (!enabled) "Desvincular QvaPay" else "Modificar Configuración QvaPay",
            reason = "Confirma tu PIN o Huella para modificar o desvincular la cuenta de QvaPay."
        ) {
            viewModelScope.launch {
                isQvaPayEnabled.value = enabled
                qvaPayAppKey.value = appKey
                qvaPayAppSecret.value = appSecret

                repository.insertConfiguracion(Configuracion(clave = "qvapay_activo", valor = enabled.toString()))
                repository.insertConfiguracion(Configuracion(clave = "qvapay_app_key", valor = appKey))
                repository.insertConfiguracion(Configuracion(clave = "qvapay_app_secret", valor = appSecret))

                if (enabled && appKey.isNotBlank() && appSecret.isNotBlank()) {
                    refreshQvaPayData()
                } else if (!enabled) {
                    qvaPayUserInfo.value = null
                    qvaPayCoins.value = emptyList()
                }
            }
        }
    }

    fun refreshQvaPayData() {
        viewModelScope.launch {
            val key = qvaPayAppKey.value
            val secret = qvaPayAppSecret.value
            if (key.isBlank() || secret.isBlank()) {
                qvaPayError.value = "Configura tu App Key y App Secret de QvaPay en Ajustes."
                return@launch
            }

            isQvaPayLoading.value = true
            qvaPayError.value = null

            val userRes = QvaPayApiService.getUserInfo(key, secret)
            userRes.onSuccess { info ->
                qvaPayUserInfo.value = info
                isQvaPayOfflineCache.value = false
                val timeStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                qvaPayCacheTimestamp.value = timeStr

                // Update local offline cache in SQLite
                repository.insertConfiguracion(Configuracion(clave = "qvapay_cache_balance", valor = info.balance.toString()))
                repository.insertConfiguracion(Configuracion(clave = "qvapay_cache_username", valor = info.username))
                repository.insertConfiguracion(Configuracion(clave = "qvapay_cache_name", valor = info.name))
                repository.insertConfiguracion(Configuracion(clave = "qvapay_cache_timestamp", valor = timeStr))
            }.onFailure { err ->
                qvaPayError.value = err.message
                if (qvaPayUserInfo.value != null) {
                    isQvaPayOfflineCache.value = true
                }
            }

            val coinsRes = QvaPayApiService.getCoins(key, secret)
            coinsRes.onSuccess { list ->
                qvaPayCoins.value = list
            }

            val txsRes = QvaPayApiService.getTransactions(key, secret)
            txsRes.onSuccess { list ->
                qvaPayTransactions.value = list
            }

            isQvaPayLoading.value = false
        }
    }

    fun createQvaPayInvoice(amount: Double, description: String) {
        requestSecurityAuth(
            title = "Generar Factura de Cobro QvaPay",
            reason = "Confirma tu PIN o Huella para generar un enlace de cobro de $amount SQP."
        ) {
            viewModelScope.launch {
                isQvaPayLoading.value = true
                qvaPayError.value = null
                qvaPaySuccessMessage.value = null

                val key = qvaPayAppKey.value
                val secret = qvaPayAppSecret.value

                val res = QvaPayApiService.createInvoice(key, secret, amount, description)
                res.onSuccess { invoice ->
                    qvaPayInvoices.value = listOf(invoice) + qvaPayInvoices.value
                    qvaPaySuccessMessage.value = "¡Enlace de Cobro Creado! URL: ${invoice.url}"
                }.onFailure { err ->
                    qvaPayError.value = err.message ?: "Error al generar factura QvaPay"
                }

                isQvaPayLoading.value = false
            }
        }
    }

    fun executeQvaPayTransfer(toUsername: String, amount: Double, description: String) {
        requestSecurityAuth(
            title = "Confirmar Envío de Fondos",
            reason = "Confirma tu PIN o Huella para enviar $amount SQP a @$toUsername."
        ) {
            viewModelScope.launch {
                val key = qvaPayAppKey.value
                val secret = qvaPayAppSecret.value
                if (key.isBlank() || secret.isBlank()) {
                    qvaPayError.value = "Ingresa tu App Key y App Secret en Configuración."
                    return@launch
                }

                isQvaPayLoading.value = true
                qvaPayError.value = null
                qvaPaySuccessMessage.value = null

                val res = QvaPayApiService.transfer(key, secret, toUsername, amount, description)
                res.onSuccess { resp ->
                    qvaPaySuccessMessage.value = "¡Transferencia de $amount SQP a @$toUsername realizada con éxito! ID: ${resp.transactionId}"
                    val cal = Calendar.getInstance()
                    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                    addTransaction(
                        monto = amount,
                        categoria = "QvaPay",
                        descripcion = "Transferencia QvaPay a @$toUsername (${description.ifBlank { "SQP" }})",
                        fecha = cal.timeInMillis,
                        hora = format.format(cal.time),
                        metodoPago = "QvaPay",
                        esEmpleador = isEmployerModeEnabled.value,
                        tipo = "gasto"
                    )
                    refreshQvaPayData()
                }.onFailure { err ->
                    qvaPayError.value = err.message ?: "Error en la transferencia QvaPay"
                }

                isQvaPayLoading.value = false
            }
        }
    }


    fun openPeekPreview(preview: PeekPreviewType) {
        activePeekPreview.value = preview
    }

    fun closePeekPreview() {
        activePeekPreview.value = null
    }

    fun requestToggleEmployerMode(enabled: Boolean) {
        if (enabled) {
            pendingModeReactivation.value = AppMode.WORK_EMPLOYER
        } else {
            toggleEmployerModeActive(false, resetData = false)
        }
    }

    fun requestToggleEmployeeMode(enabled: Boolean) {
        if (enabled) {
            pendingModeReactivation.value = AppMode.WORK_EMPLOYEE
        } else {
            toggleEmployeeModeActive(false, resetData = false)
        }
    }

    fun requestToggleDistributorMode(enabled: Boolean) {
        if (enabled) {
            pendingModeReactivation.value = AppMode.WORK_DISTRIBUTOR
        } else {
            toggleDistributorModeActive(false, resetData = false)
        }
    }

    fun confirmModeReactivation(resetData: Boolean) {
        val mode = pendingModeReactivation.value ?: return
        when (mode) {
            AppMode.WORK_EMPLOYER -> toggleEmployerModeActive(true, resetData)
            AppMode.WORK_EMPLOYEE -> toggleEmployeeModeActive(true, resetData)
            AppMode.WORK_DISTRIBUTOR -> toggleDistributorModeActive(true, resetData)
            AppMode.PERSONAL -> {}
        }
        pendingModeReactivation.value = null
    }

    fun toggleEmployerModeActive(enabled: Boolean, resetData: Boolean = false) {
        isEmployerModeEnabled.value = enabled
        if (!enabled && _appMode.value == AppMode.WORK_EMPLOYER) {
            _appMode.value = AppMode.PERSONAL
        }
        if (enabled && resetData) {
            // Reiniciar productos para empezar con datos nuevos
            viewModelScope.launch {
                repository.deleteAllProductos()
            }
        }
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "modo_empleador_activo", valor = enabled.toString()))
        }
    }

    fun toggleEmployeeModeActive(enabled: Boolean, resetData: Boolean = false) {
        isEmployeeModeEnabled.value = enabled
        if (!enabled && _appMode.value == AppMode.WORK_EMPLOYEE) {
            _appMode.value = AppMode.PERSONAL
        }
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "modo_empleado_activo", valor = enabled.toString()))
        }
    }

    fun toggleDistributorModeActive(enabled: Boolean, resetData: Boolean = false) {
        isDistributorModeEnabled.value = enabled
        if (!enabled && _appMode.value == AppMode.WORK_DISTRIBUTOR) {
            _appMode.value = AppMode.PERSONAL
        }
        if (enabled && resetData) {
            _despachosDistribuidor.value = emptyList()
        }
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "modo_distribuidor_activo", valor = enabled.toString()))
        }
    }

    fun addDespachoDistribuidor(destinatario: String, productoNombre: String, cantidad: Int, precioUnitario: Double) {
        val newDespacho = DespachoDistribuidor(
            id = System.currentTimeMillis(),
            destinatario = destinatario,
            productoNombre = productoNombre,
            cantidadUnidades = cantidad,
            precioPorUnidad = precioUnitario,
            estado = "Entregado",
            timestamp = System.currentTimeMillis()
        )
        _despachosDistribuidor.value = listOf(newDespacho) + _despachosDistribuidor.value
    }

    fun updateProfilePhoto(photoUri: String) {
        _userProfile.value = _userProfile.value.copy(photoUri = photoUri)
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "foto_perfil", valor = photoUri))
        }
    }

    fun updateUserProfile(newName: String, newPhone: String) {
        _userProfile.value = _userProfile.value.copy(
            name = newName.ifBlank { _userProfile.value.name },
            phone = newPhone.ifBlank { _userProfile.value.phone }
        )
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "nombre_usuario", valor = _userProfile.value.name))
            repository.insertConfiguracion(Configuracion(clave = "telefono_usuario", valor = _userProfile.value.phone))
        }
        showEditProfileDialog.value = false
    }

    fun toggleBiometrics(enabled: Boolean) {
        _userProfile.value = _userProfile.value.copy(isBiometricsEnabled = enabled)
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "biometria", valor = enabled.toString()))
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _userProfile.value = _userProfile.value.copy(isNotificationsEnabled = enabled)
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "notificaciones", valor = enabled.toString()))
        }
    }

    // ==========================================
    // CATALOG & PRODUCT MANAGEMENT & STAGING BRANCH
    // ==========================================
    val products: StateFlow<List<Producto>> = repository.allProductos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditorias: StateFlow<List<AuditoriaStock>> = repository.allAuditorias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val propuestasPendientes: StateFlow<List<PropuestaCambio>> = repository.propuestasPendientes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showAddProductDialog = MutableStateFlow(false)
    val selectedProductForEdit = MutableStateFlow<Producto?>(null)
    val selectedProductForStockAdjustment = MutableStateFlow<Producto?>(null)
    val selectedTransactionForReconciliation = MutableStateFlow<Transaction?>(null)

    val selectedPropuestaForReview = MutableStateFlow<PropuestaCambio?>(null)
    val showSyncP2PDialog = MutableStateFlow(false)
    val showProposeChangeDialog = MutableStateFlow(false)
    val syncP2PMessage = MutableStateFlow<String?>(null)

    fun submitEmployeeProposal(
        productoId: Long?,
        nombreProd: String,
        precio: Double,
        stock: Int,
        justificacion: String
    ) {
        viewModelScope.launch {
            val empName = userProfile.value.name.ifBlank { "Empleado" }
            val prop = PropuestaCambio(
                empleado_nombre = empName,
                producto_id = productoId,
                nombre_producto = nombreProd,
                precio_propuesto = precio,
                stock_propuesto = stock,
                justificacion = justificacion.ifBlank { "Propuesta de actualización de inventario" },
                estado = "pendiente",
                timestamp = System.currentTimeMillis()
            )
            repository.insertPropuesta(prop)
            showProposeChangeDialog.value = false
        }
    }

    fun approveAndMergeProposal(
        propuesta: PropuestaCambio,
        nombreAjustado: String,
        precioAjustado: Double,
        stockAjustado: Int,
        notasEmpleador: String
    ) {
        viewModelScope.launch {
            val existingProduct = if (propuesta.producto_id != null) {
                products.value.find { it.id == propuesta.producto_id }
            } else {
                products.value.find { it.nombre.equals(nombreAjustado, ignoreCase = true) }
            }

            val oldStock = existingProduct?.stock ?: 0
            val stockDiff = stockAjustado - oldStock

            val updatedProd = if (existingProduct != null) {
                existingProduct.copy(nombre = nombreAjustado, precio = precioAjustado, stock = stockAjustado)
            } else {
                Producto(nombre = nombreAjustado, precio = precioAjustado, stock = stockAjustado)
            }

            val prodId = if (existingProduct != null) {
                repository.updateProducto(updatedProd)
                existingProduct.id
            } else {
                repository.insertProducto(updatedProd)
            }

            val dadoEmpleadoStr = "Propuesto por Empleado (${propuesta.empleado_nombre}): $nombreAjustado | Stock: ${propuesta.stock_propuesto} u. | Precio: $${propuesta.precio_propuesto} | \"${propuesta.justificacion}\""
            val cambiadoEmpleadorStr = if (notasEmpleador.isNotBlank() || precioAjustado != propuesta.precio_propuesto || stockAjustado != propuesta.stock_propuesto) {
                "Revisado y Modificado por Empleador antes de Fusionar: Stock $stockAjustado u., Precio $${precioAjustado}. Notas: \"${notasEmpleador.ifBlank { "Ajuste previo a la fusión" }}\""
            } else {
                "Aprobado y Fusionado sin modificaciones directas"
            }

            val audit = AuditoriaStock(
                producto_id = prodId,
                nombre_producto = nombreAjustado,
                cambio_stock = stockDiff,
                stock_anterior = oldStock,
                stock_resultante = stockAjustado,
                justificacion = "Fusión de Rama Secundarias (Propuesta de ${propuesta.empleado_nombre})",
                realizado_por = "Empleador (Fusión de Rama)",
                dado_por_empleado = dadoEmpleadoStr,
                cambiado_por_empleador = cambiadoEmpleadorStr,
                es_fusion_rama = true,
                timestamp = System.currentTimeMillis()
            )
            repository.insertAuditoria(audit)

            repository.updatePropuesta(propuesta.copy(estado = "aprobado"))
            selectedPropuestaForReview.value = null
        }
    }

    fun rejectProposal(propuesta: PropuestaCambio, motivo: String) {
        viewModelScope.launch {
            repository.updatePropuesta(propuesta.copy(estado = "rechazado"))
            selectedPropuestaForReview.value = null
        }
    }

    fun simulateP2PSync() {
        viewModelScope.launch {
            syncP2PMessage.value = "Conectando por Wi-Fi Local / Bluetooth con dispositivos vinculados..."
            kotlinx.coroutines.delay(1000)

            // Sincronización de fotos de perfil (Empleador y Empleado)
            val currentEmpPhoto = repository.getConfiguracionByKey("foto_empleador")?.valor
            val myProfilePhoto = _userProfile.value.photoUri
            val myName = _userProfile.value.name.ifBlank { "Carlos (Empleador)" }

            if (myProfilePhoto != null && myProfilePhoto != currentEmpPhoto) {
                repository.insertConfiguracion(Configuracion(clave = "foto_empleador", valor = myProfilePhoto))
                repository.insertConfiguracion(Configuracion(clave = "nombre_empleador", valor = myName))
                _employerInfo.value = _employerInfo.value.copy(name = myName, photoUri = myProfilePhoto)
            }

            // Sincronizar fotos de empleados solo si han cambiado
            val employees = activeEmployees.value
            employees.forEach { emp ->
                if (emp.foto_uri == null && myProfilePhoto != null) {
                    // Solo actualiza si es diferente
                    repository.updateEmpleado(emp.copy(foto_uri = myProfilePhoto))
                }
            }

            syncP2PMessage.value = "✅ Sincronización P2P Exitosa: Fotos de perfil (Empleador/Empleado) e Inventario sincronizados sin pérdida de calidad."
            kotlinx.coroutines.delay(2200)
            syncP2PMessage.value = null
        }
    }

    fun adjustStock(product: Producto, newStock: Int, justification: String) {
        viewModelScope.launch {
            val oldStock = product.stock
            val diff = newStock - oldStock
            if (diff == 0) {
                selectedProductForStockAdjustment.value = null
                return@launch
            }

            // Update product stock
            repository.updateProducto(product.copy(stock = newStock))

            // Determine role
            val role = when (appMode.value) {
                AppMode.WORK_EMPLOYER -> "Empleador"
                AppMode.WORK_EMPLOYEE -> "Empleado"
                else -> "Usuario"
            }

            // Record audit log
            val reason = justification.ifBlank { "Ajuste manual de inventario" }
            val audit = AuditoriaStock(
                producto_id = product.id,
                nombre_producto = product.nombre,
                cambio_stock = diff,
                stock_anterior = oldStock,
                stock_resultante = newStock,
                justificacion = reason,
                realizado_por = role,
                timestamp = System.currentTimeMillis()
            )
            repository.insertAuditoria(audit)

            selectedProductForStockAdjustment.value = null
        }
    }

    fun reconcileTransactionWithProducts(
        transaction: Transaction,
        selectedProducts: Map<Producto, Int>,
        isOtherReason: Boolean,
        customReason: String = ""
    ) {
        viewModelScope.launch {
            val updatedDesc = if (isOtherReason) {
                val reason = customReason.ifBlank { "Transferencia no comercial" }
                "${transaction.descripcion.substringBefore(" [Conciliado:")} [Conciliado: $reason]"
            } else if (selectedProducts.isNotEmpty()) {
                val totalProds = selectedProducts.entries.sumOf { (p, qty) -> p.precio * qty }
                val diff = transaction.monto - totalProds
                val statusStr = when {
                    Math.abs(diff) < 0.01 -> "✅ Monto exacto coincide"
                    diff > 0.01 -> "ℹ️ Pago Excedente / Propina (+$diff CUP)"
                    else -> "⚠️ Monto Incompleto (-${Math.abs(diff)} CUP)"
                }
                val itemsSummary = selectedProducts.entries.joinToString(", ") { (p, qty) -> "${qty}x ${p.nombre} ($${p.precio * qty})" }
                "${transaction.descripcion.substringBefore(" [Conciliado:")} [Conciliado: $itemsSummary | $statusStr]"
            } else {
                transaction.descripcion
            }

            // Update transaction description in DB
            val updatedTx = transaction.copy(descripcion = updatedDesc)
            repository.updateTransaction(updatedTx)

            // Deduct stock for sold items & log audit
            if (!isOtherReason && selectedProducts.isNotEmpty()) {
                val role = when (appMode.value) {
                    AppMode.WORK_EMPLOYER -> "Empleador"
                    AppMode.WORK_EMPLOYEE -> "Empleado"
                    else -> "Usuario"
                }
                selectedProducts.forEach { (prod, qty) ->
                    val newStock = (prod.stock - qty).coerceAtLeast(0)
                    repository.updateProducto(prod.copy(stock = newStock))
                    repository.insertAuditoria(
                        AuditoriaStock(
                            producto_id = prod.id,
                            nombre_producto = prod.nombre,
                            cambio_stock = -qty,
                            stock_anterior = prod.stock,
                            stock_resultante = newStock,
                            justificacion = "Venta conciliada con transferencia (${transaction.monto} CUP)",
                            realizado_por = role,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            selectedTransactionForReconciliation.value = null
        }
    }

    fun saveProduct(nombre: String, precio: Double, stock: Int, imagenUri: String? = null) {
        viewModelScope.launch {
            val editing = selectedProductForEdit.value
            if (editing != null) {
                repository.updateProducto(editing.copy(nombre = nombre, precio = precio, stock = stock, imagen_uri = imagenUri))
            } else {
                repository.insertProducto(Producto(nombre = nombre, precio = precio, stock = stock, imagen_uri = imagenUri))
            }
            showAddProductDialog.value = false
            selectedProductForEdit.value = null
        }
    }

    fun deleteProduct(producto: Producto) {
        viewModelScope.launch {
            repository.deleteProducto(producto)
        }
    }

    // Sales Cart (Carrito de venta) for Employees in Work Mode
    private val _cart = MutableStateFlow<Map<Producto, Int>>(emptyMap())
    val cart: StateFlow<Map<Producto, Int>> = _cart.asStateFlow()

    fun addProductToCart(producto: Producto) {
        val currentCount = _cart.value[producto] ?: 0
        if (currentCount < producto.stock) {
            _cart.value = _cart.value + (producto to (currentCount + 1))
        }
    }

    fun removeProductFromCart(producto: Producto) {
        val currentCount = _cart.value[producto] ?: return
        if (currentCount <= 1) {
            _cart.value = _cart.value - producto
        } else {
            _cart.value = _cart.value + (producto to (currentCount - 1))
        }
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    val cartTotal: StateFlow<Double> = _cart.map { cartMap ->
        cartMap.entries.sumOf { it.key.precio * it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun finalizeSale() {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return

        viewModelScope.launch {
            val total = cartTotal.value
            // Update stocks
            currentCart.forEach { (prod, qty) ->
                val newStock = (prod.stock - qty).coerceAtLeast(0)
                repository.updateProducto(prod.copy(stock = newStock))
            }

            // Create transaction of type "ingreso"
            val itemNames = currentCart.map { "${it.value}x ${it.key.nombre}" }.joinToString(", ")
            val cal = Calendar.getInstance()
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())

            addTransaction(
                monto = total,
                categoria = "Ventas",
                descripcion = "Venta de Catálogo: $itemNames",
                fecha = cal.timeInMillis,
                hora = format.format(cal.time),
                metodoPago = "Efectivo",
                esEmpleador = (_appMode.value == AppMode.WORK_EMPLOYER || _appMode.value == AppMode.WORK_EMPLOYEE),
                tipo = "ingreso"
            )

            clearCart()
        }
    }

    // ==========================================
    // EMPLOYEE MANAGEMENT (Employer mode)
    // ==========================================
    val activeEmployees: StateFlow<List<Empleado>> = repository.allEmpleados
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEmployee(nombre: String, telefono: String, fotoUri: String? = null) {
        viewModelScope.launch {
            val emp = Empleado(
                nombre = nombre,
                telefono = telefono,
                estado = "activo",
                foto_uri = fotoUri,
                fecha_vinculacion = System.currentTimeMillis()
            )
            repository.insertEmpleado(emp)
        }
    }

    fun updateEmployeePhoto(empleado: Empleado, photoUri: String) {
        if (empleado.foto_uri == photoUri) return
        viewModelScope.launch {
            repository.updateEmpleado(empleado.copy(foto_uri = photoUri))
        }
    }

    fun toggleEmployeeStatus(empleado: Empleado) {
        viewModelScope.launch {
            val nextEstado = if (empleado.estado == "activo") "inactivo" else "activo"
            repository.updateEmpleado(empleado.copy(estado = nextEstado))
        }
    }

    // ==========================================
    // CHAT SYSTEM (WhatsApp Style Communication)
    // ==========================================
    val conversations: StateFlow<List<Contacto>> = repository.allContactos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeChat = MutableStateFlow<Contacto?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getActiveMessages(): Flow<List<Mensaje>> = activeChat.flatMapLatest { contact ->
        if (contact != null) {
            repository.getMensajesForContacto(contact.id)
        } else {
            flowOf(emptyList())
        }
    }

    fun sendMessageToChat(contactId: Long, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // Insert the message
            val msg = Mensaje(
                contacto_id = contactId,
                es_enviado = true,
                contenido = content,
                timestamp = now
            )
            repository.insertMensaje(msg)

            // Update contact last message
            val contact = conversations.value.find { it.id == contactId }
            if (contact != null) {
                repository.updateContacto(
                    contact.copy(
                        ultimo_mensaje = content,
                        hora_ultimo_mensaje = now,
                        mensajes_no_leidos = 0
                    )
                )
            }
        }
    }

    fun createOrGetContactForChat(nombre: String, telefono: String, onFinished: (Contacto) -> Unit) {
        viewModelScope.launch {
            var existing = repository.getContactoByTelefono(telefono)
            if (existing == null) {
                val newC = Contacto(nombre = nombre, telefono = telefono)
                val id = repository.insertContacto(newC)
                existing = newC.copy(id = id)
            }
            onFinished(existing)
        }
    }

    // Helper to clear unread counts on open
    fun selectActiveChat(contact: Contacto?) {
        activeChat.value = contact
        if (contact != null && contact.mensajes_no_leidos > 0) {
            viewModelScope.launch {
                repository.updateContacto(contact.copy(mensajes_no_leidos = 0))
            }
        }
    }

    // ==========================================
    // TRANSFER / PAYMENT BY TRANSFERMÓVIL
    // ==========================================
    private val _transferAmount = MutableStateFlow("")
    val transferAmount: StateFlow<String> = _transferAmount.asStateFlow()

    private val _transferPhone = MutableStateFlow("")
    val transferPhone: StateFlow<String> = _transferPhone.asStateFlow()

    private val _lastTransferRecipient = MutableStateFlow("")
    val lastTransferRecipient: StateFlow<String> = _lastTransferRecipient.asStateFlow()

    private val _showTransferSuccessDialog = MutableStateFlow(false)
    val showTransferSuccessDialog: StateFlow<Boolean> = _showTransferSuccessDialog.asStateFlow()

    // Configured employer's Transfermóvil number
    val employerTransfermovilNumber = flow {
        val num = repository.getConfiguracionByKey("numero_transfermovil")?.valor.orEmpty()
        emit(num)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun updateTransferPhone(phone: String) {
        _transferPhone.value = phone
    }

    fun updateTransferAmount(amount: String) {
        _transferAmount.value = amount
    }

    fun executeTransfer(onSuccess: () -> Unit) {
        val amountVal = _transferAmount.value.toDoubleOrNull() ?: return
        val phoneVal = _transferPhone.value.ifBlank { "Destinatario" }

        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())

            addTransaction(
                monto = amountVal,
                categoria = "Transferencia",
                descripcion = "Transferencia enviada a $phoneVal",
                fecha = cal.timeInMillis,
                hora = format.format(cal.time),
                metodoPago = "Transfermóvil",
                esEmpleador = false,
                tipo = "gasto"
            )

            _lastTransferRecipient.value = phoneVal
            _showTransferSuccessDialog.value = true

            _transferAmount.value = ""
            _transferPhone.value = ""

            onSuccess()
        }
    }

    fun registerEmployerManualPayment(amount: Double) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())

            addTransaction(
                monto = amount,
                categoria = "Ventas",
                descripcion = "Pago recibido por Transfermóvil",
                fecha = cal.timeInMillis,
                hora = format.format(cal.time),
                metodoPago = "Transfermóvil",
                esEmpleador = true,
                tipo = "ingreso"
            )
        }
    }

    fun dismissTransferSuccessDialog() {
        _showTransferSuccessDialog.value = false
    }

    // Setting configuration modifications
    fun exportDataToCSV(): String {
        val txs = allTransactions.value
        val sb = java.lang.StringBuilder()
        sb.append("ID,Tipo,Monto,Categoria,Descripcion,Fecha,Hora,MetodoPago,EsEmpleador\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (tx in txs) {
            sb.append("${tx.id},${tx.tipo},${tx.monto},${tx.categoria},\"${tx.descripcion}\",${sdf.format(Date(tx.fecha))},${tx.hora},${tx.metodo_pago},${tx.es_empleador}\n")
        }
        return sb.toString()
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
            repository.deleteAllProductos()
            repository.deleteAllContactos()
            repository.deleteAllConfiguraciones()
            repository.deleteAllAuditorias()
            // reload configs
            loadConfigurations()
        }
    }
}
