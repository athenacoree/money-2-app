package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Producto
import com.example.data.model.Transaction
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MoneyViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LinkProductsToTransactionDialog(
    viewModel: MoneyViewModel,
    transaction: Transaction,
    onDismiss: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    var selectedQuantities by remember { mutableStateOf<Map<Producto, Int>>(emptyMap()) }
    var isOtherReason by remember { mutableStateOf(false) }
    var otherReasonText by remember { mutableStateOf("") }
    var showCreateNewProduct by remember { mutableStateOf(false) }

    val totalProductsAmount = remember(selectedQuantities) {
        selectedQuantities.entries.sumOf { (prod, qty) -> prod.precio * qty }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("link_products_dialog"),
            cornerRadius = 28.dp,
            backgroundColor = Color(0xF5FFFFFF),
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Conciliar Transferencia",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "Monto recibido: ${currencyFormatter.format(transaction.monto)} CUP",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = PurplePrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector Mode Tabs: "Productos del Catálogo" vs "Otro motivo"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1F7C3AED))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isOtherReason) PurplePrimary else Color.Transparent)
                            .clickable { isOtherReason = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Catálogo (${selectedQuantities.values.sum()})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (!isOtherReason) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isOtherReason) PurplePrimary else Color.Transparent)
                            .clickable { isOtherReason = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Otro motivo",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isOtherReason) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isOtherReason) {
                    // Modo Otro motivo
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Indica la razón o concepto de esta transferencia:",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontSize = 13.sp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = otherReasonText,
                            onValueChange = { otherReasonText = it },
                            placeholder = { Text("Ej: Préstamo personal, Reembolso, Regalo...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                } else {
                    // Tarjeta de Análisis de Coincidencia de Precio
                    val diff = transaction.monto - totalProductsAmount
                    val isExactMatch = totalProductsAmount > 0 && Math.abs(diff) < 0.01
                    val isOverpaid = totalProductsAmount > 0 && diff > 0.01
                    val isUnderpaid = totalProductsAmount > 0 && diff < -0.01

                    val bannerBg = when {
                        isExactMatch -> Color(0x1F10B981)
                        isOverpaid -> Color(0x1F3B82F6)
                        isUnderpaid -> Color(0x1FEF4444)
                        else -> Color(0x1F7C3AED)
                    }

                    val bannerBorder = when {
                        isExactMatch -> IncomeGreen
                        isOverpaid -> Color(0xFF3B82F6)
                        isUnderpaid -> ExpenseRed
                        else -> PurplePrimary
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(bannerBg)
                            .border(1.dp, bannerBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    isExactMatch -> Icons.Default.CheckCircle
                                    isOverpaid -> Icons.Default.Info
                                    isUnderpaid -> Icons.Default.Warning
                                    else -> Icons.Default.ShoppingBag
                                },
                                contentDescription = null,
                                tint = bannerBorder,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = when {
                                        selectedQuantities.isEmpty() -> "Selecciona los productos vendidos"
                                        isExactMatch -> "✅ ¡Monto Exacto Coincide!"
                                        isOverpaid -> "ℹ️ Pago Excedente / Propina"
                                        isUnderpaid -> "⚠️ Monto Incompleto / Faltante"
                                        else -> "Análisis de Precio"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = bannerBorder
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        selectedQuantities.isEmpty() -> "Indica uno o más productos de la lista."
                                        isExactMatch -> "El total de productos (${currencyFormatter.format(totalProductsAmount)}) coincide exactamente con la transferencia."
                                        isOverpaid -> "Productos: ${currencyFormatter.format(totalProductsAmount)}. Excedente recibido: +${currencyFormatter.format(diff)}."
                                        isUnderpaid -> "Productos: ${currencyFormatter.format(totalProductsAmount)}. Faltante por cobrar: -${currencyFormatter.format(Math.abs(diff))}."
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Productos en catálogo:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )

                        TextButton(onClick = { viewModel.showAddProductDialog.value = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nuevo producto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (products.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay productos en el catálogo aún.", color = TextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(products) { product ->
                                val qty = selectedQuantities[product] ?: 0

                                GlassCard(
                                    cornerRadius = 14.dp,
                                    backgroundColor = Color(0xF8FFFFFF)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = product.nombre,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${currencyFormatter.format(product.precio)} | Stock: ${product.stock}",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (qty > 0) {
                                                IconButton(
                                                    onClick = {
                                                        if (qty <= 1) {
                                                            selectedQuantities = selectedQuantities - product
                                                        } else {
                                                            selectedQuantities = selectedQuantities + (product to (qty - 1))
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Restar", tint = ExpenseRed)
                                                }

                                                Text(
                                                    text = "$qty",
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    selectedQuantities = selectedQuantities + (product to (qty + 1))
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Sumar", tint = PurplePrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botón Guardar Conciliación
                GlassButton(
                    text = "Guardar Conciliación",
                    isPrimary = true,
                    onClick = {
                        viewModel.reconcileTransactionWithProducts(
                            transaction = transaction,
                            selectedProducts = if (isOtherReason) emptyMap() else selectedQuantities,
                            isOtherReason = isOtherReason,
                            customReason = otherReasonText
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_reconciliation_btn")
                )
            }
        }
    }
}
