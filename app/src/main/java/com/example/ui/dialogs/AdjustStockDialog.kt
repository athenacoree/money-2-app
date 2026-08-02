package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Producto
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdjustStockDialog(
    product: Producto,
    userRole: String, // "Empleador" o "Empleado"
    onDismiss: () -> Unit,
    onSave: (newStock: Int, justification: String) -> Unit
) {
    var newStockText by remember { mutableStateOf(product.stock.toString()) }
    var selectedChipJustification by remember { mutableStateOf("") }
    var customJustification by remember { mutableStateOf("") }

    val quickReasons = listOf(
        "Conteo físico / Cuadre",
        "Baja por rotura / Merma",
        "Compra de inventario",
        "Venta manual",
        "Devolución de cliente"
    )

    val currentNewStock = newStockText.toIntOrNull() ?: product.stock
    val diff = currentNewStock - product.stock

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("adjust_stock_dialog"),
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
                            text = "Ajuste de Stock (Auditoría)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "Registrado por: $userRole",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = PurplePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Info del Producto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1F7C3AED))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = product.nombre,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Stock actual registrado: ${product.stock} unidades",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        val badgeColor = when {
                            diff > 0 -> IncomeGreen
                            diff < 0 -> ExpenseRed
                            else -> TextSecondary
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (diff > 0) "+$diff" else "$diff",
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control del nuevo Stock
                Text(
                    text = "Nuevo Stock Total:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            val curr = newStockText.toIntOrNull() ?: product.stock
                            if (curr > 0) newStockText = (curr - 1).toString()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x1FEF4444))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Restar", tint = ExpenseRed)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    OutlinedTextField(
                        value = newStockText,
                        onValueChange = { newStockText = it.filter { char -> char.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .width(100.dp)
                            .testTag("new_stock_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = {
                            val curr = newStockText.toIntOrNull() ?: product.stock
                            newStockText = (curr + 1).toString()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x1F10B981))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Sumar", tint = IncomeGreen)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Motivos Rápidos (Chips)
                Text(
                    text = "Selecciona o escribe la justificación para auditoría:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickReasons.forEach { reason ->
                        val isSelected = selectedChipJustification == reason
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PurplePrimary else Color(0x1F7C3AED))
                                .clickable {
                                    if (isSelected) {
                                        selectedChipJustification = ""
                                    } else {
                                        selectedChipJustification = reason
                                        customJustification = ""
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = reason,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customJustification,
                    onValueChange = {
                        customJustification = it
                        if (it.isNotBlank()) selectedChipJustification = ""
                    },
                    placeholder = { Text("Detalle o comentario adicional...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Guardar
                val finalReason = selectedChipJustification.ifBlank { customJustification.ifBlank { "Ajuste de inventario por auditoría" } }

                GlassButton(
                    text = "Guardar Ajuste de Auditoría",
                    isPrimary = true,
                    onClick = {
                        onSave(currentNewStock, finalReason)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_stock_adjustment_btn")
                )
            }
        }
    }
}
