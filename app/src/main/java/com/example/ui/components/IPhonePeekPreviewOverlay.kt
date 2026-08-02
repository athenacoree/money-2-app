package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Producto
import com.example.data.model.Transaction
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

sealed class PeekPreviewType {
    data class TransactionDetail(val transaction: Transaction) : PeekPreviewType()
    data class ChartExpanded(val title: String, val dailyData: List<DailyBarData>, val percentage: String) : PeekPreviewType()
    data class FunctionInfo(val title: String, val description: String, val icon: ImageVector, val tips: List<String> = emptyList()) : PeekPreviewType()
    data class ProductDetail(val product: Producto) : PeekPreviewType()
}

@Composable
fun IPhonePeekPreviewDialog(
    peekType: PeekPreviewType?,
    onDismiss: () -> Unit
) {
    if (peekType == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialScale = 0.85f
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .padding(16.dp)
                        .clickable(enabled = false) {}, // Prevent dismiss when clicking inside card
                    cornerRadius = 28.dp,
                    backgroundColor = Color(0xFAFFFFFF),
                    elevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // iOS Header bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(PurplePrimary, Color(0xFF9333EA))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Previsualización Extendida (iOS)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1F000000))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic Content according to PeekType
                        when (peekType) {
                            is PeekPreviewType.TransactionDetail -> {
                                TransactionPeekContent(peekType.transaction)
                            }
                            is PeekPreviewType.ChartExpanded -> {
                                ChartPeekContent(peekType.title, peekType.dailyData, peekType.percentage)
                            }
                            is PeekPreviewType.FunctionInfo -> {
                                FunctionPeekContent(peekType.title, peekType.description, peekType.icon, peekType.tips)
                            }
                            is PeekPreviewType.ProductDetail -> {
                                ProductPeekContent(peekType.product)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        GlassButton(
                            text = "Entendido / Cerrar",
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("peek_close_btn")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionPeekContent(tx: Transaction) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = SimpleDateFormat("EEEE, d 'de' MMMM yyyy", Locale("es", "ES"))
    val timeFormatter = SimpleDateFormat("hh:mm:ss a", Locale.US)
    val isIncome = tx.tipo.lowercase() == "ingreso"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isIncome) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isIncome) "+" else "-",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) IncomeGreen else ExpenseRed
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = currencyFormatter.format(tx.monto),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = if (isIncome) IncomeGreen else ExpenseRed
            )
        )

        Text(
            text = tx.categoria.ifBlank { "General" },
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x0F7C3AED))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailRow(label = "Descripción", value = tx.descripcion.ifBlank { "Sin detalle" })
            DetailRow(label = "Fecha", value = dateFormatter.format(Date(tx.fecha)))
            DetailRow(label = "Hora", value = timeFormatter.format(Date(tx.fecha)))
            DetailRow(label = "Método de Pago", value = tx.metodo_pago.ifBlank { "Efectivo" })
            DetailRow(
                label = "Modo de Ejecución",
                value = if (tx.es_empleador) "Empleador / Negocio" else "Empleado / Terminal"
            )
        }
    }
}

@Composable
private fun ChartPeekContent(title: String, dailyData: List<DailyBarData>, percentage: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Text(
            text = "$percentage vs periodo anterior",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = IncomeGreen,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Large BarChart in extended card
        BarChart(
            dailyData = dailyData,
            percentageComparison = "Vista Expandida 3D Touch",
            modifier = Modifier.height(220.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        val totalInc = dailyData.sumOf { it.income }
        val totalExp = dailyData.sumOf { it.expense }
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x0F7C3AED))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Total Ingresos Semanales", fontSize = 11.sp, color = TextSecondary)
                Text(currencyFormatter.format(totalInc), fontWeight = FontWeight.Bold, color = IncomeGreen)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Total Egresos Semanales", fontSize = 11.sp, color = TextSecondary)
                Text(currencyFormatter.format(totalExp), fontWeight = FontWeight.Bold, color = ExpenseRed)
            }
        }
    }
}

@Composable
private fun FunctionPeekContent(title: String, description: String, icon: ImageVector, tips: List<String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x1F7C3AED)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text("Guía Rápida & Detalles", fontSize = 11.sp, color = PurplePrimary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 13.sp)
        )

        if (tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x0F7C3AED))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Aspectos Clave:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                tips.forEach { tip ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", color = PurplePrimary, fontWeight = FontWeight.Bold)
                        Text(tip, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductPeekContent(product: Producto) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, PurplePrimary, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imagen_uri.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imagen_uri,
                        contentDescription = product.nombre,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(product.nombre.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = product.nombre,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Text(
                    text = "ID Producto: #${product.id}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x0F7C3AED))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailRow(label = "Precio de Venta", value = currencyFormatter.format(product.precio))
            DetailRow(label = "Stock Disponible", value = "${product.stock} unidades")
            DetailRow(label = "Valor Total Estimado", value = currencyFormatter.format(product.precio * product.stock))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
