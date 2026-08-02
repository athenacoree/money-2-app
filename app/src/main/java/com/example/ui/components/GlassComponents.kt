package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSecondary
import com.example.ui.theme.TextPrimary
import java.text.NumberFormat
import java.util.Locale

/**
 * Animated Ambient Background with subtle organic lines, waves and soft purple gradient glow.
 */
@Composable
fun BackgroundGradientCanvas(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F0FF),
                        Color(0xFFFAFAFF),
                        Color(0xFFFFFFFF)
                    )
                )
            )
            .drawBehind {
                val width = size.width
                val height = size.height

                // Soft organic background glow circles
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x1F7C3AED), Color(0x007C3AED)),
                        center = Offset(width * 0.85f, height * 0.15f),
                        radius = width * 0.65f
                    ),
                    center = Offset(width * 0.85f, height * 0.15f),
                    radius = width * 0.65f
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x1FA78BFA), Color(0x00A78BFA)),
                        center = Offset(width * 0.15f, height * 0.55f),
                        radius = width * 0.7f
                    ),
                    center = Offset(width * 0.15f, height * 0.55f),
                    radius = width * 0.7f
                )

                // Elegant organic curved thread lines (Hilos decorativos sutiles)
                val path1 = Path().apply {
                    moveTo(0f, height * 0.12f)
                    cubicTo(
                        width * 0.4f, height * 0.05f,
                        width * 0.6f, height * 0.22f,
                        width, height * 0.18f
                    )
                }
                drawPath(
                    path = path1,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0x1F7C3AED), Color(0x33A78BFA), Color(0x057C3AED))
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )

                val path2 = Path().apply {
                    moveTo(0f, height * 0.75f)
                    cubicTo(
                        width * 0.35f, height * 0.82f,
                        width * 0.7f, height * 0.68f,
                        width, height * 0.78f
                    )
                }
                drawPath(
                    path = path2,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0x05A78BFA), Color(0x2B7C3AED), Color(0x10A78BFA))
                    ),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }
    ) {
        content()
    }
}

/**
 * Premium Glassmorphism Card with press animation scaling effect
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    backgroundColor: Color = GlassWhite,
    borderColor: Color = GlassCardBorder,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 6.dp,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pressScale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    val combinedModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color(0x14000000),
            spotColor = Color(0x1A7C3AED)
        )
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    backgroundColor,
                    backgroundColor.copy(alpha = (backgroundColor.alpha * 0.85f).coerceAtLeast(0.3f))
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        )
        .border(
            width = borderWidth,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.3f),
                    GlassBorder
                )
            ),
            shape = shape
        )
        .then(
            if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
        .then(
            if (onClick != null) {
                Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() }
                    )
                }
            } else Modifier
        )

    Box(
        modifier = combinedModifier,
        content = content
    )
}

/**
 * Modern Glass Button with vibrant purple gradient or frosted style
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    testTag: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    val shape = RoundedCornerShape(20.dp)

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0x339CA3AF)
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .shadow(
                elevation = if (isPrimary || isDestructive) 10.dp else 4.dp,
                shape = shape,
                spotColor = when {
                    isDestructive -> Color(0x80EF4444)
                    isPrimary -> Color(0x667C3AED)
                    else -> Color(0x22000000)
                }
            )
            .clip(shape)
            .background(
                brush = when {
                    isDestructive -> Brush.horizontalGradient(
                        colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                    )
                    isPrimary -> Brush.horizontalGradient(
                        colors = listOf(Color(0xFF7C3AED), Color(0xFF2563EB))
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(Color(0xFFF3F4F6), Color(0xFFE5E7EB))
                    )
                }
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = when {
                        isDestructive -> listOf(Color(0xFFFCA5A5), Color(0x44EF4444))
                        isPrimary -> listOf(Color(0x80FFFFFF), Color(0x33A78BFA))
                        else -> listOf(Color(0x807C3AED), Color(0x44A78BFA))
                    }
                ),
                shape = shape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary || isDestructive) Color.White else PurplePrimary,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (isPrimary || isDestructive) Color.White else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/**
 * Animated Number Counter for Balance displaying smooth counting animation from 0 to target value
 */
@Composable
fun AnimatedBalanceCounter(
    targetAmount: Double,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$"
) {
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(targetAmount) {
        animatedValue.animateTo(
            targetValue = targetAmount.toFloat(),
            animationSpec = tween(
                durationMillis = 1400,
                easing = FastOutSlowInEasing
            )
        )
    }

    val formatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Text(
        text = "$currencySymbol${formatter.format(animatedValue.value)}",
        style = MaterialTheme.typography.headlineLarge.copy(
            color = TextPrimary,
            fontSize = 32.sp
        ),
        modifier = modifier.testTag("balance_counter_text")
    )
}
