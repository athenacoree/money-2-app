package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.BackgroundGradientCanvas
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.data.qvapay.QvaPayTransaction
import com.example.data.qvapay.QvaPayInvoice
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MoneyViewModel
import java.util.Locale

enum class TransferTabType {
    QVAPAY_P2P,
    QVAPAY_CRYPTO,
    QVAPAY_HISTORY,
    QVAPAY_MERCHANT,
    TRANSFERMOVIL
}

@Composable
fun TransferScreen(
    viewModel: MoneyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // QvaPay ViewModel States
    val qvaPayUserInfo by viewModel.qvaPayUserInfo.collectAsState()
    val qvaPayCoins by viewModel.qvaPayCoins.collectAsState()
    val qvaPayTransactions by viewModel.qvaPayTransactions.collectAsState()
    val qvaPayInvoices by viewModel.qvaPayInvoices.collectAsState()
    val isQvaPayLoading by viewModel.isQvaPayLoading.collectAsState()
    val qvaPayError by viewModel.qvaPayError.collectAsState()
    val qvaPaySuccessMessage by viewModel.qvaPaySuccessMessage.collectAsState()
    val isQvaPayOfflineCache by viewModel.isQvaPayOfflineCache.collectAsState()
    val qvaPayCacheTimestamp by viewModel.qvaPayCacheTimestamp.collectAsState()

    // Local Transfermóvil States
    val phone by viewModel.transferPhone.collectAsState()
    val amount by viewModel.transferAmount.collectAsState()
    val contacts by viewModel.conversations.collectAsState()
    val employerNumber by viewModel.employerTransfermovilNumber.collectAsState()
    val etecsaBalance by viewModel.etecsaMobileBalance.collectAsState()

    var activeTab by remember { mutableStateOf(TransferTabType.QVAPAY_P2P) }
    var showSyncUssdDialog by remember { mutableStateOf(false) }

    // QvaPay form states
    var targetQvaPayUser by remember { mutableStateOf("") }
    var qvaPayAmountText by remember { mutableStateOf("") }
    var qvaPayDescription by remember { mutableStateOf("") }

    // Crypto market search & calculator
    var cryptoSearchQuery by remember { mutableStateOf("") }
    var calcSqpAmount by remember { mutableStateOf("100") }

    // Merchant invoice form states
    var invoiceAmountText by remember { mutableStateOf("") }
    var invoiceConceptText by remember { mutableStateOf("") }

    // History filter
    var historyFilter by remember { mutableStateOf("Todos") }

    var showManualPaymentDialog by remember { mutableStateOf(false) }
    val quickSqpAmounts = listOf("5", "10", "20", "50", "100")
    val quickCupAmounts = listOf("100", "200", "500", "1000", "2000")

    // Refresh QvaPay on initial screen enter
    LaunchedEffect(Unit) {
        viewModel.refreshQvaPayData()
    }

    BackgroundGradientCanvas(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Header Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "QvaPay & Transferencias",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = TextPrimary
                            ),
                            modifier = Modifier.testTag("transfer_screen_title")
                        )
                        Text(
                            text = "Ecosistema Financiero Oficial QvaPay API & Local",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    // Botón para actualizar datos manualmente
                    Button(
                        onClick = { viewModel.refreshQvaPayData() },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isQvaPayLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Actualizar Datos",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actualizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // QvaPay Real Wallet Banner with Offline Cache Status Badge
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    backgroundColor = Color(0xF5FFFFFF),
                    elevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PurplePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = qvaPayUserInfo?.name?.take(2)?.uppercase() ?: "QP",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = qvaPayUserInfo?.name ?: "Mi Cuenta QvaPay",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (qvaPayUserInfo != null) "@${qvaPayUserInfo!!.username}" else "API Conectada",
                                        fontSize = 12.sp,
                                        color = PurplePrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (isQvaPayOfflineCache) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1FF59E0B))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color(0xD9D97706), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "CACHE OFFLINE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xD9D97706)
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1F10B981))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "LIVE API",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Saldo Disponible QvaPay",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${qvaPayUserInfo?.balance ?: 0.00} SQP",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary,
                                        fontSize = 28.sp
                                    )
                                )
                            }

                            if (qvaPayCacheTimestamp.isNotBlank()) {
                                Text(
                                    text = "Actualizado: $qvaPayCacheTimestamp",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Sub-navigation Tabs Selector (Scrollable horizontally)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        TransferTabType.QVAPAY_P2P to "Transferir SQP",
                        TransferTabType.QVAPAY_CRYPTO to "Cripto & Mercado",
                        TransferTabType.QVAPAY_HISTORY to "Historial",
                        TransferTabType.QVAPAY_MERCHANT to "Facturas & Cobros",
                        TransferTabType.TRANSFERMOVIL to "Transfermóvil"
                    )

                    items(tabs) { (type, label) ->
                        val isSelected = activeTab == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) PurplePrimary else Color(0x1F7C3AED))
                                .clickable { activeTab = type }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }

            // TAB 1: QVAPAY REAL P2P TRANSFER
            if (activeTab == TransferTabType.QVAPAY_P2P) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        backgroundColor = Color(0xF5FFFFFF),
                        elevation = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = PurplePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enviar Fondos SQP a Cualquier Usuario",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = targetQvaPayUser,
                                onValueChange = { targetQvaPayUser = it },
                                label = { Text("Nombre de usuario QvaPay de destino") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("qvapay_target_user"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = qvaPayAmountText,
                                onValueChange = { qvaPayAmountText = it },
                                label = { Text("Monto a enviar (SQP)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("qvapay_amount_input"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick SQP chips
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                quickSqpAmounts.forEach { qAmt ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x1F7C3AED))
                                            .clickable { qvaPayAmountText = qAmt }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "$qAmt SQP",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = PurplePrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = qvaPayDescription,
                                onValueChange = { qvaPayDescription = it },
                                label = { Text("Concepto / Detalles (Opcional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            GlassButton(
                                text = if (isQvaPayLoading) "Procesando en QvaPay..." else "Enviar con Autenticación",
                                icon = Icons.Default.Lock,
                                onClick = {
                                    val amt = qvaPayAmountText.toDoubleOrNull() ?: 0.0
                                    viewModel.executeQvaPayTransfer(targetQvaPayUser, amt, qvaPayDescription)
                                },
                                enabled = !isQvaPayLoading && targetQvaPayUser.isNotBlank() && (qvaPayAmountText.toDoubleOrNull() ?: 0.0) > 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("execute_qvapay_transfer_btn")
                            )

                            if (qvaPayError != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = qvaPayError!!,
                                    color = ExpenseRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (qvaPaySuccessMessage != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x1F10B981))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = qvaPaySuccessMessage!!,
                                        color = IncomeGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: QVAPAY CRYPTO & COINS MARKET + COMPARISON CHART & CALCULATOR
            if (activeTab == TransferTabType.QVAPAY_CRYPTO) {
                // Search field
                item {
                    OutlinedTextField(
                        value = cryptoSearchQuery,
                        onValueChange = { cryptoSearchQuery = it },
                        placeholder = { Text("Buscar criptomoneda (Bitcoin, USDT, TRX, LTC)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PurplePrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary)
                    )
                }

                // Calculated Conversion Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        backgroundColor = Color(0xF5FFFFFF)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = PurplePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Calculadora de Comisiones QvaPay",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = calcSqpAmount,
                                onValueChange = { calcSqpAmount = it },
                                label = { Text("Monto a convertir (SQP)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val inputAmt = calcSqpAmount.toDoubleOrNull() ?: 0.0
                            Text(
                                text = "Monto Neto Estimado tras comisión de la red:",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tether USDT (TRC20 - 1% fee):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${String.format(Locale.US, "%.2f", inputAmt * 0.99)} USDT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bitcoin (BTC - 1.5% fee):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${String.format(Locale.US, "%.2f", inputAmt * 0.985)} SQP equiv.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                            }
                        }
                    }
                }

                // Fee Comparison Visual Bar Chart
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp,
                        backgroundColor = Color(0xF5FFFFFF)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Comparativa Visual de Comisiones (%)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            val filteredCoins = qvaPayCoins.filter {
                                it.name.contains(cryptoSearchQuery, ignoreCase = true) ||
                                        it.coin.contains(cryptoSearchQuery, ignoreCase = true)
                            }

                            if (filteredCoins.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    filteredCoins.take(6).forEach { coin ->
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "${coin.name} (${coin.coin})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                Text(text = "${coin.feePercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0x1F7C3AED))
                                            ) {
                                                val maxFee = 3.0
                                                val fraction = (coin.feePercent / maxFee).coerceIn(0.1, 1.0).toFloat()
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(fraction)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(PurplePrimary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Coins List
                item {
                    Text(
                        text = "Todas las Criptomonedas Soportadas",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                val filteredList = qvaPayCoins.filter {
                    it.name.contains(cryptoSearchQuery, ignoreCase = true) ||
                            it.coin.contains(cryptoSearchQuery, ignoreCase = true)
                }

                if (filteredList.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            backgroundColor = Color(0xF5FFFFFF)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No se encontraron criptomonedas con ese filtro.", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(filteredList) { coin ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            backgroundColor = Color(0xF5FFFFFF)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x1F7C3AED)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CurrencyBitcoin,
                                            contentDescription = null,
                                            tint = PurplePrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = coin.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Símbolo: ${coin.coin}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Comisión: ${coin.feePercent}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimary
                                    )
                                    Text(
                                        text = "Min: ${coin.min} • Máx: ${coin.max.toInt()}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TAB 3: QVAPAY TRANSACTION HISTORY
            if (activeTab == TransferTabType.QVAPAY_HISTORY) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp,
                        backgroundColor = Color(0xF5FFFFFF)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = PurplePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Historial Completo de Movimientos QvaPay",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Filter Chips
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Todos", "Pagados", "Recibidos", "Depósitos").forEach { filterOpt ->
                                    val isSel = historyFilter == filterOpt
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) PurplePrimary else Color(0x1F7C3AED))
                                            .clickable { historyFilter = filterOpt }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = filterOpt,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val filteredHistory = qvaPayTransactions.filter { tx ->
                    when (historyFilter) {
                        "Pagados" -> tx.type.contains("Paid", ignoreCase = true)
                        "Recibidos" -> tx.type.contains("Received", ignoreCase = true)
                        "Depósitos" -> tx.type.contains("Deposit", ignoreCase = true)
                        else -> true
                    }
                }

                if (filteredHistory.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            backgroundColor = Color(0xF5FFFFFF)
                        ) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No hay registros en esta categoría.", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(filteredHistory) { tx ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            backgroundColor = Color(0xF5FFFFFF)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (tx.type.contains("Received") || tx.type.contains("Deposit")) Color(0x1F10B981) else Color(0x1FEF4444)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (tx.type.contains("Received") || tx.type.contains("Deposit")) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (tx.type.contains("Received") || tx.type.contains("Deposit")) IncomeGreen else ExpenseRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = tx.description,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Usuario: @${tx.remoteUser} • ${tx.dateStr}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (tx.type.contains("Paid")) "-" else "+"}${tx.amount} SQP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (tx.type.contains("Paid")) ExpenseRed else IncomeGreen
                                    )
                                    Text(
                                        text = tx.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TAB 4: QVAPAY MERCHANT / GENERATE INVOICES & COBROS
            if (activeTab == TransferTabType.QVAPAY_MERCHANT) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        backgroundColor = Color(0xF5FFFFFF),
                        elevation = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = PurplePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generar Factura & Enlace de Cobro QvaPay",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = invoiceAmountText,
                                onValueChange = { invoiceAmountText = it },
                                label = { Text("Monto a cobrar (SQP)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = invoiceConceptText,
                                onValueChange = { invoiceConceptText = it },
                                label = { Text("Concepto del cobro (ej. Pago de producto)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            GlassButton(
                                text = "Generar Enlace de Cobro",
                                icon = Icons.Default.Link,
                                isPrimary = true,
                                onClick = {
                                    val amt = invoiceAmountText.toDoubleOrNull() ?: 0.0
                                    if (amt > 0) {
                                        viewModel.createQvaPayInvoice(amt, invoiceConceptText.ifBlank { "Cobro Negocio" })
                                        invoiceAmountText = ""
                                        invoiceConceptText = ""
                                    }
                                },
                                enabled = (invoiceAmountText.toDoubleOrNull() ?: 0.0) > 0,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Mis Facturas Generadas",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                if (qvaPayInvoices.isEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            backgroundColor = Color(0xF5FFFFFF)
                        ) {
                            Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Aún no has generado facturas de cobro en esta sesión.", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(qvaPayInvoices) { inv ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            backgroundColor = Color(0xF5FFFFFF)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.QrCode, contentDescription = null, tint = PurplePrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(inv.description, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("ID: ${inv.invoiceId}", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                    Text("${inv.amount} SQP", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = IncomeGreen)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = inv.url,
                                        fontSize = 11.sp,
                                        color = PurplePrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("QvaPay Invoice URL", inv.url)
                                            clipboard.setPrimaryClip(clip)
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar Enlace", tint = PurplePrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 5: TRANSFERMÓVIL (PAGO LOCAL)
            if (activeTab == TransferTabType.TRANSFERMOVIL) {
                // Quick Contact Selector
                item {
                    Column {
                        Text(
                            text = "Contactos Recientes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (contacts.isEmpty()) {
                            Text("No hay contactos recientes.", color = TextSecondary, fontSize = 12.sp)
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.testTag("contacts_row")
                            ) {
                                items(contacts) { contact ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { viewModel.updateTransferPhone(contact.telefono) }
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    try {
                                                        Color(android.graphics.Color.parseColor(contact.avatarColorHex))
                                                    } catch (e: Exception) {
                                                        PurplePrimary
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = contact.nombre.take(2).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = contact.nombre.split(" ").first(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Monitor Oficial de Saldo & Recursos ETECSA (*222#)
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        backgroundColor = Color(0xF5FFFFFF),
                        borderWidth = 1.dp,
                        borderColor = Color(0xFF10B981).copy(alpha = 0.4f),
                        elevation = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🇨🇺", fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Saldo Móvil & Recursos (*222#)",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Text(
                                            text = "ETECSA Cuba • Vence: ${etecsaBalance.fechaVencimiento}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                }

                                IconButton(onClick = { showSyncUssdDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sincronizar USSD",
                                        tint = PurplePrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Main CUP Balance Display
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFECFDF5))
                                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "SALDO PRINCIPAL MÓVIL",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF065F46),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        )
                                        Text(
                                            text = String.format(Locale.US, "$%.2f CUP", etecsaBalance.saldoCup),
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = IncomeGreen
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:*222#"))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Marcar *222#", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Desglose de Paquetes / Bonos (Megas, Minutos, SMS)
                            Text(
                                text = "Paquetes & Bonos Activos",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Datos GB / MB
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(14.dp))
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:*222*328#"))
                                            context.startActivity(intent)
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("🌐 Datos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E40AF))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (etecsaBalance.datosMb >= 1024) String.format(Locale.US, "%.1f GB", etecsaBalance.datosMb / 1024.0) else "${etecsaBalance.datosMb.toInt()} MB",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D4ED8)
                                        )
                                        Text("*222*328#", fontSize = 9.5.sp, color = TextSecondary)
                                    }
                                }

                                // Minutos
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFDF4FF))
                                        .border(1.dp, Color(0xFFF5D0FE), RoundedCornerShape(14.dp))
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:*222*869#"))
                                            context.startActivity(intent)
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("📞 Minutos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF86198F))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${etecsaBalance.minutosVoz} Min",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFA21CAF)
                                        )
                                        Text("*222*869#", fontSize = 9.5.sp, color = TextSecondary)
                                    }
                                }

                                // SMS
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFFF7ED))
                                        .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(14.dp))
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:*222*767#"))
                                            context.startActivity(intent)
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("💬 SMS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF9A3412))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${etecsaBalance.mensajesSms} SMS",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC2410C)
                                        )
                                        Text("*222*767#", fontSize = 9.5.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // Transfermóvil Info
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        backgroundColor = Color(0x337C3AED)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Transfermóvil del Empleador",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Número de Transfermóvil configurado: $employerNumber",
                                fontSize = 14.sp,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PurplePrimary.copy(alpha = 0.15f))
                                        .clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Numero Empleador", employerNumber)
                                            clipboard.setPrimaryClip(clip)
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copiar número", color = PurplePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PurplePrimary)
                                        .clickable { showManualPaymentDialog = true }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Ya pagué", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Transfermóvil Form
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        backgroundColor = Color(0xF5FFFFFF),
                        elevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Datos de Transferencia Local (CUP)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { viewModel.updateTransferPhone(it) },
                                label = { Text("Número de teléfono de destino") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = amount,
                                onValueChange = { viewModel.updateTransferAmount(it) },
                                label = { Text("Monto a enviar (CUP)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                quickCupAmounts.forEach { qAmt ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x1F7C3AED))
                                            .clickable { viewModel.updateTransferAmount(qAmt) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "$qAmt CUP",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = PurplePrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            GlassButton(
                                text = "Enviar por Transfermóvil",
                                icon = Icons.Default.Send,
                                onClick = { viewModel.executeTransfer(onSuccess = {}) },
                                enabled = phone.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }

    if (showManualPaymentDialog) {
        var manualAmount by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showManualPaymentDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Registrar Pago Manual", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Ingresa el monto que transferiste para registrarlo en el negocio.", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = manualAmount,
                        onValueChange = { manualAmount = it },
                        label = { Text("Monto pagado (CUP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    GlassButton(
                        text = "Registrar Pago",
                        isPrimary = true,
                        onClick = {
                            val amt = manualAmount.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                                viewModel.registerEmployerManualPayment(amt)
                                showManualPaymentDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showManualPaymentDialog = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }

    if (showSyncUssdDialog) {
        var ussdInputText by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showSyncUssdDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Sincronizar Respuesta USSD ETECSA",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pega o escribe el mensaje recibido tras marcar *222#, *222*328# (Datos), *222*869# (Voz) o *222*767# (SMS) para actualizar la app en tiempo real.",
                        color = TextSecondary,
                        fontSize = 12.5.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = ussdInputText,
                        onValueChange = { ussdInputText = it },
                        placeholder = { Text("Ej: Saldo: 250.50 CUP. Valido hasta 30/11/2026. Datos: 4.5 GB, 45 Min, 120 SMS") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    GlassButton(
                        text = "Procesar y Sincronizar Saldo",
                        isPrimary = true,
                        onClick = {
                            if (ussdInputText.isNotBlank()) {
                                viewModel.parseAndProcessUssdText(ussdInputText)
                            }
                            showSyncUssdDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showSyncUssdDialog = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}
