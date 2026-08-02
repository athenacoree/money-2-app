package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MoneyViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom geometric stick-figure Dove/Bird Icon derived from a '+' cross:
 * 1. Takes the '+' cross geometry
 * 2. Removes the top vertical line
 * 3. Angles/bends the left and right horizontal lines upwards (\ /) into wings
 * 4. Keeps the bottom vertical line as the tail (|)
 * 5. Adds a subtle beak/head accent at the top center vertex
 */
@Composable
fun DoveBirdIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidthDp: Dp = 3.dp
) {
    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidthDp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f + size.height * 0.08f
        val radius = minOf(size.width, size.height) * 0.38f

        val strokeStyle = Stroke(
            width = strokeWidthPx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        val path = Path().apply {
            // Left wing: bent & curved upwards from center junction (\)
            moveTo(cx - radius * 0.95f, cy - radius * 0.65f)
            quadraticTo(
                cx - radius * 0.5f, cy - radius * 0.15f,
                cx, cy
            )

            // Right wing: bent & curved upwards from center junction (/)
            quadraticTo(
                cx + radius * 0.5f, cy - radius * 0.15f,
                cx + radius * 0.95f, cy - radius * 0.65f
            )

            // Tail: bottom vertical stick from original cross (|)
            moveTo(cx, cy)
            lineTo(cx, cy + radius * 0.85f)

            // Head/beak accent at top junction
            moveTo(cx, cy)
            lineTo(cx + radius * 0.22f, cy - radius * 0.28f)
        }

        drawPath(
            path = path,
            color = color,
            style = strokeStyle
        )
    }
}

/**
 * Floating Action Button featuring the custom Dove/Bird icon
 */
@Composable
fun DoveBirdSearchFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                spotColor = Color(0x667C3AED),
                ambientColor = Color(0x33000000)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        PurplePrimary,
                        PurpleDark,
                        Color(0xFF4C1D95)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = CircleShape
            )
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("dove_search_fab"),
        contentAlignment = Alignment.Center
    ) {
        DoveBirdIcon(
            modifier = Modifier.size(28.dp),
            color = Color.White,
            strokeWidthDp = 3.2.dp
        )
    }
}

enum class SearchCategory(val label: String, val icon: ImageVector) {
    ALL("Todos", Icons.Default.Search),
    TRANSACTIONS("Transacciones", Icons.Default.Receipt),
    PRODUCTS("Productos", Icons.Default.Store),
    EMPLOYEES("Empleados", Icons.Default.People),
    STATS("Porcentajes & %", Icons.Default.PieChart),
    SHORTCUTS("Accesos & Modos", Icons.Default.FlashOn)
}

sealed class SearchResultItem {
    data class TransactionResult(val tx: Transaction) : SearchResultItem()
    data class ProductResult(val product: Producto) : SearchResultItem()
    data class EmployeeResult(val employee: Empleado) : SearchResultItem()
    data class StatResult(
        val title: String,
        val value: String,
        val subtitle: String,
        val percentage: Double,
        val color: Color
    ) : SearchResultItem()
    data class ShortcutResult(
        val title: String,
        val description: String,
        val icon: ImageVector,
        val action: () -> Unit
    ) : SearchResultItem()
}

