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
import com.example.data.model.PinHasher
import com.example.data.qvapay.QvaPayApiService
import com.example.data.qvapay.QvaPayCoin
import com.example.data.qvapay.QvaPayUserInfo
import com.example.data.qvapay.QvaPayTransaction
import com.example.data.qvapay.QvaPayInvoice
import com.example.data.model.SaldoMovil
import com.example.ui.components.PeekPreviewType
import com.example.data.repository.MoneyRepository
import android.telephony.TelephonyManager
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
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
    val name: String = "Empleador",
    val phone: String = "",
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
            propuestaCambioDao = db.propuestaCambioDao(),
            branchDao = db.branchDao(),
            despachoDistribuidorDao = db.despachoDistribuidorDao(),
            saldoMovilDao = db.saldoMovilDao()
        )
        viewModelScope.launch {
            repository.checkAndSeedData()
            // Initial load of config values
            loadConfigurations()
            // Ensure some products exist in work catalog
            checkAndSeedProducts()
        }

        // Collect SmsReceiver flows for transfer confirmations
        viewModelScope.launch {
            com.example.data.receiver.SmsReceiver.pendingTransferAlert.collect { tx ->
                if (appMode.value == AppMode.WORK_EMPLOYER) {
                    pendingEmployerConfirmationTx.value = tx
                }
            }
        }

        viewModelScope.launch {
            com.example.data.receiver.SmsReceiver.employeeConfirmationSmsReceived.collect { payload ->
                if (appMode.value == AppMode.WORK_EMPLOYEE) {
                    val sender = payload["sender"] ?: ""
                    val parsedMonto = payload["monto"]?.toDoubleOrNull() ?: 0.0
                    val parsedMoneda = payload["moneda"] ?: "CUP"
                    val parsedId = payload["id"] ?: ""
                    val parsedHora = payload["hora"] ?: ""

                    if (parsedMonto > 0.0) {
                        val allTxs = repository.allTransactions.first()
                        val esperas = allTxs.filter { it.descripcion.startsWith("[Espera]") }

                        Log.d("MoneyViewModel", "Processing Employee SMS confirmation. Esperas: ${esperas.size}, amount: $parsedMonto")

                        if (esperas.isEmpty()) {
                            addTransaction(
                                monto = parsedMonto,
                                categoria = "Ventas",
                                descripcion = "Pago recibido (Auto-CONF sin artículo): ID $parsedId de $sender",
                                fecha = System.currentTimeMillis(),
                                hora = parsedHora.ifBlank { "00:00" },
                                metodoPago = "Transfermóvil",
                                esEmpleador = true,
                                tipo = "ingreso"
                            )
                        } else if (esperas.size == 1) {
                            val wait = esperas.first()
                            if (Math.abs(wait.monto - parsedMonto) < 0.01) {
                                confirmEsperaTransaction(wait, parsedId)
                            } else {
                                addTransaction(
                                    monto = parsedMonto,
                                    categoria = "Ventas",
                                    descripcion = "Pago recibido (Auto-CONF monto distinto): ID $parsedId de $sender",
                                    fecha = System.currentTimeMillis(),
                                    hora = parsedHora.ifBlank { "00:00" },
                                    metodoPago = "Transfermóvil",
                                    esEmpleador = true,
                                    tipo = "ingreso"
                                )
                            }
                        } else {
                            val matchingWaits = esperas.filter { Math.abs(it.monto - parsedMonto) < 0.01 }
                            if (matchingWaits.isEmpty()) {
                                addTransaction(
                                    monto = parsedMonto,
                                    categoria = "Ventas",
                                    descripcion = "Pago recibido (Auto-CONF sin coincidencia de monto): ID $parsedId de $sender",
                                    fecha = System.currentTimeMillis(),
                                    hora = parsedHora.ifBlank { "00:00" },
                                    metodoPago = "Transfermóvil",
                                    esEmpleador = true,
                                    tipo = "ingreso"
                                )
                            } else if (matchingWaits.size == 1) {
                                confirmEsperaTransaction(matchingWaits.first(), parsedId)
                            } else {
                                ambiguousConfirmations.value = matchingWaits
                                ambiguousParsedId.value = parsedId
                            }
                        }
                    }
                }
            }
        }
    }

    fun deleteContacto(contact: Contacto) {
        viewModelScope.launch {
            repository.deleteContacto(contact)
            repository.deleteMensajesForContacto(contact.id)
            if (activeChat.value?.id == contact.id) {
                activeChat.value = null
            }
        }
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

    // Real-Time Balance & Statistics Computations por moneda
    val totalIncomeMap: StateFlow<Map<String, Double>> = filteredByProfileTransactions.map { list ->
        list.filter { it.tipo.equals("ingreso", ignoreCase = true) }
            .groupBy { it.moneda.uppercase() }
            .mapValues { (_, txs) -> txs.sumOf { it.monto } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val totalExpenseMap: StateFlow<Map<String, Double>> = filteredByProfileTransactions.map { list ->
        list.filter { it.tipo.equals("gasto", ignoreCase = true) }
            .groupBy { it.moneda.uppercase() }
            .mapValues { (_, txs) -> txs.sumOf { it.monto } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val availableBalanceMap: StateFlow<Map<String, Double>> = combine(totalIncomeMap, totalExpenseMap) { incMap, expMap ->
        val keys = incMap.keys + expMap.keys
        keys.associateWith { key ->
            (incMap[key] ?: 0.0) - (expMap[key] ?: 0.0)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // CUP stateflows for direct backward compatibility
    val totalIncome: StateFlow<Double> = totalIncomeMap.map { map ->
        map["CUP"] ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = totalExpenseMap.map { map ->
        map["CUP"] ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val availableBalance: StateFlow<Double> = availableBalanceMap.map { map ->
        map["CUP"] ?: 0.0
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

    // Onboarding and Permissions Disclosure state
    val showOnboardingTour = MutableStateFlow(false)
    val showPermissionsDisclosure = MutableStateFlow(false)

    // Dynamic Launcher Icon state
    val activeLauncherIcon = MutableStateFlow("NormalAlias")

    // Employer to Employee transfer confirmation state
    val pendingEmployerConfirmationTx = MutableStateFlow<Transaction?>(null)
    val ambiguousConfirmations = MutableStateFlow<List<Transaction>>(emptyList())
    val ambiguousParsedId = MutableStateFlow("")

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
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "modo_activo_actual", valor = mode.name))
        }
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    // --- SISTEMA DE FRASES MOTIVACIONALES (500 FRASES) ---
    val currentMotivationalPhrase = MutableStateFlow(com.example.data.model.MotivationalPhrases.getRandomPhrase("bienvenida"))
    private var lastPhrase = ""

    fun selectNewMotivationalPhrase(category: String) {
        var newPhrase = com.example.data.model.MotivationalPhrases.getRandomPhrase(category)
        var attempts = 0
        while (newPhrase == lastPhrase && attempts < 10) {
            newPhrase = com.example.data.model.MotivationalPhrases.getRandomPhrase(category)
            attempts++
        }
        lastPhrase = newPhrase
        currentMotivationalPhrase.value = newPhrase
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
                categoria = categoria,
                descripcion = descripcion,
                fecha = fecha,
                hora = hora,
                metodo_pago = metodoPago,
                es_empleador = esEmpleador,
                moneda = moneda
            )
            try {
                repository.insertTransaction(tx)
                localTransactionError.value = null
            } catch (e: Exception) {
                Log.e("MoneyViewModel", "Error al registrar la transacción localmente: ${e.message}", e)
                localTransactionError.value = "Error al guardar transacción local: ${e.localizedMessage}. La operación real pudo haberse realizado pero no se registró en esta base de datos. Transacción: $tx"
            }

            // Select contextual cuban phrase based on transaction behavior
            if (tipo.equals("ingreso", ignoreCase = true)) {
                if (availableBalance.value > 15000.0) {
                    selectNewMotivationalPhrase("balance_positivo")
                } else {
                    selectNewMotivationalPhrase("ingreso")
                }
            } else {
                if (monto > 4000.0) {
                    selectNewMotivationalPhrase("gasto_alto")
                } else if (availableBalance.value - monto < 0.0) {
                    selectNewMotivationalPhrase("balance_negativo")
                } else {
                    selectNewMotivationalPhrase("gasto")
                }
            }

            // Evaluate dynamic icon on new transaction
            if (tipo.equals("ingreso", ignoreCase = true)) {
                activeLauncherIcon.value = "IngresoAlias"
            } else if (tipo.equals("gasto", ignoreCase = true)) {
                activeLauncherIcon.value = "GastoAlias"
            }

            // Also check balance trend
            val trend = trendPercentage.value
            if (trend.startsWith("+") && trend != "+0.0%") {
                activeLauncherIcon.value = "TendenciaPosAlias"
            } else if (trend.startsWith("-") && trend != "-0.0%") {
                activeLauncherIcon.value = "TendenciaNegAlias"
            }
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
    var cachedSecuritySalt = "CubaFinanzasSalt2026"
    val isSecurityAuthEnabled = MutableStateFlow(false)
    val userSecurityPin = MutableStateFlow("")
    val isAuthDialogVisible = MutableStateFlow(false)
    val authDialogTitle = MutableStateFlow("Autenticación Requerida")
    val authDialogReason = MutableStateFlow("Confirma tu identidad para continuar.")
    private var pendingAuthSuccessCallback: (() -> Unit)? = null

    fun verifyPin(entered: String): Boolean {
        val saved = userSecurityPin.value
        if (saved.length == 4) {
            return entered == saved
        }
        return PinHasher.hash(entered, cachedSecuritySalt) == saved
    }

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
        viewModelScope.launch {
            var salt = repository.getConfiguracionByKey("user_security_salt")?.valor
            if (salt.isNullOrBlank()) {
                salt = java.util.UUID.randomUUID().toString().take(8)
                repository.insertConfiguracion(Configuracion(clave = "user_security_salt", valor = salt))
            }
            val hashedPin = if (newPin.length == 4) PinHasher.hash(newPin, salt) else newPin
            requestSecurityAuth(
                title = "Ajustes de Seguridad",
                reason = "Confirma tu identidad para modificar la autenticación de acciones sensibles."
            ) {
                viewModelScope.launch {
                    isSecurityAuthEnabled.value = enabled
                    userSecurityPin.value = hashedPin
                    repository.insertConfiguracion(Configuracion(clave = "security_auth_enabled", valor = enabled.toString()))
                    repository.insertConfiguracion(Configuracion(clave = "user_security_pin", valor = hashedPin))
                }
            }
        }
    }

    // Ramas de la Empresa (Rama Principal y Ramas Secundarias / Sucursales)
    val companyBranches: StateFlow<List<BranchInfo>> = repository.allBranches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCompanyBranch(name: String, address: String, managerName: String) {
        requestSecurityAuth(
            title = "Agregar Nueva Rama / Sucursal",
            reason = "Autentícate con PIN o Huella para vincular la rama '$name' a la principal."
        ) {
            viewModelScope.launch {
                val nextId = (companyBranches.value.maxOfOrNull { it.id } ?: 0) + 1
                val newBranch = BranchInfo(
                    id = nextId,
                    name = name,
                    address = address,
                    isMain = false,
                    managerName = managerName
                )
                repository.insertBranch(newBranch)
            }
        }
    }

    // Pending mode reactivation dialog (Empleador, Empleado, Distribuidor)
    val pendingModeReactivation = MutableStateFlow<AppMode?>(null)

    // iOS Extended Peek & Pop Preview state
    val activePeekPreview = MutableStateFlow<PeekPreviewType?>(null)

    // Distributor Dispatches (Despachos de Mercancía)
    val despachosDistribuidor: StateFlow<List<DespachoDistribuidor>> = repository.allDespachos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

        val empName = repository.getConfiguracionByKey("nombre_empleador")?.valor ?: "Empleador"
        val empPhone = repository.getConfiguracionByKey("telefono_empleador")?.valor ?: ""
        val empPhoto = repository.getConfiguracionByKey("foto_empleador")?.valor

        isEmployerModeEnabled.value = empActive
        isEmployeeModeEnabled.value = workerActive
        isDistributorModeEnabled.value = distActive

        val lastActiveModeStr = repository.getConfiguracionByKey("modo_activo_actual")?.valor
        val lastActiveMode = lastActiveModeStr?.let {
            try { AppMode.valueOf(it) } catch (e: Exception) { null }
        }

        _appMode.value = when {
            lastActiveMode == AppMode.WORK_EMPLOYER && empActive -> AppMode.WORK_EMPLOYER
            lastActiveMode == AppMode.WORK_EMPLOYEE && workerActive -> AppMode.WORK_EMPLOYEE
            lastActiveMode == AppMode.WORK_DISTRIBUTOR && distActive -> AppMode.WORK_DISTRIBUTOR
            lastActiveMode == AppMode.PERSONAL -> AppMode.PERSONAL
            empActive -> AppMode.WORK_EMPLOYER
            workerActive -> AppMode.WORK_EMPLOYEE
            distActive -> AppMode.WORK_DISTRIBUTOR
            else -> AppMode.PERSONAL
        }

        val activeEmpIdStr = repository.getConfiguracionByKey("empleado_activo_id")?.valor
        empleadoActivoId.value = activeEmpIdStr?.toLongOrNull()

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
        val secAuth = repository.getConfiguracionByKey("security_auth_enabled")?.valor?.toBoolean() ?: false
        val secPinRaw = repository.getConfiguracionByKey("user_security_pin")?.valor ?: ""

        var salt = repository.getConfiguracionByKey("user_security_salt")?.valor
        if (salt.isNullOrBlank() && secPinRaw.isNotBlank()) {
            salt = "CubaFinanzasSalt2026" // Legacy fallback salt for existing PIN upgrade
        } else if (salt.isNullOrBlank()) {
            salt = java.util.UUID.randomUUID().toString().take(8)
            repository.insertConfiguracion(Configuracion(clave = "user_security_salt", valor = salt))
        }

        cachedSecuritySalt = salt!!

        val secPin = if (secPinRaw.isNotBlank()) {
            if (secPinRaw.length == 4) {
                val hashed = PinHasher.hash(secPinRaw, salt!!)
                repository.insertConfiguracion(Configuracion(clave = "user_security_pin", valor = hashed))
                hashed
            } else {
                secPinRaw
            }
        } else {
            ""
        }
        isSecurityAuthEnabled.value = if (secPin.isBlank()) false else secAuth
        userSecurityPin.value = secPin

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

        val onboardingSeen = repository.getConfiguracionByKey("onboarding_seen")?.valor?.toBoolean() ?: false
        val permissionsDisclosed = repository.getConfiguracionByKey("permissions_disclosed")?.valor?.toBoolean() ?: false

        showOnboardingTour.value = !onboardingSeen
        if (onboardingSeen && !permissionsDisclosed) {
            showPermissionsDisclosure.value = true
        }
    }

    fun setOnboardingFinished() {
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "onboarding_seen", valor = "true"))
            showOnboardingTour.value = false

            val permissionsDisclosed = repository.getConfiguracionByKey("permissions_disclosed")?.valor?.toBoolean() ?: false
            if (!permissionsDisclosed) {
                showPermissionsDisclosure.value = true
            }
        }
    }

    fun setPermissionsDisclosedFinished() {
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "permissions_disclosed", valor = "true"))
            showPermissionsDisclosure.value = false
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
            repository.insertConfiguracion(Configuracion(clave = "modo_activo_actual", valor = _appMode.value.name))
        }
    }

    fun toggleEmployeeModeActive(enabled: Boolean, resetData: Boolean = false) {
        isEmployeeModeEnabled.value = enabled
        if (!enabled && _appMode.value == AppMode.WORK_EMPLOYEE) {
            _appMode.value = AppMode.PERSONAL
        }
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "modo_empleado_activo", valor = enabled.toString()))
            repository.insertConfiguracion(Configuracion(clave = "modo_activo_actual", valor = _appMode.value.name))
        }
    }

    fun toggleDistributorModeActive(enabled: Boolean, resetData: Boolean = false) {
        isDistributorModeEnabled.value = enabled
        if (!enabled && _appMode.value == AppMode.WORK_DISTRIBUTOR) {
            _appMode.value = AppMode.PERSONAL
        }
        if (enabled && resetData) {
            viewModelScope.launch {
                repository.deleteAllDespachos()
            }
        }
        viewModelScope.launch {
            repository.insertConfiguracion(Configuracion(clave = "modo_distribuidor_activo", valor = enabled.toString()))
            repository.insertConfiguracion(Configuracion(clave = "modo_activo_actual", valor = _appMode.value.name))
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
        viewModelScope.launch {
            repository.insertDespacho(newDespacho)
        }
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

    // --- REAL P2P SYNCHRONIZATION OVER TCP ---
    val p2pState = MutableStateFlow("IDLE") // "IDLE", "HOSTING", "CONNECTING", "SYNCING", "COMPLETED", "ERROR"
    val p2pIpAddress = MutableStateFlow("")
    val p2pStatusMessage = MutableStateFlow<String?>(null)

    fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val ip = addr.hostAddress ?: ""
                        if (ip.isNotBlank()) return ip
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IP", "Error getting local IP: ${e.message}")
        }
        return "192.168.1.105" // standard fallback
    }

    private var serverJob: kotlinx.coroutines.Job? = null

    // Helper functions for DRY payload exchange
    fun generateServerPayload(): String {
        return try {
            val productsList = kotlinx.coroutines.runBlocking { repository.allProductos.first() }
            val employeesList = kotlinx.coroutines.runBlocking { repository.allEmpleados.first() }

            JSONObject().apply {
                val pArray = JSONArray()
                productsList.forEach { p ->
                    pArray.put(JSONObject().apply {
                        put("nombre", p.nombre)
                        put("precio", p.precio)
                        put("stock", p.stock)
                        put("imagen_uri", p.imagen_uri)
                    })
                }
                put("productos", pArray)

                val eArray = JSONArray()
                employeesList.forEach { e ->
                    eArray.put(JSONObject().apply {
                        put("nombre", e.nombre)
                        put("telefono", e.telefono)
                        put("estado", e.estado)
                        put("fecha_vinculacion", e.fecha_vinculacion)
                    })
                }
                put("empleados", eArray)
            }.toString()
        } catch (e: Exception) {
            "{}"
        }
    }

    fun generateClientPayload(proposalsList: List<PropuestaCambio>): String {
        return try {
            JSONObject().apply {
                val pArray = JSONArray()
                proposalsList.forEach { prop ->
                    pArray.put(JSONObject().apply {
                        put("empleado_nombre", prop.empleado_nombre)
                        put("producto_id", prop.producto_id)
                        put("nombre_producto", prop.nombre_producto)
                        put("precio_propuesto", prop.precio_propuesto)
                        put("stock_propuesto", prop.stock_propuesto)
                        put("justificacion", prop.justificacion)
                        put("timestamp", prop.timestamp)
                    })
                }
                put("propuestas", pArray)
            }.toString()
        } catch (e: Exception) {
            "{}"
        }
    }

    suspend fun processClientPayload(jsonStr: String): Int {
        var proposalsCount = 0
        try {
            val clientPayload = JSONObject(jsonStr)
            val receivedPropuestas = clientPayload.optJSONArray("propuestas")
            if (receivedPropuestas != null) {
                for (i in 0 until receivedPropuestas.length()) {
                    val pObj = receivedPropuestas.getJSONObject(i)
                    val prop = PropuestaCambio(
                        empleado_nombre = pObj.optString("empleado_nombre", "Sucursal"),
                        producto_id = if (pObj.has("producto_id") && !pObj.isNull("producto_id")) pObj.getLong("producto_id") else null,
                        nombre_producto = pObj.optString("nombre_producto"),
                        precio_propuesto = pObj.optDouble("precio_propuesto"),
                        stock_propuesto = pObj.optInt("stock_propuesto"),
                        justificacion = pObj.optString("justificacion"),
                        estado = "pendiente",
                        timestamp = pObj.optLong("timestamp", System.currentTimeMillis())
                    )
                    repository.insertPropuesta(prop)
                    proposalsCount++
                }
            }
        } catch (e: Exception) {
            Log.e("P2PSync", "Error processing client payload: ${e.message}")
        }
        return proposalsCount
    }

    suspend fun processServerPayload(jsonStr: String): Int {
        var productsImported = 0
        try {
            val serverPayload = JSONObject(jsonStr)
            val receivedProducts = serverPayload.optJSONArray("productos")
            if (receivedProducts != null) {
                for (i in 0 until receivedProducts.length()) {
                    val pObj = receivedProducts.getJSONObject(i)
                    val name = pObj.optString("nombre")
                    val price = pObj.optDouble("precio")
                    val stock = pObj.optInt("stock")
                    val img = if (pObj.has("imagen_uri") && !pObj.isNull("imagen_uri")) pObj.getString("imagen_uri") else null

                    val existing = repository.allProductos.first().find { it.nombre.equals(name, ignoreCase = true) }
                    if (existing != null) {
                        repository.updateProducto(existing.copy(precio = price, stock = stock, imagen_uri = img))
                    } else {
                        repository.insertProducto(Producto(nombre = name, precio = price, stock = stock, imagen_uri = img))
                    }
                    productsImported++
                }
            }

            val receivedEmployees = serverPayload.optJSONArray("empleados")
            if (receivedEmployees != null) {
                for (i in 0 until receivedEmployees.length()) {
                    val eObj = receivedEmployees.getJSONObject(i)
                    val name = eObj.optString("nombre")
                    val phone = eObj.optString("telefono")
                    val state = eObj.optString("estado")
                    val dateJoined = eObj.optLong("fecha_vinculacion")

                    val existing = repository.allEmpleados.first().find { it.nombre.equals(name, ignoreCase = true) }
                    if (existing == null) {
                        repository.insertEmpleado(Empleado(nombre = name, telefono = phone, estado = state, fecha_vinculacion = dateJoined))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("P2PSync", "Error processing server payload: ${e.message}")
        }
        return productsImported
    }

    fun startP2PSyncServer() {
        serverJob?.cancel()
        serverJob = viewModelScope.launch(Dispatchers.IO) {
            val ip = getLocalIpAddress()
            p2pIpAddress.value = ip
            p2pState.value = "HOSTING"
            p2pStatusMessage.value = "Socio Principal esperando conexión en $ip:8888..."

            var serverSocket: java.net.ServerSocket? = null
            try {
                serverSocket = java.net.ServerSocket(8888)
                serverSocket.soTimeout = 40000 // 40 sec timeout
                val socket = serverSocket.accept()

                p2pState.value = "SYNCING"
                p2pStatusMessage.value = "Conexión recibida de sucursal. Sincronizando datos..."

                val reader = java.io.BufferedReader(java.io.InputStreamReader(socket.getInputStream(), "UTF-8"))
                val jsonStr = reader.readLine() ?: "{}"

                val proposalsCount = processClientPayload(jsonStr)

                val serverPayload = generateServerPayload()
                val writer = java.io.OutputStreamWriter(socket.getOutputStream(), "UTF-8")
                writer.write(serverPayload + "\n")
                writer.flush()

                socket.close()
                p2pState.value = "COMPLETED"
                p2pStatusMessage.value = "¡Sincronización P2P Completada! Recibiste $proposalsCount propuestas de cambios."
            } catch (e: Exception) {
                p2pState.value = "ERROR"
                p2pStatusMessage.value = "Error de sincronización: ${e.message ?: e.localizedMessage}"
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }
    }

    fun connectToP2PServer(targetIp: String) {
        viewModelScope.launch(Dispatchers.IO) {
            p2pState.value = "CONNECTING"
            p2pStatusMessage.value = "Conectando al Socio Principal en $targetIp:8888..."

            var socket: java.net.Socket? = null
            try {
                socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress(targetIp, 8888), 15000) // 15s timeout

                p2pState.value = "SYNCING"
                p2pStatusMessage.value = "Conectado. Transfiriendo tus propuestas pendientes..."

                val proposalsList = repository.propuestasPendientes.first()
                val clientPayload = generateClientPayload(proposalsList)

                val writer = java.io.OutputStreamWriter(socket.getOutputStream(), "UTF-8")
                writer.write(clientPayload + "\n")
                writer.flush()

                p2pStatusMessage.value = "Recibiendo catálogo de productos unificado..."
                val reader = java.io.BufferedReader(java.io.InputStreamReader(socket.getInputStream(), "UTF-8"))
                val jsonStr = reader.readLine() ?: "{}"

                val productsImported = processServerPayload(jsonStr)

                p2pState.value = "COMPLETED"
                p2pStatusMessage.value = "¡Sincronización exitosa! Importaste $productsImported productos unificados."
            } catch (e: Exception) {
                p2pState.value = "ERROR"
                p2pStatusMessage.value = "Error al conectar o sincronizar: ${e.message ?: e.localizedMessage}"
            } finally {
                try { socket?.close() } catch (e: Exception) {}
            }
        }
    }

    fun simulateP2PSync() {
        if (appMode.value == AppMode.WORK_EMPLOYER) {
            startP2PSyncServer()
        } else {
            connectToP2PServer("192.168.1.105")
        }
    }

    // WiFi Direct (WifiP2pManager) Real Implementation
    val wifiP2pDevices = MutableStateFlow<List<android.net.wifi.p2p.WifiP2pDevice>>(emptyList())

    fun startWifiP2pDiscovery() {
        val context = getApplication<Application>().applicationContext
        val manager = context.getSystemService(android.content.Context.WIFI_P2P_SERVICE) as? android.net.wifi.p2p.WifiP2pManager
        val channel = manager?.initialize(context, android.os.Looper.getMainLooper(), null)

        p2pState.value = "SEARCHING"
        p2pStatusMessage.value = "Buscando dispositivos WiFi Directo..."

        if (manager == null || channel == null) {
            simulateWifiP2pDiscovery()
            return
        }

        try {
            manager.discoverPeers(channel, object : android.net.wifi.p2p.WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    p2pStatusMessage.value = "Buscando... WiFi Directo iniciado."
                    manager.requestPeers(channel) { peerList ->
                        val list = peerList.deviceList.toList()
                        wifiP2pDevices.value = list
                        if (list.isEmpty()) {
                            p2pStatusMessage.value = "No se encontraron dispositivos WiFi Directo cercanos."
                        } else {
                            p2pState.value = "FOUND"
                            p2pStatusMessage.value = "Dispositivos WiFi Directo encontrados: ${list.size}"
                        }
                    }
                }
                override fun onFailure(reason: Int) {
                    p2pStatusMessage.value = "Error al iniciar búsqueda WiFi Directo (Código: $reason)."
                    simulateWifiP2pDiscovery()
                }
            })
        } catch (e: SecurityException) {
            p2pStatusMessage.value = "Falta permiso de ubicación para buscar WiFi Directo."
            simulateWifiP2pDiscovery()
        } catch (e: Exception) {
            simulateWifiP2pDiscovery()
        }
    }

    private fun simulateWifiP2pDiscovery() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            val simulatedList = listOf(
                createSimulatedWifiP2pDevice("Socio Principal WiFi Directo", "02:1a:2b:3c:4d:5e"),
                createSimulatedWifiP2pDevice("Sucursal Playa WiFi Directo", "02:1a:2b:3c:4d:5f")
            )
            wifiP2pDevices.value = simulatedList
            p2pState.value = "FOUND"
            p2pStatusMessage.value = "Dispositivos WiFi Directo encontrados (Simulados): ${simulatedList.size}"
        }
    }

    private fun createSimulatedWifiP2pDevice(name: String, address: String): android.net.wifi.p2p.WifiP2pDevice {
        val d = android.net.wifi.p2p.WifiP2pDevice()
        d.deviceName = name
        d.deviceAddress = address
        d.status = android.net.wifi.p2p.WifiP2pDevice.AVAILABLE
        return d
    }

    fun connectToWifiDirectPeer(device: android.net.wifi.p2p.WifiP2pDevice) {
        val context = getApplication<Application>().applicationContext
        val manager = context.getSystemService(android.content.Context.WIFI_P2P_SERVICE) as? android.net.wifi.p2p.WifiP2pManager
        val channel = manager?.initialize(context, android.os.Looper.getMainLooper(), null)

        p2pState.value = "CONNECTING"
        p2pStatusMessage.value = "Estableciendo enlace WiFi Directo con ${device.deviceName}..."

        if (manager == null || channel == null) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(1500)
                p2pStatusMessage.value = "Conexión WiFi Directo simulada exitosa. Sincronizando..."
                if (appMode.value == AppMode.WORK_EMPLOYER) {
                    startP2PSyncServer()
                } else {
                    connectToP2PServer("192.168.1.105")
                }
            }
            return
        }

        val config = android.net.wifi.p2p.WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        try {
            manager.connect(channel, config, object : android.net.wifi.p2p.WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    manager.requestConnectionInfo(channel) { info ->
                        val groupOwnerAddress = info.groupOwnerAddress?.hostAddress
                        if (groupOwnerAddress != null) {
                            if (info.groupFormed) {
                                if (appMode.value == AppMode.WORK_EMPLOYER) {
                                    startP2PSyncServer()
                                } else {
                                    connectToP2PServer(groupOwnerAddress)
                                }
                            }
                        } else {
                            if (appMode.value == AppMode.WORK_EMPLOYER) {
                                startP2PSyncServer()
                            } else {
                                connectToP2PServer("192.168.49.1")
                            }
                        }
                    }
                }
                override fun onFailure(reason: Int) {
                    p2pState.value = "ERROR"
                    p2pStatusMessage.value = "Conexión WiFi Directo fallida (Código: $reason)."
                }
            })
        } catch (e: SecurityException) {
            p2pState.value = "ERROR"
            p2pStatusMessage.value = "Falta de permisos para conectar por WiFi Directo."
        } catch (e: Exception) {
            p2pState.value = "ERROR"
            p2pStatusMessage.value = "Error al conectar por WiFi Directo: ${e.message}"
        }
    }

    // Bluetooth Classic Real Implementation
    data class SimpleBluetoothDevice(
        val name: String,
        val address: String,
        val realDevice: android.bluetooth.BluetoothDevice? = null
    )

    val bluetoothDevices = MutableStateFlow<List<SimpleBluetoothDevice>>(emptyList())
    private var btServerJob: kotlinx.coroutines.Job? = null

    fun startBluetoothDiscovery() {
        val context = getApplication<Application>().applicationContext
        val bluetoothManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val adapter = bluetoothManager?.adapter ?: android.bluetooth.BluetoothAdapter.getDefaultAdapter()

        p2pState.value = "SEARCHING"
        p2pStatusMessage.value = "Buscando dispositivos Bluetooth..."

        if (adapter == null) {
            simulateBluetoothDiscovery()
            return
        }

        val list = mutableListOf<SimpleBluetoothDevice>()
        try {
            val bonded = adapter.bondedDevices
            bonded?.forEach { dev ->
                list.add(SimpleBluetoothDevice(dev.name ?: "Dispositivo Bluetooth", dev.address, dev))
            }
        } catch (e: SecurityException) {
            p2pStatusMessage.value = "Permiso denegado para consultar Bluetooth."
        } catch (e: Exception) {}

        bluetoothDevices.value = list

        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            adapter.startDiscovery()
            p2pStatusMessage.value = "Escaneando dispositivos Bluetooth..."

            val filter = android.content.IntentFilter(android.bluetooth.BluetoothDevice.ACTION_FOUND)
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                    if (intent?.action == android.bluetooth.BluetoothDevice.ACTION_FOUND) {
                        val dev = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE, android.bluetooth.BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
                        }
                        if (dev != null) {
                            val name = dev.name ?: "Dispositivo Bluetooth"
                            val address = dev.address
                            if (bluetoothDevices.value.none { it.address == address }) {
                                bluetoothDevices.value = bluetoothDevices.value + SimpleBluetoothDevice(name, address, dev)
                            }
                        }
                    }
                }
            }
            context.registerReceiver(receiver, filter)
        } catch (e: SecurityException) {
            simulateBluetoothDiscovery()
        } catch (e: Exception) {
            simulateBluetoothDiscovery()
        }
    }

    private fun simulateBluetoothDiscovery() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            val simulatedList = listOf(
                SimpleBluetoothDevice("Socio Bluetooth Principal (Simulado)", "00:11:22:33:44:55"),
                SimpleBluetoothDevice("Redmi Trabajador Bluetooth (Simulado)", "00:11:22:33:44:66")
            )
            bluetoothDevices.value = simulatedList
            p2pState.value = "FOUND"
            p2pStatusMessage.value = "Dispositivos Bluetooth encontrados: ${simulatedList.size}"
        }
    }

    fun startBluetoothSyncServer() {
        btServerJob?.cancel()
        btServerJob = viewModelScope.launch(Dispatchers.IO) {
            p2pState.value = "HOSTING"
            p2pStatusMessage.value = "Servidor Bluetooth iniciado. Esperando conexión..."

            val context = getApplication<Application>().applicationContext
            val bluetoothManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val adapter = bluetoothManager?.adapter ?: android.bluetooth.BluetoothAdapter.getDefaultAdapter()

            if (adapter == null) {
                p2pState.value = "ERROR"
                p2pStatusMessage.value = "Bluetooth no soportado."
                return@launch
            }

            var serverSocket: android.bluetooth.BluetoothServerSocket? = null
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord("MONEY_SYNC", java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                val socket = serverSocket.accept(30000)

                p2pState.value = "SYNCING"
                p2pStatusMessage.value = "Socio Bluetooth conectado. Sincronizando..."

                val reader = java.io.BufferedReader(java.io.InputStreamReader(socket.inputStream, "UTF-8"))
                val jsonStr = reader.readLine() ?: "{}"

                val proposalsCount = processClientPayload(jsonStr)

                val serverPayload = generateServerPayload()
                val writer = java.io.OutputStreamWriter(socket.outputStream, "UTF-8")
                writer.write(serverPayload + "\n")
                writer.flush()

                socket.close()
                p2pState.value = "COMPLETED"
                p2pStatusMessage.value = "¡Sincronización Bluetooth exitosa! Recibiste $proposalsCount propuestas."
            } catch (e: Exception) {
                p2pState.value = "ERROR"
                p2pStatusMessage.value = "Error Bluetooth Server: ${e.message ?: e.localizedMessage}"
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }
    }

    fun connectToBluetoothPeer(device: SimpleBluetoothDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            p2pState.value = "CONNECTING"
            p2pStatusMessage.value = "Conectando por Bluetooth a ${device.name}..."

            val realDev = device.realDevice
            if (realDev == null) {
                p2pState.value = "SYNCING"
                p2pStatusMessage.value = "Sincronizando por Bluetooth simulado..."
                kotlinx.coroutines.delay(1500)
                p2pState.value = "COMPLETED"
                p2pStatusMessage.value = "¡Sincronización Bluetooth simulada completada con éxito!"
                return@launch
            }

            var socket: android.bluetooth.BluetoothSocket? = null
            try {
                socket = realDev.createRfcommSocketToServiceRecord(java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                socket.connect()

                p2pState.value = "SYNCING"
                p2pStatusMessage.value = "Estableciendo sesión. Transfiriendo datos..."

                val proposalsList = repository.propuestasPendientes.first()
                val clientPayload = generateClientPayload(proposalsList)

                val writer = java.io.OutputStreamWriter(socket.outputStream, "UTF-8")
                writer.write(clientPayload + "\n")
                writer.flush()

                p2pStatusMessage.value = "Recibiendo catálogo unificado por Bluetooth..."
                val reader = java.io.BufferedReader(java.io.InputStreamReader(socket.inputStream, "UTF-8"))
                val jsonStr = reader.readLine() ?: "{}"

                val productsImported = processServerPayload(jsonStr)

                socket.close()
                p2pState.value = "COMPLETED"
                p2pStatusMessage.value = "¡Sincronización Bluetooth completada! Importados $productsImported productos."
            } catch (e: Exception) {
                p2pState.value = "ERROR"
                p2pStatusMessage.value = "Fallo de conexión Bluetooth: ${e.message ?: e.localizedMessage}"
            } finally {
                try { socket?.close() } catch (e: Exception) {}
            }
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
            val role = when (appMode.value) {
                AppMode.WORK_EMPLOYER -> "Empleador"
                AppMode.WORK_EMPLOYEE -> "Empleado"
                else -> "Usuario"
            }

            // Fetch current products from database to ensure fresh stock data
            val freshProducts = repository.allProductos.first()
            val processedItems = mutableListOf<Pair<Producto, Int>>()

            currentCart.forEach { (prod, qty) ->
                val dbProd = freshProducts.find { it.id == prod.id }
                val currentStock = dbProd?.stock ?: 0
                if (qty > currentStock) {
                    Log.e("MoneyViewModel", "Conflicto de stock / sobreventa para el producto: ${prod.nombre}. Solicitado: $qty, Disponible: $currentStock")
                    localTransactionError.value = "Sobreventa evitada para ${prod.nombre}. Solicitado: $qty, Disponible en BD: $currentStock. Esta línea fue omitida de la transacción."
                } else {
                    processedItems.add(prod to qty)
                }
            }

            if (processedItems.isEmpty()) {
                clearCart()
                return@launch
            }

            var processedTotal = 0.0
            processedItems.forEach { (prod, qty) ->
                processedTotal += prod.precio * qty
            }

            // Update stocks and write audits
            processedItems.forEach { (prod, qty) ->
                val dbProd = freshProducts.find { it.id == prod.id }!!
                val newStock = dbProd.stock - qty
                repository.updateProducto(dbProd.copy(stock = newStock))
                repository.insertAuditoria(
                    AuditoriaStock(
                        producto_id = prod.id,
                        nombre_producto = prod.nombre,
                        cambio_stock = -qty,
                        stock_anterior = dbProd.stock,
                        stock_resultante = newStock,
                        justificacion = "Venta desde Catálogo",
                        realizado_por = role,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            // Create transaction of type "ingreso"
            val itemNames = processedItems.map { "${it.second}x ${it.first.nombre}" }.joinToString(", ")
            val cal = Calendar.getInstance()
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())

            val isWorkSale = appMode.value == AppMode.WORK_EMPLOYER || appMode.value == AppMode.WORK_EMPLOYEE

            addTransaction(
                monto = processedTotal,
                categoria = "Ventas",
                descripcion = "Venta de Catálogo: $itemNames",
                fecha = cal.timeInMillis,
                hora = format.format(cal.time),
                metodoPago = "Efectivo",
                esEmpleador = isWorkSale, // Work mode sales dynamically
                tipo = "ingreso"
            )

            clearCart()
        }
    }

    fun saveSaleAsEspera() {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return

        viewModelScope.launch {
            val total = cartTotal.value
            val itemNames = currentCart.map { "${it.value}x ${it.key.nombre}" }.joinToString(", ")
            val cal = Calendar.getInstance()
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val isWorkSale = appMode.value == AppMode.WORK_EMPLOYER || appMode.value == AppMode.WORK_EMPLOYEE

            addTransaction(
                monto = total,
                categoria = "Ventas",
                descripcion = "[Espera] Venta: $itemNames",
                fecha = cal.timeInMillis,
                hora = format.format(cal.time),
                metodoPago = "Transfermóvil",
                esEmpleador = isWorkSale,
                tipo = "ingreso"
            )

            clearCart()
        }
    }

    fun confirmEsperaTransaction(tx: Transaction, txId: String) {
        viewModelScope.launch {
            val updatedDesc = tx.descripcion.replace("[Espera]", "[Confirmado]") + " | ID: $txId"
            repository.updateTransaction(tx.copy(descripcion = updatedDesc))

            // Parse and deduct stock
            val productsList = repository.allProductos.first()
            val itemsToDeduct = parseProductsFromDescription(tx.descripcion, productsList)
            val role = when (appMode.value) {
                AppMode.WORK_EMPLOYER -> "Empleador"
                AppMode.WORK_EMPLOYEE -> "Empleado"
                else -> "Usuario"
            }
            itemsToDeduct.forEach { (prod, qty) ->
                val newStock = (prod.stock - qty).coerceAtLeast(0)
                repository.updateProducto(prod.copy(stock = newStock))
                repository.insertAuditoria(
                    AuditoriaStock(
                        producto_id = prod.id,
                        nombre_producto = prod.nombre,
                        cambio_stock = -qty,
                        stock_anterior = prod.stock,
                        stock_resultante = newStock,
                        justificacion = "Confirmación automática de venta en espera",
                        realizado_por = role,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun unconfirmEsperaTransaction(tx: Transaction) {
        viewModelScope.launch {
            val updatedDesc = tx.descripcion.replace("[Confirmado]", "[Espera]").substringBefore(" | ID:")
            repository.updateTransaction(tx.copy(descripcion = updatedDesc))

            // Parse and restore stock
            val productsList = repository.allProductos.first()
            val itemsToRestore = parseProductsFromDescription(tx.descripcion, productsList)
            val role = when (appMode.value) {
                AppMode.WORK_EMPLOYER -> "Empleador"
                AppMode.WORK_EMPLOYEE -> "Empleado"
                else -> "Usuario"
            }
            itemsToRestore.forEach { (prod, qty) ->
                val newStock = prod.stock + qty
                repository.updateProducto(prod.copy(stock = newStock))
                repository.insertAuditoria(
                    AuditoriaStock(
                        producto_id = prod.id,
                        nombre_producto = prod.nombre,
                        cambio_stock = qty,
                        stock_anterior = prod.stock,
                        stock_resultante = newStock,
                        justificacion = "Anulación/Desmarcado manual de venta confirmada",
                        realizado_por = role,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun parseProductsFromDescription(description: String, productsList: List<Producto>): Map<Producto, Int> {
        val result = mutableMapOf<Producto, Int>()
        try {
            val cleanDesc = description.substringAfter("Venta:").substringBefore(" | ID:")
            val items = cleanDesc.split(",").map { it.trim() }
            for (item in items) {
                val qtyMatch = java.util.regex.Pattern.compile("(\\d+)x\\s+(.+)").matcher(item)
                if (qtyMatch.find()) {
                    val qty = qtyMatch.group(1)?.toIntOrNull() ?: 0
                    val name = qtyMatch.group(2) ?: ""
                    val prod = productsList.find { it.nombre.equals(name, ignoreCase = true) }
                    if (prod != null && qty > 0) {
                        result[prod] = qty
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MoneyViewModel", "Error parsing products from desc: ${e.message}")
        }
        return result
    }

    fun selectAmbiguousConfirmation(tx: Transaction) {
        confirmEsperaTransaction(tx, ambiguousParsedId.value)
        ambiguousConfirmations.value = emptyList()
        ambiguousParsedId.value = ""
    }

    fun sendConfirmationSmsToActiveEmployee(transaction: Transaction) {
        viewModelScope.launch {
            val empId = empleadoActivoId.value ?: return@launch
            val employee = activeEmployees.value.find { it.id == empId } ?: return@launch
            val phone = employee.telefono
            if (phone.isBlank()) return@launch

            // Extract ID
            val pattern = java.util.regex.Pattern.compile("(?i)(?:No\\.|Nro\\.|Id Compra|Transaccion|Id|Ref)[:\\s]+([A-Z0-9]+)")
            val matcher = pattern.matcher(transaction.descripcion)
            val txId = if (matcher.find()) matcher.group(1) ?: "" else {
                val fallbackPattern = java.util.regex.Pattern.compile("\\b([A-Z0-9]{8,15})\\b")
                val fallbackMatcher = fallbackPattern.matcher(transaction.descripcion)
                if (fallbackMatcher.find()) fallbackMatcher.group(1) ?: "" else "CONF"
            }

            // Message format: MONEYAPP-CONF|monto:150.00|moneda:CUP|id:MM4004WKVI987|hora:14:32
            val smsText = "MONEYAPP-CONF|monto:${transaction.monto}|moneda:${transaction.moneda}|id:$txId|hora:${transaction.hora}"

            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    getApplication<Application>().getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phone, null, smsText, null, null)
                Log.d("MoneyViewModel", "Sent confirmation SMS to active employee ($phone): $smsText")
            } catch (e: Exception) {
                Log.e("MoneyViewModel", "Error sending SMS: ${e.message}")
            }
        }
    }

    // ==========================================
    // EMPLOYEE MANAGEMENT (Employer mode)
    // ==========================================
    val activeEmployees: StateFlow<List<Empleado>> = repository.allEmpleados
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val empleadoActivoId = MutableStateFlow<Long?>(null)

    fun selectActiveEmployee(employeeId: Long?) {
        viewModelScope.launch {
            empleadoActivoId.value = employeeId
            repository.insertConfiguracion(Configuracion(clave = "empleado_activo_id", valor = employeeId?.toString() ?: ""))
            repository.insertConfiguracion(Configuracion(clave = "empleado_activo_timestamp", valor = System.currentTimeMillis().toString()))
        }
    }

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

    val localTransactionError = MutableStateFlow<String?>(null)

    private val _localTransferError = MutableStateFlow<String?>(null)
    val localTransferError: StateFlow<String?> = _localTransferError.asStateFlow()

    // Configured employer's Transfermóvil number
    val employerTransfermovilNumber = flow {
        val num = repository.getConfiguracionByKey("numero_transfermovil")?.valor.orEmpty()
        emit(num)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun updateTransferPhone(phone: String) {
        _transferPhone.value = phone
        _localTransferError.value = null
    }

    fun updateTransferAmount(amount: String) {
        _transferAmount.value = amount
        _localTransferError.value = null
    }

    fun executeTransfer(onSuccess: () -> Unit, moneda: String = "CUP") {
        val amountVal = _transferAmount.value.toDoubleOrNull() ?: return
        val phoneVal = _transferPhone.value.ifBlank { "Destinatario" }
        val currencyUpper = moneda.uppercase()

        val currencyBalance = availableBalanceMap.value[currencyUpper] ?: 0.0
        if (amountVal > currencyBalance) {
            _localTransferError.value = "Saldo insuficiente para realizar la transferencia en $currencyUpper."
            return
        }

        _localTransferError.value = null

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
                tipo = "gasto",
                moneda = currencyUpper
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

    // --- CUBACEL BALANCE & USSD STATE ---
    val allSaldoMovil: StateFlow<List<SaldoMovil>> = repository.allSaldoMovil
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestSaldoMovil: StateFlow<SaldoMovil?> = allSaldoMovil.map { list ->
        list.firstOrNull { it.tipo == "saldo_principal" || it.tipo == "bono_datos" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activePromociones: StateFlow<List<SaldoMovil>> = repository.allPromociones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ussdStatus = MutableStateFlow("IDLE") // "IDLE", "REQUESTING", "SUCCESS", "ERROR"
    val ussdMessage = MutableStateFlow<String?>(null)

    fun requestUssdBalanceUpdate(ussdCode: String = "*222#") {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val telephonyManager = context.getSystemService(Application.TELEPHONY_SERVICE) as? TelephonyManager

            if (telephonyManager == null) {
                ussdStatus.value = "ERROR"
                ussdMessage.value = "Servicio de telefonía no disponible en este dispositivo."
                return@launch
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                ussdStatus.value = "ERROR"
                ussdMessage.value = "La consulta automática por USSD requiere Android 8.0+."
                return@launch
            }

            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                ussdStatus.value = "ERROR"
                ussdMessage.value = "No se ha concedido el permiso de llamadas (CALL_PHONE) para ejecutar consultas USSD."
                return@launch
            }

            ussdStatus.value = "REQUESTING"
            ussdMessage.value = "Enviando consulta USSD $ussdCode..."

            try {
                telephonyManager.sendUssdRequest(
                    ussdCode,
                    object : TelephonyManager.UssdResponseCallback() {
                        override fun onReceiveUssdResponse(
                            telephonyManager: TelephonyManager?,
                            request: String?,
                            response: CharSequence?
                        ) {
                            val responseText = response?.toString() ?: ""
                            Log.d("USSD", "USSD Response: $responseText")
                            viewModelScope.launch {
                                try {
                                    val parsed = com.example.data.receiver.CubacelMessageParser.parseMessage(responseText, System.currentTimeMillis())
                                    repository.insertSaldoMovil(parsed)
                                    ussdStatus.value = "SUCCESS"
                                    ussdMessage.value = "Saldo actualizado: ${parsed.saldoCUP} CUP, Datos: ${parsed.datosMB} MB"
                                } catch (e: Exception) {
                                    ussdStatus.value = "ERROR"
                                    ussdMessage.value = "Error al procesar respuesta USSD: ${e.message}"
                                }
                            }
                        }

                        override fun onReceiveUssdResponseFailed(
                            telephonyManager: TelephonyManager?,
                            request: String?,
                            failureCode: Int
                        ) {
                            Log.e("USSD", "USSD Failed: $failureCode")
                            ussdStatus.value = "ERROR"
                            ussdMessage.value = when (failureCode) {
                                -1 -> "Error de retorno de red (USSD_RETURN_FAILURE)."
                                -2 -> "Servicio USSD temporalmente no disponible."
                                else -> "Fallo consulta USSD (código $failureCode)."
                            }
                        }
                    },
                    android.os.Handler(android.os.Looper.getMainLooper())
                )
            } catch (e: Exception) {
                ussdStatus.value = "ERROR"
                ussdMessage.value = "Error al ejecutar USSD: ${e.message}"
            }
        }
    }

    fun saveManualSaldo(saldoCUP: Double, datosMB: Double, bonoDatosMB: Double, vencimiento: String) {
        viewModelScope.launch {
            val record = SaldoMovil(
                tipo = "saldo_principal",
                saldoCUP = saldoCUP,
                datosMB = datosMB,
                bonoDatosMB = bonoDatosMB,
                fechaVencimiento = vencimiento.ifBlank { "30 días" },
                descripcion = "Ingreso manual: $saldoCUP CUP | $datosMB MB | $bonoDatosMB MB",
                timestamp = System.currentTimeMillis()
            )
            repository.insertSaldoMovil(record)
        }
    }

    // --- CONSUMO DE DATOS REAL ---
    fun hasUsageStatsPermission(): Boolean {
        val context = getApplication<Application>().applicationContext
        val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    // Expose state flow with the consumed cellular bytes computed dynamically
    val consumedMobileDataBytes: StateFlow<Long> = latestSaldoMovil.map { latest ->
        val context = getApplication<Application>().applicationContext
        if (!hasUsageStatsPermission()) {
            return@map 0L
        }

        // Start measuring from the timestamp when the current package was active/configured
        val startTime = latest?.timestamp ?: (System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)
        val endTime = System.currentTimeMillis()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkStatsManager = context.getSystemService(android.content.Context.NETWORK_STATS_SERVICE) as? android.app.usage.NetworkStatsManager
            if (networkStatsManager != null) {
                try {
                    val bucket = networkStatsManager.querySummaryForDevice(
                        android.net.NetworkCapabilities.TRANSPORT_CELLULAR,
                        null,
                        startTime,
                        endTime
                    )
                    return@map bucket.rxBytes + bucket.txBytes
                } catch (e: Exception) {
                    Log.e("DataUsage", "Error querying device mobile data: ${e.message}")
                }
            }
        }
        0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
            repository.deleteAllProductos()
            repository.deleteAllContactos()
            repository.deleteAllConfiguraciones()
            repository.deleteAllAuditorias()
            repository.deleteAllSaldoMovil()
            // reload configs
            loadConfigurations()
        }
    }
}
