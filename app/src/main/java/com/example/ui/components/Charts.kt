package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

// -------------------------------------------------------------
// 1. SPARKLINE LINE CHART (7-DAY TREND)
// -------------------------------------------------------------
@Composable
fun SparklineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    isPositive: Boolean = true,
    percentageChange: String = "+12.5%",
    onClick: (() -> Unit)? = null
) {
    val lineProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints) {
        lineProgress.animateTo(1f, animationSpec = tween(1000))
    }

    val lineColor = if (isPositive) IncomeGreen else ExpenseRed
    val gradientColors = if (isPositive) {
        listOf(IncomeGreen.copy(alpha = 0.35f), IncomeGreen.copy(alpha = 0.0f))
    } else {
        listOf(ExpenseRed.copy(alpha = 0.35f), ExpenseRed.copy(alpha = 0.0f))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Line Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (dataPoints.size < 2) return@Canvas

                val width = size.width
                val height = size.height
                val minVal = dataPoints.minOrNull() ?: 0f
                val maxVal = dataPoints.maxOrNull() ?: 1f
                val range = (maxVal - minVal).coerceAtLeast(1f)

                val points = dataPoints.mapIndexed { index, value ->
                    val x = index * (width / (dataPoints.size - 1))
                    val y = height - ((value - minVal) / range * (height - 12.dp.toPx()) + 6.dp.toPx())
                    Offset(x, y)
                }

                // Smooth cubic path
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val cx = (p1.x + p2.x) / 2f
                        cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                    }
                }

                // Fill path
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(points.last().x * lineProgress.value, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(gradientColors)
                )

                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // End point glow dot
                val lastPoint = points.last()
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = lastPoint)
                drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = lastPoint)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Percentage Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(lineColor.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = percentageChange,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = lineColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )
        }
    }
}

