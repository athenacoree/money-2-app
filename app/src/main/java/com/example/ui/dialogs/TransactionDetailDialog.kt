package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Transaction
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.ui.viewmodel.MoneyViewModel

@Composable
fun TransactionDetailDialog(
    viewModel: MoneyViewModel,
    transaction: Transaction,
    onDismiss: () -> Unit
) {
    TransactionDetailDialog(
        transaction = transaction,
        onDismiss = onDismiss,
        onEdit = { /* Edit transaction handled */ },
        onDelete = { viewModel.deleteTransaction(it) },
        onReconcile = { tx ->
            viewModel.selectedTransactionForDetail.value = null
            viewModel.selectedTransactionForReconciliation.value = tx
        },
        onUnconfirm = { tx ->
            viewModel.unconfirmEsperaTransaction(tx)
        }
    )
}

@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onShareReceipt: ((Transaction) -> Unit)? = null,
    onReconcile: ((Transaction) -> Unit)? = null,
    onUnconfirm: ((Transaction) -> Unit)? = null
) {
    val isIncome = transaction.tipo == "ingreso"
    val accentColor = if (isIncome) IncomeGreen else ExpenseRed

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy - hh:mm a", Locale("es", "ES"))

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("tx_detail_dialog"),
            cornerRadius = 28.dp,
            backgroundColor = Color(0xF5FFFFFF),
            elevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Detalle de Transacción",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Icono grande de categoría (60dp)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transaction.categoria.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            fontSize = 26.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Monto en grande (32sp)
                Text(
                    text = "${if (isIncome) "+" else "-"}${currencyFormatter.format(transaction.monto)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        fontSize = 32.sp
                    ),
                    modifier = Modifier.testTag("tx_detail_amount")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Nombre de categoría (20sp, semibold)
                Text(
                    text = transaction.categoria,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Descripción (16sp, gris)
                Text(
                    text = transaction.descripcion,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Fecha y hora (16sp, gris)
                Text(
                    text = dateFormatter.format(Date(transaction.fecha)) + " (${transaction.hora})",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Badge Método de pago
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x1F7C3AED))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = "Método de pago",
                            tint = PurplePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = transaction.metodo_pago,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = PurplePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                val isConfirmedEspera = transaction.descripcion.startsWith("[Confirmado]")
                if (isConfirmedEspera && onUnconfirm != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassButton(
                        text = "Desmarcar como Confirmado (Revertir)",
                        isPrimary = false,
                        onClick = {
                            onUnconfirm(transaction)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unconfirm_tx_btn")
                    )
                }

                if (isIncome && !isConfirmedEspera && onReconcile != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassButton(
                        text = "Conciliar con Productos del Catálogo",
                        isPrimary = false,
                        onClick = { onReconcile(transaction) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reconcile_tx_btn")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones de acción inferior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cerrar (morado)
                    GlassButton(
                        text = "Cerrar",
                        isPrimary = true,
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                    )

                    // Eliminar (rojo)
                    GlassButton(
                        text = "Eliminar",
                        icon = Icons.Default.Delete,
                        isPrimary = false,
                        onClick = {
                            onDelete(transaction)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delete_tx_btn")
                    )
                }
            }
        }
    }
}
