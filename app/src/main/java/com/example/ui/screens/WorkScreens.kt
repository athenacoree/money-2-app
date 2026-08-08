package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Empleado
import com.example.data.model.Producto
import com.example.data.model.PropuestaCambio
import com.example.ui.components.BackgroundGradientCanvas
import com.example.ui.components.BarChart
import com.example.ui.components.DailyBarData
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.QRCodeCanvas
import com.example.ui.theme.*
import com.example.ui.viewmodel.EmployerInfo
import com.example.ui.viewmodel.MoneyViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.example.data.model.DespachoDistribuidor
import com.example.ui.components.IPhonePeekPreviewDialog
import com.example.ui.components.PeekPreviewType
import java.util.Locale

@Composable
fun EmployerScreen(
    viewModel: MoneyViewModel,
    onOpenCatalog: () -> Unit,
    onBackToPersonal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeEmployees by viewModel.activeEmployees.collectAsState()
    val weeklyBars by viewModel.weeklyDailyBars.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val balance by viewModel.availableBalance.collectAsState()

    val propuestasPendientes by viewModel.propuestasPendientes.collectAsState()
    val selectedPropuesta by viewModel.selectedPropuestaForReview.collectAsState()
    val showSyncDialog by viewModel.showSyncP2PDialog.collectAsState()
    val syncMessage by viewModel.syncP2PMessage.collectAsState()
    val companyBranches by viewModel.companyBranches.collectAsState()
    val empleadoActivoId by viewModel.empleadoActivoId.collectAsState()

    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showAddBranchDialog by remember { mutableStateOf(false) }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    EmployerDashboardScreen(
        employees = activeEmployees,
        propuestas = propuestasPendientes,
        branches = companyBranches,
        incomeToday = totalIncome,
        expenseToday = totalExpense,
        balanceToday = balance,
        weeklyBars = weeklyBars,
        empleadoActivoId = empleadoActivoId,
        onSelectActiveEmployee = { viewModel.selectActiveEmployee(it) },
        onAddEmployeeClick = { showAddEmployeeDialog = true },
        onAddBranchClick = { showAddBranchDialog = true },
        onOpenCatalog = onOpenCatalog,
        onOpenSyncP2P = { viewModel.showSyncP2PDialog.value = true },
        onSelectPropuesta = { viewModel.selectedPropuestaForReview.value = it },
        onBackToPersonal = onBackToPersonal,
        currencyFormatter = currencyFormatter,
        modifier = modifier
    )

    // Employer SMS Confirmation Dialog (Problem B)
    val pendingEmployerConfirmationTx by viewModel.pendingEmployerConfirmationTx.collectAsState()
    pendingEmployerConfirmationTx?.let { tx ->
        val activeEmp = activeEmployees.find { it.id == empleadoActivoId }
        val empName = activeEmp?.nombre

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
                        text = "Detalles de la transacción: ${tx.monto} ${tx.moneda} (Hora: ${tx.hora})",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (empName != null) {
                        Text(
                            text = "¿Deseas enviar un SMS automático de confirmación a $empName sobre este pago?",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    } else {
                        Text(
                            text = "ATENCIÓN: No hay ningún empleado activo seleccionado. Debes elegir un empleado para poder enviarle la confirmación SMS.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.pendingEmployerConfirmationTx.value = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Omitir", color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }

                        GlassButton(
                            text = "Enviar confirmación",
                            isPrimary = true,
                            enabled = empName != null,
                            onClick = {
                                viewModel.sendConfirmationSmsToActiveEmployee(tx)
                                viewModel.pendingEmployerConfirmationTx.value = null
                            },
                            modifier = Modifier.weight(1.8f)
                        )
                    }
                }
            }
        }
    }

    if (showAddBranchDialog) {
        var branchName by remember { mutableStateOf("") }
        var branchAddress by remember { mutableStateOf("") }
        var branchManager by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddBranchDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = PurplePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar Nueva Rama / Sucursal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it },
                        label = { Text("Nombre de la Rama (e.g. Sucursal Playa)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = branchAddress,
                        onValueChange = { branchAddress = it },
                        label = { Text("Dirección / Ubicación") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = branchManager,
                        onValueChange = { branchManager = it },
                        label = { Text("Encargado / Responsable") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    GlassButton(
                        text = "Vincular a la Principal",
                        icon = Icons.Default.Lock,
                        isPrimary = true,
                        onClick = {
                            if (branchName.isNotBlank()) {
                                viewModel.addCompanyBranch(
                                    name = branchName,
                                    address = branchAddress.ifBlank { "Sede Secundaría" },
                                    managerName = branchManager.ifBlank { "Encargado" }
                                )
                                showAddBranchDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showAddBranchDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }

    if (showSyncDialog) {
        SyncP2PDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showSyncP2PDialog.value = false }
        )
    }

    if (selectedPropuesta != null) {
        ReviewProposalDialog(
            propuesta = selectedPropuesta!!,
            onDismiss = { viewModel.selectedPropuestaForReview.value = null },
            onApproveAndMerge = { nombre, precio, stock, notas ->
                viewModel.approveAndMergeProposal(selectedPropuesta!!, nombre, precio, stock, notas)
            },
            onReject = { motivo ->
                viewModel.rejectProposal(selectedPropuesta!!, motivo)
            }
        )
    }

    if (showAddEmployeeDialog) {
        var empName by remember { mutableStateOf("") }
        var empPhone by remember { mutableStateOf("") }
        var empPhotoUri by remember { mutableStateOf<String?>(null) }

        val employeePhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { empPhotoUri = it.toString() }
        }

        Dialog(onDismissRequest = { showAddEmployeeDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Agregar Empleado", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x1F7C3AED))
                                .border(1.5.dp, PurplePrimary, CircleShape)
                                .clickable { employeePhotoPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (empPhotoUri != null) {
                                AsyncImage(
                                    model = empPhotoUri,
                                    contentDescription = "Foto del trabajador",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Foto", tint = PurplePrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        TextButton(onClick = { employeePhotoPicker.launch("image/*") }) {
                            Text(if (empPhotoUri != null) "Cambiar foto" else "Cargar foto del trabajador", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = empName,
                        onValueChange = { empName = it },
                        label = { Text("Nombre del empleado") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = empPhone,
                        onValueChange = { empPhone = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    GlassButton(
                        text = "Agregar",
                        isPrimary = true,
                        onClick = {
                            if (empName.isNotBlank() && empPhone.isNotBlank()) {
                                viewModel.addEmployee(empName, empPhone, empPhotoUri)
                                showAddEmployeeDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_employee_btn")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showAddEmployeeDialog = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeScreen(
    viewModel: MoneyViewModel,
    onOpenCatalog: () -> Unit,
    onBackToPersonal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIncome by viewModel.totalIncome.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val employerInfo by viewModel.employerInfo.collectAsState()
    val showSyncDialog by viewModel.showSyncP2PDialog.collectAsState()
    val syncMessage by viewModel.syncP2PMessage.collectAsState()

    EmployeeDashboardScreen(
        employeePhone = userProfile.phone,
        employerInfo = employerInfo,
        salesToday = totalIncome,
        onOpenCatalog = onOpenCatalog,
        onOpenSyncP2P = { viewModel.showSyncP2PDialog.value = true },
        onOpenSalesHistory = { /* Sales history */ },
        onBackToPersonal = onBackToPersonal,
        modifier = modifier
    )

    if (showSyncDialog) {
        SyncP2PDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showSyncP2PDialog.value = false }
        )
    }

    // Employee Ambiguous Waits Duplicate selection alert (Problem C)
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

                    LazyColumn(
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
                                    Text("Monto: ${tx.monto} ${tx.moneda} | Hora: ${tx.hora}", fontSize = 11.sp, color = TextSecondary)
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

// -------------------------------------------------------------
// VISTA DE EMPLEADOR
// -------------------------------------------------------------
@Composable
fun EmployerDashboardScreen(
    employees: List<Empleado>,
    propuestas: List<PropuestaCambio>,
    branches: List<com.example.data.model.BranchInfo> = emptyList(),
    incomeToday: Double,
    expenseToday: Double,
    balanceToday: Double,
    weeklyBars: List<Pair<Double, Double>>,
    empleadoActivoId: Long? = null,
    onSelectActiveEmployee: (Long?) -> Unit = {},
    onAddEmployeeClick: () -> Unit,
    onAddBranchClick: () -> Unit = {},
    onOpenCatalog: () -> Unit,
    onOpenSyncP2P: () -> Unit,
    onSelectPropuesta: (PropuestaCambio) -> Unit,
    onBackToPersonal: () -> Unit = {},
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val weekdays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val salesChartData = remember(weeklyBars) {
        weeklyBars.mapIndexed { index, (inc, exp) ->
            DailyBarData(weekdays[index % 7], inc, exp)
        }
    }

    BackgroundGradientCanvas(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(20.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackToPersonal) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Panel Empleador",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = TextPrimary
                            ),
                            modifier = Modifier.testTag("employer_dashboard_title")
                        )
                    }

                    IconButton(onClick = onOpenSyncP2P) {
                        Icon(imageVector = Icons.Default.WifiTethering, contentDescription = "Vincular App", tint = PurplePrimary)
                    }
                }
            }

            // Tarjeta de Resumen
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
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Resumen del Negocio",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Ventas de hoy", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    text = currencyFormatter.format(incomeToday),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = IncomeGreen
                                )
                            }

                            Column {
                                Text(text = "Gastos de hoy", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    text = currencyFormatter.format(expenseToday),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = ExpenseRed
                                )
                            }

                            Column {
                                Text(text = "Balance general", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    text = currencyFormatter.format(balanceToday),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = PurplePrimary
                                )
                            }
                        }
                    }
                }
            }

            // SECCIÓN: EMPLEADO ACTIVO / DE TURNO
            item {
                var expanded by remember { mutableStateOf(false) }
                val activeEmployee = employees.find { it.id == empleadoActivoId }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    backgroundColor = Color(0xF5FFFFFF),
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = PurplePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Empleado de Turno Activo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                            }

                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text(
                                        text = activeEmployee?.nombre ?: "Seleccionar...",
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimary
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PurplePrimary)
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Ninguno / Sin empleado") },
                                        onClick = {
                                            onSelectActiveEmployee(null)
                                            expanded = false
                                        }
                                    )
                                    employees.forEach { emp ->
                                        DropdownMenuItem(
                                            text = { Text(emp.nombre) },
                                            onClick = {
                                                onSelectActiveEmployee(emp.id)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (activeEmployee != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Este empleado recibirá las notificaciones automáticas de todas las transferencias entrantes detectadas.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No hay ningún empleado seleccionado. Las transferencias entrantes no generarán alertas para ningún empleado.",
                                fontSize = 11.sp,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }

            // SECCIÓN: RAMA DE REVISIÓN (PROPUESTAS DE EMPLEADOS)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    backgroundColor = Color(0xF5FFFFFF),
                    elevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MergeType, contentDescription = null, tint = PurplePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Rama de Revisión (Propuestas)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                            }

                            if (propuestas.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(PurplePrimary)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${propuestas.size}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (propuestas.isEmpty()) {
                            Text(
                                text = "Sin propuestas pendientes. El inventario está al día con la rama principal.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                propuestas.forEach { prop ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x0F7C3AED))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prop.nombre_producto,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Por: ${prop.empleado_nombre} | Propone Stock: ${prop.stock_propuesto} u. @ $${prop.precio_propuesto}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Button(
                                            onClick = { onSelectPropuesta(prop) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Revisar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN: RAMAS Y SUCURSALES DE LA EMPRESA
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    backgroundColor = Color(0xF5FFFFFF),
                    elevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountTree, contentDescription = null, tint = PurplePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ramas & Sucursales",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                            }

                            IconButton(onClick = onAddBranchClick) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar Rama", tint = PurplePrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            branches.forEach { branch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (branch.isMain) Color(0x1F7C3AED) else Color(0x0F7C3AED))
                                        .border(
                                            width = if (branch.isMain) 1.dp else 0.5.dp,
                                            color = if (branch.isMain) PurplePrimary else Color(0x337C3AED),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = branch.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            if (branch.isMain) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(PurplePrimary)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Principal", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${branch.address} | Responsable: ${branch.managerName}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Icon(
                                        imageVector = if (branch.isMain) Icons.Default.Verified else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (branch.isMain) PurplePrimary else IncomeGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Gráfica de Barras de Ventas (Últimos 7 días)
            item {
                BarChart(
                    dailyData = salesChartData,
                    percentageComparison = "Ventas e ingresos totales del negocio"
                )
            }

            // Lista de Empleados Activos
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Empleados Activos",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        )

                        IconButton(onClick = onAddEmployeeClick) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Agregar", tint = PurplePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (employees.isEmpty()) {
                        Text("No hay empleados activos en este momento.", color = TextSecondary)
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(employees) { emp ->
                                GlassCard(
                                    cornerRadius = 18.dp,
                                    backgroundColor = Color(0xF5FFFFFF)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, PurplePrimary, CircleShape)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(PurplePrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!emp.foto_uri.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = emp.foto_uri,
                                                    contentDescription = "Foto de ${emp.nombre}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(
                                                    text = emp.nombre.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = emp.nombre.split(" ").first(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (emp.estado == "activo") IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (emp.estado == "activo") "Activo" else "Inactivo",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (emp.estado == "activo") IncomeGreen else ExpenseRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        text = "Vincular App (Wi-Fi / Bluetooth)",
                        icon = Icons.Default.WifiTethering,
                        isPrimary = false,
                        onClick = onOpenSyncP2P,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("employer_p2p_sync_btn")
                    )

                    GlassButton(
                        text = "Catálogo de Productos",
                        icon = Icons.Default.Storefront,
                        isPrimary = true,
                        onClick = onOpenCatalog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("employer_catalog_btn")
                    )

                    GlassButton(
                        text = "Agregar empleado",
                        icon = Icons.Default.PersonAdd,
                        isPrimary = false,
                        onClick = onAddEmployeeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_employee_btn")
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }
}

// -------------------------------------------------------------
// VISTA DE EMPLEADO
// -------------------------------------------------------------
@Composable
fun EmployeeDashboardScreen(
    employeePhone: String,
    employerInfo: EmployerInfo = EmployerInfo(),
    salesToday: Double,
    onOpenCatalog: () -> Unit,
    onOpenSyncP2P: () -> Unit,
    onOpenSalesHistory: () -> Unit,
    onBackToPersonal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    BackgroundGradientCanvas(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(20.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackToPersonal) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Panel de Empleado",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = TextPrimary
                            ),
                            modifier = Modifier.testTag("employee_dashboard_title")
                        )
                    }

                    IconButton(onClick = onOpenSyncP2P) {
                        Icon(imageVector = Icons.Default.WifiTethering, contentDescription = "Vincular App", tint = PurplePrimary)
                    }
                }
            }

            // Tarjeta "Tu Empleador" (Muestra la foto del empleador al empleado)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    backgroundColor = Color(0xF5FFFFFF),
                    elevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, PurplePrimary, CircleShape)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(Color(0x1F7C3AED)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = employerInfo.photoUri ?: R.drawable.img_profile_avatar,
                                contentDescription = "Foto de tu Empleador",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Tu Empleador",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = PurplePrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(IncomeGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Sincronizado", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = employerInfo.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                            )

                            Text(
                                text = employerInfo.phone,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        IconButton(onClick = onOpenSyncP2P) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = "Sincronizar", tint = PurplePrimary)
                        }
                    }
                }
            }

            // Tarjeta de Resumen
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    backgroundColor = Color(0xF5FFFFFF),
                    elevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = currencyFormatter.format(salesToday), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = PurplePrimary)
                            Text(text = "Ventas hoy", fontSize = 11.sp, color = TextSecondary)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val commission = salesToday * 0.10
                            Text(text = currencyFormatter.format(commission), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = IncomeGreen)
                            Text(text = "Comisiones (10%)", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Código QR Grande en el Centro (200x200dp)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp,
                    backgroundColor = Color(0xF5FFFFFF),
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Cobro Rápido (Transfermóvil)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // QR Code dynamic content: the employee's phone number
                        val qrContent = remember(employeePhone) {
                            org.json.JSONObject().apply {
                                put("tipo", "cobro")
                                put("destino", employeePhone)
                                put("empleadoId", "")
                                put("nota", "Cobro de Empleado")
                            }.toString()
                        }
                        QRCodeCanvas(content = qrContent, sizeDp = 200)

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Escanear para pagar: $employeePhone",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Action Buttons
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text = "Vincular App (Wi-Fi / Bluetooth)",
                        icon = Icons.Default.WifiTethering,
                        isPrimary = false,
                        onClick = onOpenSyncP2P,
                        modifier = Modifier.fillMaxWidth().testTag("employee_p2p_sync_btn")
                    )

                    GlassButton(
                        text = "Ventas y Catálogo",
                        icon = Icons.Default.Store,
                        isPrimary = true,
                        onClick = onOpenCatalog,
                        modifier = Modifier.fillMaxWidth().testTag("employee_open_catalog_btn")
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }
}

