package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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

data class PermissionDetail(
    val title: String,
    val description: String,
    val consequence: String,
    val icon: ImageVector,
    val color: Color,
    val systemPermissions: List<String>
)

@Composable
fun PermissionsDisclosureDialog(
    permissionsToExplain: List<PermissionDetail>,
    onDismiss: () -> Unit,
    onApproved: (PermissionDetail) -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }

    if (currentIndex >= permissionsToExplain.size) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }

    val permission = permissionsToExplain[currentIndex]

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("permissions_disclosure_dialog"),
            cornerRadius = 24.dp,
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(permission.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = permission.icon,
                            contentDescription = null,
                            tint = permission.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Solicitud de Permiso",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = PurplePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = permission.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0A000000))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "¿Para qué se usa?",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = permission.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Consequence Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ExpenseRed.copy(alpha = 0.08f))
                        .border(1.dp, ExpenseRed.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "Si decides no concederlo:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = permission.consequence,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Strict Privacy Statement (Garantía de Privacidad Real)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IncomeGreen.copy(alpha = 0.08f))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacidad",
                            tint = IncomeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tus datos financieros se queda guardados localmente en tu teléfono. Solo se transmiten datos externos si decides activar de forma voluntaria la integración oficial de QvaPay o la sincronización P2P dentro de tu red local.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IncomeGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = {
                            currentIndex++
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Ahora no",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    GlassButton(
                        text = "Entendido y Continuar",
                        isPrimary = true,
                        onClick = {
                            onApproved(permission)
                            currentIndex++
                        },
                        modifier = Modifier.weight(1.8f)
                    )
                }
            }
        }
    }
}
