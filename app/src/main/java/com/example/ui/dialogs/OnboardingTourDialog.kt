package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

data class TourPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun OnboardingTourDialog(
    onDismiss: () -> Unit,
    onFinished: () -> Unit
) {
    val pages = remember {
        listOf(
            TourPage(
                title = "¡Bienvenido a CubaFinanzas!",
                description = "La herramienta definitiva para la gestión de tus finanzas personales y de negocios en Cuba. Totalmente adaptada a nuestro entorno.",
                icon = Icons.Default.AccountBalanceWallet,
                color = PurplePrimary
            ),
            TourPage(
                title = "Registro Automático de SMS",
                description = "Olvídate de anotar cada pago a mano. Detectamos automáticamente las transferencias de Transfermóvil y EnZona directamente de tus mensajes entrantes.",
                icon = Icons.Default.Sms,
                color = IncomeGreen
            ),
            TourPage(
                title = "Modos Especializados de Trabajo",
                description = "Gestiona tu negocio fácilmente como Empleador, Empleado o Distribuidor. Maneja catálogos unificados, existencias y despachos con facilidad.",
                icon = Icons.Default.BusinessCenter,
                color = PurplePrimary
            ),
            TourPage(
                title = "Integración con QvaPay v2",
                description = "Conecta tu cuenta de QvaPay de forma segura y directa para generar facturas de cobro (enlaces SQP) y enviar remesas o fondos en segundos.",
                icon = Icons.Default.QrCodeScanner,
                color = Color(0xFF3B82F6)
            ),
            TourPage(
                title = "Cubacel y Control de Datos",
                description = "Consulta tu saldo Cubacel real mediante USSD (*222#) y lleva un seguimiento exacto de tus megabytes consumidos en tu red móvil en tiempo real.",
                icon = Icons.Default.Wifi,
                color = Color(0xFFFF9800)
            )
        )
    }

    var currentPageIndex by remember { mutableStateOf(0) }
    val page = pages[currentPageIndex]

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("onboarding_tour_dialog"),
            cornerRadius = 28.dp,
            backgroundColor = Color(0xF5FFFFFF),
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with "Skip" option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onFinished) {
                        Text(
                            text = "Omitir",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Huge illustration-style icon
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(page.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = page.color,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.heightIn(min = 60.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentPageIndex) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (index == currentPageIndex) page.color else Color.LightGray)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentPageIndex > 0) {
                        GlassButton(
                            text = "Anterior",
                            isPrimary = false,
                            onClick = { currentPageIndex-- },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    GlassButton(
                        text = if (currentPageIndex == pages.size - 1) "¡Comenzar!" else "Siguiente",
                        isPrimary = true,
                        onClick = {
                            if (currentPageIndex < pages.size - 1) {
                                currentPageIndex++
                            } else {
                                onFinished()
                            }
                        },
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}
