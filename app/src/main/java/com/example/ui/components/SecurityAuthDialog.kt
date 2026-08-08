package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.data.model.PinHasher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SecurityAuthDialog(
    title: String,
    reason: String,
    onVerifyPin: (String) -> Boolean,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableStateOf(0) }
    var cooldownSecondsRemaining by remember { mutableStateOf(0) }
    var isScanningBiometric by remember { mutableStateOf(false) }
    var biometricSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(cooldownSecondsRemaining) {
        if (cooldownSecondsRemaining > 0) {
            delay(1000)
            cooldownSecondsRemaining -= 1
        }
    }

    // Pulse animation for biometrics
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(28.dp), spotColor = PurplePrimary)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF9F5FF)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xCC7C3AED),
                            Color(0x66A78BFA),
                            Color(0x993B82F6)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Shield Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(if (isScanningBiometric) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (biometricSuccess) listOf(IncomeGreen, Color(0xFF059669))
                                else listOf(PurplePrimary, Color(0xFF2563EB))
                            )
                        )
                        .shadow(10.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (biometricSuccess) Icons.Default.Shield else Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("security_auth_dialog_title")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = reason,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isScanningBiometric) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = PurplePrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (biometricSuccess) "¡Identidad Verificada!" else "Verificando Huella / Rostro...",
                            fontWeight = FontWeight.Bold,
                            color = if (biometricSuccess) IncomeGreen else PurplePrimary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    // PIN Dots Display (4 digits)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..4) {
                            val isFilled = enteredPin.length >= i
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            pinError -> ExpenseRed
                                            isFilled -> PurplePrimary
                                            else -> Color(0x337C3AED)
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (pinError) ExpenseRed else PurplePrimary,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    if (cooldownSecondsRemaining > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Demasiados intentos. Espera $cooldownSecondsRemaining s.",
                            color = ExpenseRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (pinError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PIN incorrecto. Inténtalo de nuevo (${5 - failedAttempts} intentos restantes).",
                            color = ExpenseRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Number Pad (Keypad)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val numRows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("BIO", "0", "DEL")
                        )

                        numRows.forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                row.forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                when (key) {
                                                    "BIO" -> Color(0x1F7C3AED)
                                                    "DEL" -> Color(0x1FEF4444)
                                                    else -> Color(0xFFF3F4F6)
                                                }
                                            )
                                            .clickable(enabled = cooldownSecondsRemaining <= 0) {
                                                pinError = false
                                                when (key) {
                                                    "DEL" -> {
                                                        if (enteredPin.isNotEmpty()) {
                                                            enteredPin = enteredPin.dropLast(1)
                                                        }
                                                    }
                                                    "BIO" -> {
                                                        coroutineScope.launch {
                                                            isScanningBiometric = true
                                                            delay(700)
                                                            biometricSuccess = true
                                                            delay(300)
                                                            onSuccess()
                                                        }
                                                    }
                                                    else -> {
                                                        if (enteredPin.length < 4) {
                                                            enteredPin += key
                                                            if (enteredPin.length == 4) {
                                                                val isMatch = onVerifyPin(enteredPin)
                                                                if (isMatch) {
                                                                    onSuccess()
                                                                } else {
                                                                    failedAttempts++
                                                                    if (failedAttempts >= 5) {
                                                                        cooldownSecondsRemaining = 30 * (failedAttempts - 4)
                                                                    }
                                                                    pinError = true
                                                                    enteredPin = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (key) {
                                            "BIO" -> Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometría",
                                                tint = PurplePrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            "DEL" -> Text(
                                                text = "⌫",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = ExpenseRed
                                            )
                                            else -> Text(
                                                text = key,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancelar", color = TextSecondary, fontSize = 14.sp)
                    }

                    Text(
                        text = "PIN Por Defecto: 1234",
                        fontSize = 11.sp,
                        color = PurplePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
