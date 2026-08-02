package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PeekPreviewType
import com.example.ui.components.BackgroundGradientCanvas
import com.example.ui.components.BarChart
import com.example.ui.components.DailyBarData
import com.example.ui.components.GlassButton
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HistoryFilter
import com.example.ui.viewmodel.MoneyViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: MoneyViewModel,
    modifier: Modifier = Modifier
) {
    val filteredTxList by viewModel.filteredTransactions.collectAsState()
    val activeFilter by viewModel.historyFilter.collectAsState()
    val weeklyBarData by viewModel.weeklyBarData.collectAsState()
    val trendPercentage by viewModel.trendPercentage.collectAsState()

    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    BackgroundGradientCanvas(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Header
                item {
                    Text(
                        text = "Historial de Transacciones",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.testTag("history_screen_title")
                    )
                }

                // GRÁFICA DE BARRAS DE INGRESOS VS EGRESOS
                item {
                    BarChart(
                        dailyData = weeklyBarData,
                        percentageComparison = "$trendPercentage vs periodo anterior"
                    )
                }

                // Filter Chips Row
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("history_filters_row")
                    ) {
                        val filterOptions = listOf(
                            HistoryFilter.ALL to "Todos",
                            HistoryFilter.TODAY to "Hoy",
                            HistoryFilter.THIS_WEEK to "Esta semana",
                            HistoryFilter.THIS_MONTH to "Este mes",
                            HistoryFilter.INCOME to "Ingresos",
                            HistoryFilter.EXPENSE to "Egresos"
                        )

                        items(filterOptions) { (filter, label) ->
                            val isSelected = activeFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) PurplePrimary else Color(0xCCFFFFFF)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) PurplePrimary else GlassBorder,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { viewModel.setHistoryFilter(filter) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }

                if (filteredTxList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No hay transacciones registradas en este período.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(filteredTxList) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            timeFormatter = timeFormatter,
                            currencyFormatter = currencyFormatter,
                            onClick = { viewModel.selectedTransactionForDetail.value = tx },
                            onLongClick = { viewModel.openPeekPreview(PeekPreviewType.TransactionDetail(tx)) }
                        )
                    }
                }

                item {
                    GlassButton(
                        text = "+ Nueva Transacción",
                        onClick = { viewModel.showAddTransactionDialog.value = true },
                        modifier = Modifier.fillMaxWidth().testTag("add_transaction_bottom_btn")
                    )
                }

                item { Spacer(modifier = Modifier.height(90.dp)) }
            }
        }
    }
}

