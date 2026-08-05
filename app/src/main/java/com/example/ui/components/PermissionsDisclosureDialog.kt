package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PermissionsDisclosureDialog(
    onDismiss: () -> Unit,
    onGrantPermissions: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(16.dp)
                .testTag("permissions_disclosure_dialog"),
            cornerRadius = 28.dp,
            backgroundColor = Color(0xF2FFFFFF),
            borderColor = PurplePrimary.copy(alpha = 0.4f),
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Security Shield Header
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(IncomeGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Seguridad Privada",
                        tint = IncomeGreen,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Transparencia de Permisos y Privacidad",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Antes de solicitar accesos en tu teléfono, queremos explicarte con total claridad para qué se utilizará cada permiso:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Feature Item 1: SMS Reading
                PermissionItemRow(
                    icon = Icons.Default.Sms,
                    title = "Lectura de SMS Bancarios (Transfermóvil)",
                    purpose = "Permite identificar automáticamente las notificaciones de pago e ingresos de la plataforma Transfermóvil para registrar tus cuentas de inmediato sin que tengas que escribirlas manualmente.",
                    accentColor = Color(0xFF2563EB)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Feature Item 2: Phone State / USSD
                PermissionItemRow(
                    icon = Icons.Default.Phone,
                    title = "Estado del Teléfono y Llamadas USSD",
                    purpose = "Facilita la consulta directa de saldo (*222# ETECSA) y ejecución de transferencias bancarias (*966# Transfermóvil) mediante códigos USSD oficiales.",
                    accentColor = Color(0xFF7C3AED)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Feature Item 3: Notifications
                PermissionItemRow(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones de Alerta Financiera",
                    purpose = "Te avisa en el instante exacto en que recibes saldo por QvaPay, comprobantes de pago de clientes o cuando ocurren movimientos importantes.",
                    accentColor = Color(0xFFD97706)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Absolute Privacy Guarantee Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(IncomeGreen.copy(alpha = 0.12f))
                        .border(1.5.dp, IncomeGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Garantía",
                            tint = IncomeGreen,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🔒 GARANTÍA DE PRIVACIDAD ABSOLUTA",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = IncomeGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tus mensajes y datos financieros NUNCA se comparten con terceros y JAMÁS salen de tu dispositivo. Todo el procesamiento se ejecuta 100% en local en el almacenamiento seguro de tu teléfono.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                GlassButton(
                    text = "Entendido y Continuar",
                    isPrimary = true,
                    onClick = onGrantPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accept_permissions_disclosure_btn")
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_permissions_disclosure_btn")
                ) {
                    Text(
                        text = "Configurar Más Tarde",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    purpose: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = purpose,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            )
        }
    }
}
