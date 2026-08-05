package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSecondary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class TourStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badgeText: String,
    val color: Color
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingTourDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val steps = remember {
        listOf(
            TourStep(
                title = "Bienvenido a MONEY",
                description = "Tu plataforma de control financiero inteligente. Administra tu dinero con interfaz moderna, gráficos en tiempo real y máxima nitidez.",
                icon = Icons.Default.AccountBalanceWallet,
                badgeText = "FINANZAS 100% LIMPIAS",
                color = PurplePrimary
            ),
            TourStep(
                title = "Ecosistema QvaPay Integrado",
                description = "Conecta tu cuenta de QvaPay para realizar transferencias P2P a cualquier usuario, seguir cotizaciones de criptomonedas y emitir facturas QR.",
                icon = Icons.Default.FlashOn,
                badgeText = "API REAL Y EN VIVO",
                color = Color(0xFF7C3AED)
            ),
            TourStep(
                title = "SMS & Lectura Automática",
                description = "Detección instantánea de mensajes de Transfermóvil y notificaciones bancarias para registrar tus ingresos sin esfuerzo.",
                icon = Icons.Default.Sms,
                badgeText = "AUTOMATIZACIÓN LOCAL",
                color = Color(0xFF2563EB)
            ),
            TourStep(
                title = "Modos Empleador, Empleado y Distribuidor",
                description = "Cambia el modo de la aplicación para gestionar catálogo de productos, sucursales, auditorías de inventario y despachos de mercancía.",
                icon = Icons.Default.Storefront,
                badgeText = "GESTIÓN EMPRESARIAL",
                color = Color(0xFFD97706)
            ),
            TourStep(
                title = "Privacidad & Seguridad Garantizada",
                description = "Tus datos financieros jamás salen de tu dispositivo. Todo el almacenamiento es local con encriptación y protección por PIN o biometría.",
                icon = Icons.Default.Shield,
                badgeText = "ALMACENAMIENTO PRIVADO",
                color = IncomeGreen
            )
        )
    }

    var currentStepIndex by remember { mutableStateOf(0) }
    val currentStep = steps[currentStepIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .testTag("onboarding_tour_dialog"),
            cornerRadius = 28.dp,
            backgroundColor = Color(0xF2FFFFFF),
            borderColor = currentStep.color.copy(alpha = 0.4f),
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentStep.color.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = currentStep.badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = currentStep.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Hero Icon Display
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(currentStep.color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = currentStep.icon,
                        contentDescription = null,
                        tint = currentStep.color,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title & Description
                Text(
                    text = currentStep.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = currentStep.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Dots Progress Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { idx, _ ->
                        val isCurrent = idx == currentStepIndex
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isCurrent) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) currentStep.color else Color.LightGray.copy(alpha = 0.5f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIndex > 0) {
                        TextButton(
                            onClick = { currentStepIndex-- },
                            modifier = Modifier.testTag("tour_prev_btn")
                        ) {
                            Text("Anterior", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("tour_skip_btn")
                        ) {
                            Text("Omitir", color = TextSecondary)
                        }
                    }

                    GlassButton(
                        text = if (currentStepIndex == steps.size - 1) "¡Comenzar!" else "Siguiente",
                        isPrimary = true,
                        onClick = {
                            if (currentStepIndex < steps.size - 1) {
                                currentStepIndex++
                            } else {
                                onComplete()
                            }
                        },
                        modifier = Modifier.testTag("tour_next_btn")
                    )
                }
            }
        }
    }
}