@Composable
fun GlobalSearchDialog(
    viewModel: MoneyViewModel,
    onDismiss: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SearchCategory.ALL) }

    val transactions by viewModel.allTransactions.collectAsState()
    val products by viewModel.products.collectAsState()
    val employees by viewModel.activeEmployees.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val availableBalance by viewModel.availableBalance.collectAsState()
    val trendPercentage by viewModel.trendPercentage.collectAsState()
    val categoryDistribution by viewModel.categoryDistribution.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val queryClean = searchQuery.trim().lowercase(Locale.getDefault())

    // Generate search results dynamically
    val results = remember(queryClean, selectedCategory, transactions, products, employees, totalIncome, totalExpense, categoryDistribution) {
        val list = mutableListOf<SearchResultItem>()

        // 1. Transactions
        if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.TRANSACTIONS) {
            val matchedTx = transactions.filter { tx ->
                queryClean.isEmpty() ||
                        tx.descripcion.lowercase().contains(queryClean) ||
                        tx.categoria.lowercase().contains(queryClean) ||
                        tx.tipo.lowercase().contains(queryClean) ||
                        tx.metodo_pago.lowercase().contains(queryClean) ||
                        tx.monto.toString().contains(queryClean)
            }.take(15)
            matchedTx.forEach { list.add(SearchResultItem.TransactionResult(it)) }
        }

        // 2. Products
        if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.PRODUCTS) {
            val matchedProd = products.filter { prod ->
                queryClean.isEmpty() ||
                        prod.nombre.lowercase().contains(queryClean) ||
                        prod.precio.toString().contains(queryClean) ||
                        prod.stock.toString().contains(queryClean)
            }.take(15)
            matchedProd.forEach { list.add(SearchResultItem.ProductResult(it)) }
        }

        // 3. Employees
        if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.EMPLOYEES) {
            val matchedEmp = employees.filter { emp ->
                queryClean.isEmpty() ||
                        emp.nombre.lowercase().contains(queryClean) ||
                        emp.estado.lowercase().contains(queryClean) ||
                        emp.telefono.lowercase().contains(queryClean)
            }.take(10)
            matchedEmp.forEach { list.add(SearchResultItem.EmployeeResult(it)) }
        }

        // 4. Stats & Percentages
        if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.STATS ||
            queryClean.contains("porcenta") || queryClean.contains("%") || queryClean.contains("estadist") ||
            queryClean.contains("gasto") || queryClean.contains("ingreso") || queryClean.contains("balance")) {

            val ratio = if (totalIncome > 0) (totalExpense / totalIncome * 100.0) else 0.0
            list.add(
                SearchResultItem.StatResult(
                    title = "% Relativo Gastos vs Ingresos",
                    value = String.format(Locale.US, "%.1f%%", ratio),
                    subtitle = "Gastos (${currencyFormatter.format(totalExpense)}) / Ingresos (${currencyFormatter.format(totalIncome)})",
                    percentage = ratio.coerceIn(0.0, 100.0),
                    color = ExpenseRed
                )
            )

            list.add(
                SearchResultItem.StatResult(
                    title = "Tendencia de Crecimiento Global",
                    value = trendPercentage,
                    subtitle = "Variación de flujo financiero en el período activo",
                    percentage = 75.0,
                    color = PurplePrimary
                )
            )

            val lowStockCount = products.count { it.stock < 5 }
            val totalProd = products.size
            val lowStockPct = if (totalProd > 0) (lowStockCount.toDouble() / totalProd * 100.0) else 0.0
            list.add(
                SearchResultItem.StatResult(
                    title = "% Productos con Stock Bajo (<5)",
                    value = String.format(Locale.US, "%.1f%% (%d/%d)", lowStockPct, lowStockCount, totalProd),
                    subtitle = "Alertas de inventario que requieren reposición",
                    percentage = lowStockPct.coerceIn(0.0, 100.0),
                    color = IncomeGreen
                )
            )

            // Category breakdown percentages
            categoryDistribution.forEach { (cat, amount) ->
                val catPct = if (totalExpense > 0) (amount / totalExpense * 100.0) else 0.0
                if (queryClean.isEmpty() || cat.lowercase().contains(queryClean) || queryClean.contains("porcenta")) {
                    list.add(
                        SearchResultItem.StatResult(
                            title = "% Distribución: $cat",
                            value = String.format(Locale.US, "%.1f%%", catPct),
                            subtitle = "Monto asignado: ${currencyFormatter.format(amount)}",
                            percentage = catPct.coerceIn(0.0, 100.0),
                            color = PurpleDark
                        )
                    )
                }
            }
        }

        // 5. Shortcuts & Navigation
        if (selectedCategory == SearchCategory.ALL || selectedCategory == SearchCategory.SHORTCUTS || queryClean.isNotEmpty()) {
            val shortcuts = listOf(
                SearchResultItem.ShortcutResult(
                    title = "Catálogo & Inventario",
                    description = "Ver lista completa de productos, editar o ajustar stock",
                    icon = Icons.Default.Store,
                    action = {
                        onDismiss()
                        onNavigateToCatalog()
                    }
                ),
                SearchResultItem.ShortcutResult(
                    title = "Histórico de Transacciones",
                    description = "Consultar estados de cuenta, reportes y gráficos de barra",
                    icon = Icons.Default.History,
                    action = {
                        onDismiss()
                        onNavigateToHistory()
                    }
                ),
                SearchResultItem.ShortcutResult(
                    title = "Envío & Transferencia P2P",
                    description = "Enviar fondos o realizar transferencias directas a contactos",
                    icon = Icons.Default.SwapHoriz,
                    action = {
                        onDismiss()
                        onNavigateToTransfer()
                    }
                ),
                SearchResultItem.ShortcutResult(
                    title = "Perfil & Configuración de Modos",
                    description = "Activar Modo Empleador, Modo Empleado o Modo Distribuidor",
                    icon = Icons.Default.Person,
                    action = {
                        onDismiss()
                        onNavigateToProfile()
                    }
                ),
                SearchResultItem.ShortcutResult(
                    title = "Activar Modo Empleador",
                    description = "Cambiar directamente la consola al panel de control de negocio",
                    icon = Icons.Default.Business,
                    action = {
                        viewModel.setAppMode(AppMode.WORK_EMPLOYER)
                        onDismiss()
                    }
                ),
                SearchResultItem.ShortcutResult(
                    title = "Activar Modo Distribuidor",
                    description = "Ir al panel de despacho de mercancía y gestión de insumos",
                    icon = Icons.Default.LocalShipping,
                    action = {
                        viewModel.setAppMode(AppMode.WORK_DISTRIBUTOR)
                        onDismiss()
                    }
                )
            )

            shortcuts.filter { sc ->
                queryClean.isEmpty() ||
                        sc.title.lowercase().contains(queryClean) ||
                        sc.description.lowercase().contains(queryClean)
            }.forEach { list.add(it) }
        }

        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .testTag("global_search_dialog"),
                cornerRadius = 28.dp,
                backgroundColor = Color(0xF7FFFFFF)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header with custom Dove Icon and Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PurplePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                DoveBirdIcon(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidthDp = 2.8.dp
                                )
                            }

                            Column {
                                Text(
                                    text = "Buscador Universal",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Transacciones, catálogo, empleados, % y funciones",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x1A000000))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search input field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("global_search_input"),
                        placeholder = {
                            Text(
                                "Buscar transacciones, productos, empleados, % ...",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = PurplePrimary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xF0F8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Category Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SearchCategory.values()) { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isSelected) Color.White else TextSecondary
                                        )
                                        Text(
                                            text = category.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurplePrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0x147C3AED),
                                    labelColor = TextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = GlassCardBorder,
                                    selectedBorderColor = PurplePrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Results List
                    if (results.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "No se encontraron resultados para '$searchQuery'",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(results) { item ->
                                when (item) {
                                    is SearchResultItem.TransactionResult -> {
                                        val tx = item.tx
                                        val isIncome = tx.tipo == "ingreso"
                                        val color = if (isIncome) IncomeGreen else ExpenseRed

                                        GlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.selectedTransactionForDetail.value = tx
                                                    onDismiss()
                                                },
                                            cornerRadius = 16.dp,
                                            backgroundColor = Color.White
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(color.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                            contentDescription = null,
                                                            tint = color,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    Column {
                                                        Text(
                                                            text = tx.descripcion.ifBlank { tx.categoria },
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = TextPrimary
                                                        )
                                                        Text(
                                                            text = "${tx.categoria} • ${dateFormatter.format(Date(tx.fecha))} • ${tx.metodo_pago}",
                                                            fontSize = 11.sp,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "${if (isIncome) "+" else "-"}${currencyFormatter.format(tx.monto)}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = color,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }

                                    is SearchResultItem.ProductResult -> {
                                        val product = item.product
                                        GlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onDismiss()
                                                    onNavigateToCatalog()
                                                },
                                            cornerRadius = 16.dp,
                                            backgroundColor = Color.White
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(PurplePrimary.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ShoppingBag,
                                                            contentDescription = null,
                                                            tint = PurplePrimary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    Column {
                                                        Text(
                                                            text = product.nombre,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = TextPrimary
                                                        )
                                                        Text(
                                                            text = "Stock: ${product.stock} unidades",
                                                            fontSize = 11.sp,
                                                            color = if (product.stock < 5) ExpenseRed else TextSecondary
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = currencyFormatter.format(product.precio),
                                                    fontWeight = FontWeight.Bold,
                                                    color = PurplePrimary,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }

                                    is SearchResultItem.EmployeeResult -> {
                                        val emp = item.employee
                                        GlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onDismiss()
                                                    onNavigateToProfile()
                                                },
                                            cornerRadius = 16.dp,
                                            backgroundColor = Color.White
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF0284C7).copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = Color(0xFF0284C7),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = emp.nombre,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = "Estado: ${emp.estado.replaceFirstChar { it.uppercase() }} • Tel: ${emp.telefono.ifBlank { "Sin registro" }}",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }

                                                Text(
                                                    text = emp.estado.replaceFirstChar { it.uppercase() },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (emp.estado == "activo") IncomeGreen else TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    is SearchResultItem.StatResult -> {
                                        GlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            cornerRadius = 16.dp,
                                            backgroundColor = Color.White
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = item.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = item.value,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = item.color
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = item.subtitle,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                LinearProgressIndicator(
                                                    progress = (item.percentage / 100.0).toFloat().coerceIn(0f, 1f),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = item.color,
                                                    trackColor = item.color.copy(alpha = 0.15f)
                                                )
                                            }
                                        }
                                    }

                                    is SearchResultItem.ShortcutResult -> {
                                        GlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { item.action() },
                                            cornerRadius = 16.dp,
                                            backgroundColor = Color.White
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(PurplePrimary.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = null,
                                                        tint = PurplePrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = item.description,
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
