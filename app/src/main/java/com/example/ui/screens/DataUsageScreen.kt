package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.BackgroundGradientCanvas
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MoneyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataUsageScreen(
    viewModel: MoneyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestSaldo by viewModel.latestSaldoMovil.collectAsState()
    val consumedBytes by viewModel.consumedMobileDataBytes.collectAsState()
    val activePromos by viewModel.activePromociones.collectAsState()
    val allSaldoMovil by viewModel.allSaldoMovil.collectAsState()

    val ussdStatus by viewModel.ussdStatus.collectAsState()
    val ussdMsg by viewModel.ussdMessage.collectAsState()

    var showManualDialog by remember { mutableStateOf(false) }
    var manualCUPText by remember { mutableStateOf("") }
    var manualMBText by remember { mutableStateOf("") }
    var manualBonoMBText by remember { mutableStateOf("") }
    var manualVencimientoText by remember { mutableStateOf("") }

    val hasPermission = remember { mutableStateOf(viewModel.hasUsageStatsPermission()) }

    // Recheck permission on resume
    LaunchedEffect(Unit) {
        hasPermission.value = viewModel.hasUsageStatsPermission()
    }

    // Calculations
    val totalConfiguredMB = latestSaldo?.datosMB ?: 0.0
    val consumedMB = (consumedBytes / (1024.0 * 1024.0))
    val remainingMB = (totalConfiguredMB - consumedMB).coerceAtLeast(0.0)
    val progressFraction = if (totalConfiguredMB > 0.0) {
        (consumedMB / totalConfiguredMB).coerceIn(0.0, 1.0)
    } else {
        0.0
    }

    BackgroundGradientCanvas(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Header with Back Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Consumo de Datos",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = TextPrimary
                        )
                    )
                }
            }

            // Real Cellular Data Access Warning Card
            if (!hasPermission.value) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        backgroundColor = Color(0xFFFFF3CD), // Light warning yellow
                        elevation = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "Advertencia", tint = Color(0xFF856404))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Acceso a estadísticas de uso requerido",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF856404)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Para poder medir cuántos MB ha gastado tu teléfono del paquete móvil de ETECSA en tiempo real, es necesario conceder un permiso especial del sistema.",
                                fontSize = 12.sp,
                                color = Color(0xFF856404)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF856404))
                            ) {
                                Text("Ir a Ajustes para Activar", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Gráfica de Consumo (Rueda o Progress Bar estilizada)
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
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Consumo del Paquete Activo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (latestSaldo == null) {
                            Text(
                                text = "Sin datos — toca para consultar tu saldo",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        } else {
                            // Progress Indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x1F7C3AED))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progressFraction.toFloat())
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PurplePrimary)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Gasto Real", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        String.format(Locale.US, "%.1f MB", consumedMB),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PurplePrimary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Restante", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        String.format(Locale.US, "%.1f MB", remainingMB),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = IncomeGreen
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Contratado", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        String.format(Locale.US, "%.1f MB", totalConfiguredMB),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Vence: ${latestSaldo?.fechaVencimiento ?: "No configurado"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )

                                Text(
                                    "Saldo CUP: ${latestSaldo?.saldoCUP ?: 0.0} CUP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimary
                                )
                            }
                        }
                    }
                }
            }

            // Acciones: Consulta USSD o Ingreso Manual
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassButton(
                        text = "Consulta USSD (*222#)",
                        icon = Icons.Default.PhoneCallback,
                        isPrimary = true,
                        onClick = { viewModel.requestUssdBalanceUpdate() },
                        modifier = Modifier.weight(1f)
                    )

                    GlassButton(
                        text = "Configuración Manual",
                        icon = Icons.Default.Edit,
                        isPrimary = false,
                        onClick = { showManualDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // USSD Status Message Overlay
            if (ussdStatus != "IDLE") {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        backgroundColor = Color(0x0F7C3AED)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (ussdStatus == "REQUESTING") {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PurplePrimary, strokeWidth = 2.5.dp)
                            } else {
                                Icon(
                                    imageVector = if (ussdStatus == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (ussdStatus == "SUCCESS") IncomeGreen else ExpenseRed
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = ussdMsg ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (ussdStatus != "REQUESTING") {
                                IconButton(onClick = { viewModel.ussdStatus.value = "IDLE" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN DE PROMOCIONES DE ETECSA
            item {
                Text(
                    text = "Promociones y Mensajes de ETECSA / Cubacel",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            if (activePromos.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        backgroundColor = Color(0xF5FFFFFF)
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Loyalty, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No se han detectado promociones activas por SMS.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(activePromos) { promo ->
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
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PurplePrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "PROMO INTERNACIONAL",
                                        color = PurplePrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(promo.timestamp)),
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = promo.descripcion,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // SECCIÓN: HISTORIAL COMPLETO DE MENSAJES
            item {
                Text(
                    text = "Historial Completo de Mensajes ETECSA",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            if (allSaldoMovil.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        backgroundColor = Color(0xF5FFFFFF)
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No se han detectado mensajes de ETECSA aún.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(allSaldoMovil) { msg ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        backgroundColor = Color(0xF5FFFFFF)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val badgeColor = when (msg.tipo) {
                                    "saldo_principal" -> IncomeGreen
                                    "bono_datos" -> PurplePrimary
                                    "promocion" -> Color(0xFF3B82F6)
                                    else -> Color(0xFFFF9800)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(badgeColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = msg.tipo.replace("_", " ").uppercase(),
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = msg.descripcion.ifBlank { "Saldo principal: ${msg.saldoCUP} CUP | Datos: ${msg.datosMB} MB | Bono: ${msg.bonoDatosMB} MB" },
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }

    // Manual Entry Dialog
    if (showManualDialog) {
        Dialog(onDismissRequest = { showManualDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Configuración Manual de Saldo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = manualCUPText,
                        onValueChange = { manualCUPText = it },
                        label = { Text("Saldo Principal (CUP)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualMBText,
                        onValueChange = { manualMBText = it },
                        label = { Text("Paquete de Datos (MB)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualBonoMBText,
                        onValueChange = { manualBonoMBText = it },
                        label = { Text("Bono de Datos / Nacional (MB)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualVencimientoText,
                        onValueChange = { manualVencimientoText = it },
                        label = { Text("Fecha Vencimiento (e.g. 25/11/2026)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    GlassButton(
                        text = "Guardar",
                        isPrimary = true,
                        onClick = {
                            val cup = manualCUPText.toDoubleOrNull() ?: 0.0
                            val mb = manualMBText.toDoubleOrNull() ?: 0.0
                            val bono = manualBonoMBText.toDoubleOrNull() ?: 0.0
                            viewModel.saveManualSaldo(cup, mb, bono, manualVencimientoText)
                            showManualDialog = false
                            manualCUPText = ""
                            manualMBText = ""
                            manualBonoMBText = ""
                            manualVencimientoText = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = { showManualDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}
