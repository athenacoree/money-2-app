package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.example.ui.components.PeekPreviewType
import com.example.data.model.AppMode
import com.example.data.model.Transaction
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MoneyViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MoneyViewModel,
    onNavigateTab: (NavTab) -> Unit,
    onOpenAddIncome: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val availableBalance by viewModel.availableBalance.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val recentTxList by viewModel.filteredTransactions.collectAsState()
    val appMode by viewModel.appMode.collectAsState()
    val isEmployerModeEnabled by viewModel.isEmployerModeEnabled.collectAsState()
    val isEmployeeModeEnabled by viewModel.isEmployeeModeEnabled.collectAsState()
    val isDistributorModeEnabled by viewModel.isDistributorModeEnabled.collectAsState()
    val trendPercentage by viewModel.trendPercentage.collectAsState()
    val distributionPercentages by viewModel.categoryDistribution.collectAsState()
    val weeklyBars by viewModel.weeklyDailyBars.collectAsState()

    val timeFormatter = rememberFormatter()
    val currencyFormatter = rememberCurrencyFormatter()

    val trendDataPoints = remember(weeklyBars) {
        weeklyBars.map { (inc, exp) -> (inc - exp).toFloat() }
    }

    val categoryColors = mapOf(
        "Comida" to Color(0xFFF97316),
        "Transporte" to Color(0xFFF59E0B),
        "Salario" to Color(0xFF10B981),
        "Ventas" to Color(0xFF34D399),
        "Servicios" to Color(0xFF3B82F6),
        "Educación" to Color(0xFF6366F1),
        "Salud" to Color(0xFFEF4444),
        "Entretenimiento" to Color(0xFF8B5CF6),
        "Ahorros" to Color(0xFFEC4899),
        "Otros" to Color(0xFF6B7280)
    )

    val donutCategories = remember(distributionPercentages) {
        if (distributionPercentages.isEmpty()) {
            listOf(ExpenseCategoryItem("Sin Gastos", 0.0, Color(0xFF6B7280)))
        } else {
            distributionPercentages.map { (cat, pct) ->
                ExpenseCategoryItem(cat, pct, categoryColors[cat] ?: Color(0xFF7C3AED))
            }
        }
    }

    BackgroundGradientCanvas(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Top Status & Header Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_header_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        Text(
                            text = sdfTime.format(Date()),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val welcomeName = if (userProfile.name.isNotBlank()) userProfile.name.trim().split(" ").first() else "Usuario"
                        Text(
                            text = "Bienvenido, $welcomeName",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 22.sp
                            ),
                            modifier = Modifier.testTag("welcome_user_text")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x1F7C3AED))
                                .clickable { onOpenChat() }
                                .padding(10.dp)
                                .testTag("chat_shortcut_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "Chat",
                                tint = PurplePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (appMode == AppMode.WORK_EMPLOYER || appMode == AppMode.WORK_EMPLOYEE) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0x1F7C3AED))
                                    .clickable { onOpenCatalog() }
                                    .padding(10.dp)
                                    .testTag("catalog_shortcut_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = "Catálogo",
                                    tint = PurplePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Mode Switcher Bar (Personal vs Empleador vs Empleado vs Distribuidor según configuración)
            item {
                val availableModes = remember(isEmployerModeEnabled, isEmployeeModeEnabled, isDistributorModeEnabled) {
                    val list = mutableListOf(com.example.data.model.AppMode.PERSONAL to "Personal")
                    if (isEmployerModeEnabled) list.add(com.example.data.model.AppMode.WORK_EMPLOYER to "Empleador")
                    if (isEmployeeModeEnabled) list.add(com.example.data.model.AppMode.WORK_EMPLOYEE to "Empleado")
                    if (isDistributorModeEnabled) list.add(com.example.data.model.AppMode.WORK_DISTRIBUTOR to "Distribuidor")
                    list
                }

                if (availableModes.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1F7C3AED))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableModes.forEach { (mode, label) ->
                            val isSelected = appMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PurplePrimary else Color.Transparent)
                                    .clickable { viewModel.setAppMode(mode) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // TARJETA DE SALDO PRINCIPAL (CON MINI GRÁFICA DE TENDENCIA 7 DÍAS)
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_balance_card"),
                    cornerRadius = 24.dp,
                    backgroundColor = Color(0x267C3AED),
                    borderColor = Color(0x4D7C3AED),
                    elevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x40FFFFFF), Color(0x00FFFFFF)),
                                        center = Offset(0f, 0f),
                                        radius = size.width * 0.45f
                                    ),
                                    center = Offset(0f, 0f),
                                    radius = size.width * 0.45f
                                )
                            }
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (appMode == AppMode.PERSONAL) "Saldo personal disponible" else "Saldo del negocio",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x337C3AED))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "AURA Card",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = PurplePrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        AnimatedBalanceCounter(targetAmount = availableBalance)

                        Spacer(modifier = Modifier.height(10.dp))

                        // GRÁFICA DE TENDENCIA EN LA TARJETA DE SALDO
                        SparklineChart(
                            dataPoints = if (trendDataPoints.isEmpty()) listOf(0f) else trendDataPoints,
                            isPositive = !trendPercentage.startsWith("-"),
                            percentageChange = trendPercentage
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Quick Buttons (+ Ingreso / - Gasto)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(IncomeGreen.copy(alpha = 0.2f))
                                    .clickable { onOpenAddIncome() }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Agregar Ingreso", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ExpenseRed.copy(alpha = 0.2f))
                                    .clickable { onOpenAddExpense() }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Agregar Gasto", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Cuban Motivational Phrase sutil display
            item {
                val currentPhrase by viewModel.currentMotivationalPhrase.collectAsState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0F7C3AED))
                        .border(0.5.dp, Color(0x267C3AED), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🇨🇺",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = "\"$currentPhrase\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // GRID 2x2 DE ACCESO RÁPIDO
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("quick_access_grid")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 20.dp,
                            onClick = onOpenAddExpense,
                            testTag = "quick_card_expenses"
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Gastos", tint = ExpenseRed, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "Control de Gastos", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontSize = 12.sp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "-${currencyFormatter.format(totalExpense)}", style = MaterialTheme.typography.titleMedium.copy(color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 15.sp))
                            }
                        }

                        GlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 20.dp,
                            onClick = { onNavigateTab(NavTab.Transfer) },
                            testTag = "quick_card_transfer"
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PurplePrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Transferencias", tint = PurplePrimary, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "Transferencias", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontSize = 12.sp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Enviar dinero", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 20.dp,
                            onClick = onOpenAddIncome,
                            testTag = "quick_card_income"
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Saldo Actual", tint = IncomeGreen, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "Ingresos Totales", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontSize = 12.sp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "+${currencyFormatter.format(totalIncome)}", style = MaterialTheme.typography.titleMedium.copy(color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp))
                            }
                        }

                        GlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 20.dp,
                            onClick = { onNavigateTab(NavTab.History) },
                            testTag = "quick_card_history"
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PurpleSecondary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.History, contentDescription = "Historial", tint = PurplePrimary, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "Historial", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontSize = 12.sp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Ver registros", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp))
                            }
                        }
                    }
                }
            }

            // GRÁFICA DE DISTRIBUCIÓN DE GASTOS (DONUT CHART)
            item {
                DonutChart(categories = donutCategories)
            }

            // SECCIÓN ACTIVIDAD RECIENTE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Actividad Reciente",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Ver todos",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable { onNavigateTab(NavTab.History) }
                            .padding(4.dp)
                    )
                }
            }

            items(recentTxList.take(5)) { tx ->
                TransactionItemCard(
                    transaction = tx,
                    timeFormatter = timeFormatter,
                    currencyFormatter = currencyFormatter,
                    onClick = { viewModel.selectedTransactionForDetail.value = tx },
                    onLongClick = { viewModel.openPeekPreview(PeekPreviewType.TransactionDetail(tx)) }
                )
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItemCard(
    transaction: Transaction,
    timeFormatter: SimpleDateFormat,
    currencyFormatter: NumberFormat,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val isIncome = transaction.tipo == "ingreso"
    val accentColor = if (isIncome) IncomeGreen else ExpenseRed

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("home_tx_item_${transaction.id}"),
        cornerRadius = 18.dp,
        backgroundColor = Color(0xF5FFFFFF)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transaction.categoria.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.categoria,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.descripcion.ifBlank { "Sin descripción" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${currencyFormatter.format(transaction.monto)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.hora,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun rememberFormatter(): SimpleDateFormat {
    return SimpleDateFormat("hh:mm a", Locale.getDefault())
}

@Composable
private fun rememberCurrencyFormatter(): NumberFormat {
    return NumberFormat.getCurrencyInstance(Locale.US)
}