// -------------------------------------------------------------
// DIÁLOGOS DE REVISIÓN P2P Y DE RAMAS
// -------------------------------------------------------------
@Composable
fun SyncP2PDialog(
    viewModel: MoneyViewModel,
    onDismiss: () -> Unit
) {
    val p2pState by viewModel.p2pState.collectAsState()
    val p2pIpAddress by viewModel.p2pIpAddress.collectAsState()
    val p2pStatusMessage by viewModel.p2pStatusMessage.collectAsState()
    val appMode by viewModel.appMode.collectAsState()

    val myIp = remember { viewModel.getLocalIpAddress() }
    var targetIpInput by remember { mutableStateOf("192.168.1.105") }

    // Problem D tabs: WiFi Directo / Bluetooth
    var activePairingTab by remember { mutableStateOf("wifi") } // "wifi" or "bluetooth"

    // Peer lists from ViewModel
    val wifiP2pDevices by viewModel.wifiP2pDevices.collectAsState()
    val bluetoothDevices by viewModel.bluetoothDevices.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            cornerRadius = 24.dp,
            backgroundColor = Color(0xF5FFFFFF)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WifiTethering,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Vincular App P2P",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Transmite de verdad catálogo, empleados e inventario en tiempo real localmente.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de método (WiFi Directo / Bluetooth)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0F7C3AED))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activePairingTab == "wifi") PurplePrimary else Color.Transparent)
                            .clickable { activePairingTab = "wifi" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "WiFi Directo",
                            color = if (activePairingTab == "wifi") Color.White else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activePairingTab == "bluetooth") PurplePrimary else Color.Transparent)
                            .clickable { activePairingTab = "bluetooth" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bluetooth",
                            color = if (activePairingTab == "bluetooth") Color.White else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TAB 1: WIFI DIRECTO FLOW
                if (activePairingTab == "wifi") {
                    if (appMode == com.example.data.model.AppMode.WORK_EMPLOYER) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1F7C3AED))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Socio Principal (Servidor WiFi Directo)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                                Text("Tu IP: $myIp", fontSize = 12.sp, color = TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassButton(
                                text = "Buscar pares",
                                icon = Icons.Default.Search,
                                isPrimary = false,
                                onClick = { viewModel.startWifiP2pDiscovery() },
                                modifier = Modifier.weight(1f)
                            )
                            GlassButton(
                                text = "Iniciar Servidor",
                                icon = Icons.Default.PlayArrow,
                                isPrimary = true,
                                onClick = { viewModel.startP2PSyncServer() },
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1F7C3AED))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Sucursal (Cliente WiFi Directo)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                                Text("Tu IP: $myIp", fontSize = 12.sp, color = TextPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        GlassButton(
                            text = "Buscar Socio Principal",
                            icon = Icons.Default.Search,
                            isPrimary = true,
                            onClick = { viewModel.startWifiP2pDiscovery() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (wifiP2pDevices.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Selecciona dispositivo para conectar:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.align(Alignment.Start))
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(modifier = Modifier.heightIn(max = 120.dp).fillMaxWidth()) {
                                items(wifiP2pDevices) { dev ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x0A000000))
                                            .clickable { viewModel.connectToWifiDirectPeer(dev) }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(dev.deviceName.ifBlank { "Dispositivo sin nombre" }, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(dev.deviceAddress, fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("--- O introduce IP a mano ---", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = targetIpInput,
                            onValueChange = { targetIpInput = it },
                            label = { Text("IP del Socio Principal") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        GlassButton(
                            text = "Conectar por IP",
                            icon = Icons.Default.Sync,
                            isPrimary = false,
                            onClick = { viewModel.connectToP2PServer(targetIpInput.trim()) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // TAB 2: BLUETOOTH FLOW
                if (activePairingTab == "bluetooth") {
                    if (appMode == com.example.data.model.AppMode.WORK_EMPLOYER) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1F7C3AED))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Socio Principal (Servidor Bluetooth)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                                Text("Estado: Esperando RFCOMM de Sucursal", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        GlassButton(
                            text = "Iniciar Servidor Bluetooth",
                            icon = Icons.Default.PlayArrow,
                            isPrimary = true,
                            onClick = { viewModel.startBluetoothSyncServer() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1F7C3AED))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Sucursal (Cliente Bluetooth)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                                Text("Estado: Busca y pulsa para sincronizar", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        GlassButton(
                            text = "Escanear Socios Bluetooth",
                            icon = Icons.Default.Search,
                            isPrimary = true,
                            onClick = { viewModel.startBluetoothDiscovery() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (bluetoothDevices.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Dispositivos Bluetooth encontrados:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.align(Alignment.Start))
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(modifier = Modifier.heightIn(max = 140.dp).fillMaxWidth()) {
                                items(bluetoothDevices) { dev ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0x0A000000))
                                            .clickable { viewModel.connectToBluetoothPeer(dev) }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(dev.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(dev.address, fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mensajes de estado en tiempo real (Sincronizando, Completado, Buscando, etc.)
                if (p2pStatusMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (p2pState == "COMPLETED") IncomeGreen.copy(alpha = 0.15f)
                                else if (p2pState == "ERROR") ExpenseRed.copy(alpha = 0.15f)
                                else Color(0x0F7C3AED)
                            )
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (p2pState == "SYNCING" || p2pState == "CONNECTING" || p2pState == "SEARCHING") {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PurplePrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = p2pStatusMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = if (p2pState == "COMPLETED") IncomeGreen else if (p2pState == "ERROR") ExpenseRed else TextPrimary,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
fun ReviewProposalDialog(
    propuesta: PropuestaCambio,
    onDismiss: () -> Unit,
    onApproveAndMerge: (nombreAjustado: String, precioAjustado: Double, stockAjustado: Int, notas: String) -> Unit,
    onReject: (motivo: String) -> Unit
) {
    var editNombre by remember { mutableStateOf(propuesta.nombre_producto) }
    var editPrecio by remember { mutableStateOf(propuesta.precio_propuesto.toString()) }
    var editStock by remember { mutableStateOf(propuesta.stock_propuesto.toString()) }
    var notasEmpleador by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            cornerRadius = 24.dp,
            backgroundColor = Color(0xF5FFFFFF)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Rule, contentDescription = null, tint = PurplePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Revisar Rama de Empleado",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Empleado: ${propuesta.empleado_nombre}",
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    fontSize = 13.sp
                )
                Text(
                    text = "Justificación del empleado: \"${propuesta.justificacion}\"",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Ajustes del Empleador antes de Fusionar:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editNombre,
                    onValueChange = { editNombre = it },
                    label = { Text("Nombre del Producto") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editPrecio,
                        onValueChange = { editPrecio = it },
                        label = { Text("Precio ($)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editStock,
                        onValueChange = { editStock = it },
                        label = { Text("Stock Final") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notasEmpleador,
                    onValueChange = { notasEmpleador = it },
                    label = { Text("Notas de Auditoría (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                GlassButton(
                    text = "Aprobar y Fusionar a Rama Principal",
                    icon = Icons.Default.MergeType,
                    isPrimary = true,
                    onClick = {
                        val p = editPrecio.toDoubleOrNull() ?: propuesta.precio_propuesto
                        val s = editStock.toIntOrNull() ?: propuesta.stock_propuesto
                        onApproveAndMerge(editNombre, p, s, notasEmpleador)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("approve_proposal_btn")
                )

                Spacer(modifier = Modifier.height(8.dp))

                GlassButton(
                    text = "Rechazar Propuesta",
                    icon = Icons.Default.Close,
                    isPrimary = false,
                    onClick = { onReject("Rechazado por el empleador") },
                    modifier = Modifier.fillMaxWidth().testTag("reject_proposal_btn")
                )

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DistributorDashboardScreen(
    viewModel: MoneyViewModel,
    onBackToPersonal: () -> Unit
) {
    val despachos by viewModel.despachosDistribuidor.collectAsState()
    var showAddDispatchDialog by remember { mutableStateOf(false) }

    var destNombre by remember { mutableStateOf("") }
    var prodNombre by remember { mutableStateOf("") }
    var cantidadText by remember { mutableStateOf("") }
    var precioUnitarioText by remember { mutableStateOf("") }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val totalDespachadoVal = despachos.sumOf { d -> d.cantidadUnidades * d.precioPorUnidad }
    val totalUnidadesVal = despachos.sumOf { d -> d.cantidadUnidades }

    BackgroundGradientCanvas {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PurplePrimary)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("MODO DISTRIBUIDOR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Módulo Proveedor / Mercancía", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    }

                    TextButton(onClick = onBackToPersonal) {
                        Text("Modo Personal", fontWeight = FontWeight.Bold, color = PurplePrimary)
                    }
                }
            }

            // Stats Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    backgroundColor = Color(0xF5FFFFFF)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(currencyFormat.format(totalDespachadoVal), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PurplePrimary)
                            Text("Total Mercancía Envíada", fontSize = 11.sp, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalUnidadesVal u.", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = IncomeGreen)
                            Text("Unidades Despachadas", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Actions: Vincular & Registrar Despacho
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassButton(
                        text = "Vincular Tienda P2P",
                        icon = Icons.Default.QrCode,
                        isPrimary = false,
                        onClick = {
                            viewModel.openPeekPreview(
                                PeekPreviewType.FunctionInfo(
                                    title = "Vincular Distribuidor P2P",
                                    description = "Permite conectar tu terminal de Distribuidor con la tienda del Empleador o Empleado vía QR o Wi-Fi Direct para transferir lotes de mercancía automáticamente.",
                                    icon = Icons.Default.QrCode,
                                    tips = listOf(
                                        "Genera un código QR único con los lotes enviados",
                                        "El Empleador puede aceptar la carga de stock al instante",
                                        "Sincronización en tiempo real sin necesidad de internet"
                                    )
                                )
                            )
                        },
                        modifier = Modifier.weight(1f).testTag("distributor_link_p2p_btn")
                    )

                    GlassButton(
                        text = "Nuevo Despacho",
                        icon = Icons.Default.LocalShipping,
                        isPrimary = true,
                        onClick = { showAddDispatchDialog = true },
                        modifier = Modifier.weight(1f).testTag("distributor_add_dispatch_btn")
                    )
                }
            }

            // Title
            item {
                Text(
                    text = "Guías de Despacho Registradas",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
            }

            if (despachos.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        backgroundColor = Color(0xF5FFFFFF)
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No hay despachos registrados aún. Presiona 'Nuevo Despacho' para enviar mercancía.", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(despachos) { item ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    viewModel.openPeekPreview(
                                        PeekPreviewType.FunctionInfo(
                                            title = "Guía: ${item.productoNombre}",
                                            description = "Despacho asignado a ${item.destinatario}.\nTotal: ${currencyFormat.format(item.cantidadUnidades * item.precioPorUnidad)} (${item.cantidadUnidades} unidades a ${currencyFormat.format(item.precioPorUnidad)} c/u).\nEstado actual: ${item.estado}.",
                                            icon = Icons.Default.LocalShipping,
                                            tips = listOf(
                                                "Sincronizado con el inventario del negocio",
                                                "Estado verificado por firma digital P2P",
                                                "Fecha de despacho: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(java.util.Date(item.timestamp))}"
                                            )
                                        )
                                    )
                                }
                            ),
                        cornerRadius = 16.dp,
                        backgroundColor = Color(0xF5FFFFFF)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1F7C3AED)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PurplePrimary)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productoNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text("Destino: ${item.destinatario}", fontSize = 12.sp, color = TextSecondary)
                                Text("${item.cantidadUnidades} u. x ${currencyFormat.format(item.precioPorUnidad)}", fontSize = 11.sp, color = PurplePrimary)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    currencyFormat.format(item.cantidadUnidades * item.precioPorUnidad),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (item.estado == "Entregado") IncomeGreen.copy(alpha = 0.15f) else Color(0x1F7C3AED))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(item.estado, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (item.estado == "Entregado") IncomeGreen else PurplePrimary)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }

    if (showAddDispatchDialog) {
        Dialog(onDismissRequest = { showAddDispatchDialog = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Registrar Despacho de Mercancía", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = destNombre,
                        onValueChange = { destNombre = it },
                        label = { Text("Destinatario (Empleador / Empleado)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = prodNombre,
                        onValueChange = { prodNombre = it },
                        label = { Text("Nombre del Producto / Lote") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cantidadText,
                            onValueChange = { cantidadText = it },
                            label = { Text("Cantidad (Unidades)") },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = precioUnitarioText,
                            onValueChange = { precioUnitarioText = it },
                            label = { Text("Precio Unitario ($)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    GlassButton(
                        text = "Guardar y Despachar",
                        isPrimary = true,
                        onClick = {
                            val cant = cantidadText.toIntOrNull() ?: 0
                            val prec = precioUnitarioText.toDoubleOrNull() ?: 0.0
                            if (destNombre.isNotBlank() && prodNombre.isNotBlank() && cant > 0) {
                                viewModel.addDespachoDistribuidor(destNombre, prodNombre, cant, prec)
                                showAddDispatchDialog = false
                                destNombre = ""
                                prodNombre = ""
                                cantidadText = ""
                                precioUnitarioText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
