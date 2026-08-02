package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
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
import com.example.data.model.AppMode
import com.example.ui.components.BackgroundGradientCanvas
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MoneyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategoryCatalogItem(
    val name: String,
    val iconEmoji: String,
    val color: Color
)

val CATEGORY_CATALOG = listOf(
    CategoryCatalogItem("Comida", "🍽️", Color(0xFFF97316)),
    CategoryCatalogItem("Transporte", "🚗", Color(0xFFF59E0B)),
    CategoryCatalogItem("Salario", "💼", Color(0xFF10B981)),
    CategoryCatalogItem("Ventas", "🛒", Color(0xFF34D399)),
    CategoryCatalogItem("Servicios", "📦", Color(0xFF3B82F6)),
    CategoryCatalogItem("Educación", "📚", Color(0xFF6366F1)),
    CategoryCatalogItem("Salud", "🏥", Color(0xFFEF4444)),
    CategoryCatalogItem("Entretenimiento", "🎬", Color(0xFFEC4899)),
    CategoryCatalogItem("Ahorros", "💰", Color(0xFFEAB308)),
    CategoryCatalogItem("Otros", "📌", Color(0xFF6B7280))
)

@Composable
fun AddTransactionScreen(
    viewModel: MoneyViewModel,
    initialTypeString: String, // "ingreso" o "gasto"
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appMode by viewModel.appMode.collectAsState()

    AddTransactionScreen(
        initialTypeString = initialTypeString,
        appMode = appMode,
        onDismiss = onBack,
        onSave = { category, description, amount, paymentMethod, isBusiness ->
            val cal = Calendar.getInstance()
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            viewModel.addTransaction(
                monto = amount,
                categoria = category,
                descripcion = description,
                fecha = cal.timeInMillis,
                hora = format.format(cal.time),
                metodoPago = paymentMethod,
                esEmpleador = isBusiness,
                tipo = initialTypeString
            )
            onBack()
        },
        modifier = modifier
    )
}

@Composable
fun AddTransactionScreen(
    initialTypeString: String,
    appMode: AppMode,
    onDismiss: () -> Unit,
    onSave: (category: String, description: String, amount: Double, method: String, isBusiness: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CATEGORY_CATALOG.first()) }
    var selectedPaymentMethod by remember { mutableStateOf("Transfermóvil") }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    val inBusinessMode = (appMode == AppMode.WORK_EMPLOYER || appMode == AppMode.WORK_EMPLOYEE)
    var isBusinessTransaction by remember { mutableStateOf(inBusinessMode) }

    val isIncome = initialTypeString == "ingreso"
    val titleText = if (isIncome) "Agregar Ingreso" else "Agregar Gasto"
    val accentColor = if (isIncome) PurplePrimary else ExpenseRed

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale.getDefault()) }

    BackgroundGradientCanvas(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.testTag("add_tx_screen_title")
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("add_tx_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Glass Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Campo Monto
                    Text(
                        text = "Monto",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = { Text("\$0.00", fontSize = 28.sp, color = TextSecondary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontSize = 28.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_tx_amount_input"),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = GlassCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Catálogo de Categorías (Grid)
                    Text(
                        text = "Seleccionar Categoría",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(CATEGORY_CATALOG) { item ->
                            val isSelected = selectedCategory == item
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color(0x0FFFFFFF))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) accentColor else GlassBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedCategory = item }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(item.color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = item.iconEmoji, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) accentColor else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Campo Descripción
                    Text(
                        text = "Descripción",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        placeholder = { Text("Escribe una descripción...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_tx_description_input"),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = GlassCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Selector Fecha y Hora
                    Text(
                        text = "Fecha y hora",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, GlassCardBorder, RoundedCornerShape(18.dp))
                            .background(Color(0x0FFFFFFF))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateFormatter.format(Date(selectedTimestamp)),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Fecha",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Método de pago
                    Text(
                        text = "Método de pago",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Transfermóvil", "EnZona", "Efectivo", "Otro").forEach { method ->
                            val isSelected = selectedPaymentMethod == method
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) accentColor else Color(0x1F7C3AED))
                                    .clickable { selectedPaymentMethod = method }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = method,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!inBusinessMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { isBusinessTransaction = !isBusinessTransaction }
                        ) {
                            Checkbox(
                                checked = isBusinessTransaction,
                                onCheckedChange = { isBusinessTransaction = it },
                                colors = CheckboxDefaults.colors(checkedColor = accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Es del negocio (Trabajo)",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // Action Buttons
                    GlassButton(
                        text = "Guardar",
                        isPrimary = true,
                        onClick = {
                            val amountVal = amountText.toDoubleOrNull() ?: 0.0
                            if (amountVal > 0) {
                                onSave(
                                    selectedCategory.name,
                                    descriptionText.ifBlank { "Sin descripción" },
                                    amountVal,
                                    selectedPaymentMethod,
                                    isBusinessTransaction
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_transaction_btn")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDismiss() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancelar",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