// -------------------------------------------------------------
// 2. DONUT CHART (EXPENSE DISTRIBUTION)
// -------------------------------------------------------------
data class ExpenseCategoryItem(
    val categoryName: String,
    val amount: Double,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutChart(
    categories: List<ExpenseCategoryItem>,
    modifier: Modifier = Modifier,
    onCategorySelected: (ExpenseCategoryItem?) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<ExpenseCategoryItem?>(null) }
    val totalAmount = categories.sumOf { it.amount }.coerceAtLeast(1.0)
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(categories) {
        animProgress.animateTo(1f, tween(1000))
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("donut_chart_card"),
        cornerRadius = 24.dp,
        backgroundColor = Color(0xF5FFFFFF),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Distribución de Gastos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                )

                if (selectedCategory != null) {
                    Text(
                        text = "Limpiar filtro",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable {
                                selectedCategory = null
                                onCategorySelected(null)
                            }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Donut Ring Canvas with Center Label
            Box(
                modifier = Modifier.size(170.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(categories) {
                            detectTapGestures { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                var currentAngle = 0f
                                for (item in categories) {
                                    val sweep = (item.amount / totalAmount * 360f).toFloat()
                                    if (angle >= currentAngle && angle <= currentAngle + sweep) {
                                        selectedCategory = if (selectedCategory == item) null else item
                                        onCategorySelected(selectedCategory)
                                        break
                                    }
                                    currentAngle += sweep
                                }
                            }
                        }
                ) {
                    val strokeWidth = 28.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)

                    var startAngle = -90f
                    categories.forEach { item ->
                        val sweepAngle = ((item.amount / totalAmount) * 360f * animProgress.value).toFloat()
                        val isSelected = selectedCategory == item
                        val currentStroke = if (isSelected) strokeWidth + 8.dp.toPx() else strokeWidth

                        drawArc(
                            color = item.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - 2f, // gap
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = currentStroke, cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }

                // Center Text Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selectedCategory?.categoryName ?: "Total Gastos",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currencyFormatter.format(selectedCategory?.amount ?: totalAmount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = selectedCategory?.color ?: TextPrimary,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Categories Legend Wrap Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val percentage = String.format(Locale.US, "%.1f%%", (cat.amount / totalAmount) * 100)

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) cat.color.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                selectedCategory = if (isSelected) null else cat
                                onCategorySelected(selectedCategory)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(cat.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${cat.categoryName} ($percentage)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSelected) cat.color else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. BAR CHART (HISTORY DAILY INCOME VS EXPENSE)
// -------------------------------------------------------------
data class DailyBarData(
    val dayLabel: String,
    val income: Double,
    val expense: Double
)

@Composable
fun BarChart(
    dailyData: List<DailyBarData>,
    modifier: Modifier = Modifier,
    percentageComparison: String = "+15% vs semana pasada"
) {
    var hoveredBarIndex by remember { mutableStateOf<Int?>(null) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val maxVal = remember(dailyData) {
        dailyData.flatMap { listOf(it.income, it.expense) }.maxOrNull()?.coerceAtLeast(100.0) ?: 1000.0
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(dailyData) {
        animProgress.animateTo(1f, tween(1000))
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bar_chart_card"),
        cornerRadius = 24.dp,
        backgroundColor = Color(0xF5FFFFFF),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Balance Semanal",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = percentageComparison,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = IncomeGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }

                // Legend Pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ingresos", fontSize = 11.sp, color = TextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseRed))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Egresos", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tooltip banner when bar is touched
            if (hoveredBarIndex != null && hoveredBarIndex!! in dailyData.indices) {
                val bar = dailyData[hoveredBarIndex!!]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PurplePrimary.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${bar.dayLabel}: Ingresos ${currencyFormatter.format(bar.income)} | Egresos ${currencyFormatter.format(bar.expense)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Canvas Bars Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dailyData) {
                            detectTapGestures { offset ->
                                val barGroupWidth = size.width / dailyData.size
                                val index = (offset.x / barGroupWidth).toInt().coerceIn(0, dailyData.size - 1)
                                hoveredBarIndex = if (hoveredBarIndex == index) null else index
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val barGroupWidth = width / dailyData.size
                    val singleBarWidth = (barGroupWidth * 0.32f)

                    dailyData.forEachIndexed { index, item ->
                        val groupX = index * barGroupWidth
                        val incomeH = ((item.income / maxVal) * (height - 24.dp.toPx()) * animProgress.value).toFloat()
                        val expenseH = ((item.expense / maxVal) * (height - 24.dp.toPx()) * animProgress.value).toFloat()

                        // Income Bar
                        val incX = groupX + barGroupWidth * 0.12f
                        val incY = height - incomeH
                        drawRoundRect(
                            color = IncomeGreen,
                            topLeft = Offset(incX, incY),
                            size = Size(singleBarWidth, incomeH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Expense Bar
                        val expX = incX + singleBarWidth + 4.dp.toPx()
                        val expY = height - expenseH
                        drawRoundRect(
                            color = ExpenseRed,
                            topLeft = Offset(expX, expY),
                            size = Size(singleBarWidth, expenseH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dailyData.forEachIndexed { index, bar ->
                    val isSelected = hoveredBarIndex == index
                    Text(
                        text = bar.dayLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isSelected) PurplePrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. QR CODE CANVAS (200x200 FOR EMPLOYEE PAYMENT)
// -------------------------------------------------------------
@Composable
fun QRCodeCanvas(
    modifier: Modifier = Modifier,
    sizeDp: Int = 200
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(3.dp, PurplePrimary, RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridCount = 13
            val cellSize = size.width / gridCount

            // Pseudo-random deterministic matrix pattern representing QR code
            val qrPattern = arrayOf(
                booleanArrayOf(true, true, true, true, true, true, true, false, true, true, true, true, true),
                booleanArrayOf(true, false, false, false, false, false, true, false, false, true, false, true, true),
                booleanArrayOf(true, false, true, true, true, false, true, false, true, false, true, false, true),
                booleanArrayOf(true, false, true, true, true, false, true, false, false, true, true, true, true),
                booleanArrayOf(true, false, false, false, false, false, true, false, true, true, false, false, true),
                booleanArrayOf(true, true, true, true, true, true, true, false, false, false, true, true, true),
                booleanArrayOf(false, false, false, false, false, false, false, false, true, false, false, false, false),
                booleanArrayOf(true, false, true, true, false, true, true, false, true, true, true, true, true),
                booleanArrayOf(true, true, false, false, true, false, false, false, false, true, false, false, true),
                booleanArrayOf(true, false, true, false, true, true, true, false, true, false, true, false, true),
                booleanArrayOf(true, false, true, true, false, false, false, false, true, true, false, true, true),
                booleanArrayOf(true, false, false, false, true, true, true, false, false, false, true, false, true),
                booleanArrayOf(true, true, true, true, true, false, true, false, true, true, true, true, true)
            )

            for (row in 0 until gridCount) {
                for (col in 0 until gridCount) {
                    if (qrPattern[row][col]) {
                        drawRect(
                            color = Color(0xFF1E1B4B),
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize - 1f, cellSize - 1f)
                        )
                    }
                }
            }
        }
    }
}
