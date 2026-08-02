package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.AppMode
import com.example.data.model.Producto
import com.example.data.model.AuditoriaStock
import com.example.ui.dialogs.AdjustStockDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.example.ui.components.PeekPreviewType
import com.example.ui.components.BackgroundGradientCanvas
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MoneyViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CatalogScreen(
    viewModel: MoneyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appMode by viewModel.appMode.collectAsState()
    val products by viewModel.products.collectAsState()
    val auditorias by viewModel.auditorias.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val selectedProductForStockAdjustment by viewModel.selectedProductForStockAdjustment.collectAsState()

    var showProposalDialog by remember { mutableStateOf(false) }

    CatalogScreen(
        appMode = appMode,
        products = products,
        auditorias = auditorias,
        cart = cart,
        cartTotal = cartTotal,
        onAddProductClick = { viewModel.showAddProductDialog.value = true },
        onEditProduct = { product ->
            viewModel.selectedProductForEdit.value = product
            viewModel.showAddProductDialog.value = true
        },
        onDeleteProduct = { product -> viewModel.deleteProduct(product) },
        onAdjustStock = { product -> viewModel.selectedProductForStockAdjustment.value = product },
        onAddToCart = { product -> viewModel.addProductToCart(product) },
        onRemoveFromCart = { product -> viewModel.removeProductFromCart(product) },
        onCheckout = { viewModel.finalizeSale() },
        onOpenProposalDialog = { showProposalDialog = true },
        onProductLongClick = { product -> viewModel.openPeekPreview(PeekPreviewType.ProductDetail(product)) },
        onBack = onBack,
        modifier = modifier
    )

    if (showProposalDialog) {
        ProposeProductDialog(
            onDismiss = { showProposalDialog = false },
            onSubmit = { name, price, stock, justification ->
                viewModel.submitEmployeeProposal(null, name, price, stock, justification)
                showProposalDialog = false
            }
        )
    }

    selectedProductForStockAdjustment?.let { product ->
        val userRole = if (appMode == AppMode.WORK_EMPLOYER) "Empleador" else "Empleado"
        AdjustStockDialog(
            product = product,
            userRole = userRole,
            onDismiss = { viewModel.selectedProductForStockAdjustment.value = null },
            onSave = { newStock, justification ->
                viewModel.adjustStock(product, newStock, justification)
            }
        )
    }
}

@Composable
fun AddProductDialog(
    viewModel: MoneyViewModel,
    onDismiss: () -> Unit
) {
    val editingProduct by viewModel.selectedProductForEdit.collectAsState()

    AddProductDialog(
        initialProduct = editingProduct,
        onDismiss = onDismiss,
        onSave = { name, price, stock, imageUri ->
            viewModel.saveProduct(name, price, stock, imageUri)
        }
    )
}

@Composable
fun CatalogScreen(
    appMode: AppMode,
    products: List<Producto>,
    auditorias: List<AuditoriaStock> = emptyList(),
    cart: Map<Producto, Int>,
    cartTotal: Double,
    onAddProductClick: () -> Unit,
    onEditProduct: (Producto) -> Unit,
    onDeleteProduct: (Producto) -> Unit,
    onAdjustStock: (Producto) -> Unit = {},
    onAddToCart: (Producto) -> Unit,
    onRemoveFromCart: (Producto) -> Unit,
    onCheckout: () -> Unit,
    onOpenProposalDialog: () -> Unit = {},
    onProductLongClick: ((Producto) -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val isEmployer = appMode == AppMode.WORK_EMPLOYER
    var showCartDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Catálogo, 1 = Auditoría & Justificaciones

    BackgroundGradientCanvas(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Catálogo de Productos",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = TextPrimary
                            ),
                            modifier = Modifier.testTag("catalog_screen_title")
                        )
                    }

                    // Cart Icon (Solo para empleados si tienen productos)
                    if (!isEmployer) {
                        IconButton(
                            onClick = { showCartDialog = true },
                            modifier = Modifier.testTag("cart_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (cart.isNotEmpty()) {
                                        Badge { Text(cart.values.sum().toString()) }
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Carrito")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isEmployer) "Modo Empleador: Administra inventario y aprueba propuestas" else "Modo Empleado: Vende o propone cambios a la rama de revisión",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de pestañas: "Catálogo" vs "Auditoría de Stock"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1F7C3AED))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == 0) PurplePrimary else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Catálogo (${products.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (selectedTab == 0) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTab == 1) PurplePrimary else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color.White else PurplePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auditorías (${auditorias.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (selectedTab == 1) Color.White else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // PESTAÑA CATÁLOGO
                    if (products.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No hay productos en el catálogo.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(products) { product ->
                                ProductCardItem(
                                    product = product,
                                    isEmployer = isEmployer,
                                    currencyFormatter = currencyFormatter,
                                    onEdit = { onEditProduct(product) },
                                    onDelete = { onDeleteProduct(product) },
                                    onAdjustStock = { onAdjustStock(product) },
                                    onClick = {
                                        if (!isEmployer) {
                                            onAddToCart(product)
                                        }
                                    },
                                    onLongClick = {
                                        onProductLongClick?.invoke(product)
                                    }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(90.dp)) }
                        }
                    }
                } else {
                    // PESTAÑA AUDITORÍA Y JUSTIFICACIONES
                    AuditHistoryView(auditorias = auditorias)
                }
            }

            // Floating action button "+" (Empleador agrega producto, Empleado propone producto)
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = if (isEmployer) onAddProductClick else onOpenProposalDialog,
                    containerColor = PurplePrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 90.dp, end = 20.dp)
                        .testTag("catalog_action_fab")
                ) {
                    Icon(
                        imageVector = if (isEmployer) Icons.Default.Add else Icons.Default.Send,
                        contentDescription = if (isEmployer) "Agregar producto" else "Proponer cambio"
                    )
                }
            }
        }
    }

    // Dialogo del Carrito (Cart) para el Empleado
    if (showCartDialog) {
        Dialog(onDismissRequest = { showCartDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Carrito de Ventas",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (cart.isEmpty()) {
                        Text("El carrito está vacío.", color = TextSecondary)
                    } else {
                        cart.forEach { (prod, qty) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.nombre, fontWeight = FontWeight.Bold)
                                    Text("${currencyFormatter.format(prod.precio)} x $qty")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onRemoveFromCart(prod) }) {
                                        Icon(Icons.Default.Remove, "Quitar")
                                    }
                                    Text("$qty", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { onAddToCart(prod) }) {
                                        Icon(Icons.Default.Add, "Agregar")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(currencyFormatter.format(cartTotal), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PurplePrimary)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        GlassButton(
                            text = "Finalizar venta",
                            isPrimary = true,
                            onClick = {
                                onCheckout()
                                showCartDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().testTag("checkout_btn")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showCartDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductCardItem(
    product: Producto,
    isEmployer: Boolean,
    currencyFormatter: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            )
            .testTag("product_card_${product.id}"),
        cornerRadius = 20.dp,
        backgroundColor = Color(0xF5FFFFFF),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Imagen del producto o Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6),
                                Color(0xFFA78BFA),
                                Color(0xFFDDD6FE)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imagen_uri.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imagen_uri,
                        contentDescription = product.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = product.nombre,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                if (!isEmployer) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Agregar",
                            tint = PurplePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nombre del producto (16sp, semibold)
            Text(
                text = product.nombre,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = TextPrimary
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Precio (16sp, morado)
            Text(
                text = currencyFormatter.format(product.precio),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PurplePrimary
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Stock (13sp, gris)
            Text(
                text = "Stock: ${product.stock} u.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón "Ajustar Stock" disponible para empleador y empleado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x1F7C3AED))
                        .clickable { onAdjustStock() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Ajustar Stock",
                            tint = PurplePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ajustar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                    }
                }

                if (isEmployer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = PurplePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = ExpenseRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// VISTA DE HISTORIAL DE AUDITORÍA Y JUSTIFICACIONES
// -------------------------------------------------------------
@Composable
fun AuditHistoryView(
    auditorias: List<AuditoriaStock>,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    if (auditorias.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Sin registros de auditoría aún.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cualquier ajuste de stock o fusión de ramas quedará auditado aquí.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(auditorias) { audit ->
                ExpandableAuditCard(audit = audit, sdf = sdf)
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }
}

@Composable
fun ExpandableAuditCard(
    audit: AuditoriaStock,
    sdf: SimpleDateFormat
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        cornerRadius = 18.dp,
        backgroundColor = Color(0xF5FFFFFF),
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (audit.es_fusion_rama) PurplePrimary.copy(alpha = 0.15f) else if (audit.cambio_stock >= 0) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (audit.es_fusion_rama) Icons.Default.MergeType else if (audit.cambio_stock >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (audit.es_fusion_rama) PurplePrimary else if (audit.cambio_stock >= 0) IncomeGreen else ExpenseRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = audit.nombre_producto,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = sdf.format(Date(audit.timestamp)),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                val diffStr = if (audit.cambio_stock > 0) "+${audit.cambio_stock}" else "${audit.cambio_stock}"
                val badgeBg = if (audit.es_fusion_rama) PurplePrimary.copy(alpha = 0.15f) else if (audit.cambio_stock >= 0) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
                val badgeTextColor = if (audit.es_fusion_rama) PurplePrimary else if (audit.cambio_stock >= 0) IncomeGreen else ExpenseRed

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = diffStr,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeTextColor,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expandir auditoría",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stock: ${audit.stock_anterior} ➔ ${audit.stock_resultante} u.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1F7C3AED))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Por: ${audit.realizado_por}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // CONTENIDO EXPANDIBLE DE NOTIFICACIÓN DETALLADA DE AUDITORÍA
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = GlassCardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0F7C3AED))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Notificación de Auditoría / Cambios de Rama:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PurplePrimary
                    )

                    if (!audit.dado_por_empleado.isNullOrBlank()) {
                        Text(
                            text = "• Dado por empleado: ${audit.dado_por_empleado}",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }

                    if (!audit.cambiado_por_empleador.isNullOrBlank()) {
                        Text(
                            text = "• Modificado por empleador: ${audit.cambiado_por_empleador}",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "• Justificación: \"${audit.justificacion}\"",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    if (audit.es_fusion_rama) {
                        Text(
                            text = "• Estado: Fusionado con la rama principal.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProposeProductDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, price: Double, stock: Int, justification: String) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("") }
    var justificationText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            cornerRadius = 24.dp,
            backgroundColor = Color(0xF5FFFFFF)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Proponer Cambio de Rama (Empleado)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "El empleador revisará esta propuesta en su rama de revisión antes de aplicarla.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Nombre del Producto") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Precio Propuesto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock Propuesto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = justificationText,
                    onValueChange = { justificationText = it },
                    label = { Text("Justificación del Cambio") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                GlassButton(
                    text = "Enviar a Rama de Revisión",
                    icon = Icons.Default.Send,
                    isPrimary = true,
                    onClick = {
                        val p = priceText.toDoubleOrNull() ?: 0.0
                        val s = stockText.toIntOrNull() ?: 0
                        if (nameText.isNotBlank()) {
                            onSubmit(nameText, p, s, justificationText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("send_proposal_btn")
                )

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Cancelar")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PANTALLA DE AGREGAR PRODUCTO (SOLO EMPLEADOR)
// -------------------------------------------------------------
@Composable
fun AddProductDialog(
    initialProduct: Producto? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, price: Double, stock: Int, imageUri: String?) -> Unit
) {
    var nameText by remember { mutableStateOf(initialProduct?.nombre ?: "") }
    var priceText by remember { mutableStateOf(initialProduct?.precio?.toString() ?: "") }
    var stockText by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "") }
    var selectedImageUri by remember { mutableStateOf<String?>(initialProduct?.imagen_uri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it.toString() }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("add_product_dialog"),
            cornerRadius = 24.dp,
            backgroundColor = Color(0xF5FFFFFF),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (initialProduct == null) "Agregar Producto" else "Editar Producto",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Nombre del producto") },
                    placeholder = { Text("Nombre...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = GlassCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Precio") },
                    placeholder = { Text("\$0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_price_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = GlassCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text("Cantidad en stock") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_stock_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = GlassCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Campo Foto: Visualización previa y botón seleccionar imagen
                if (!selectedImageUri.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, GlassCardBorder, RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Vista previa",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1F7C3AED))
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedImageUri.isNullOrBlank()) "Subir foto del producto" else "Cambiar foto",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                GlassButton(
                    text = "Guardar",
                    onClick = {
                        val p = priceText.toDoubleOrNull() ?: 0.0
                        val s = stockText.toIntOrNull() ?: 0
                        if (nameText.isNotBlank()) {
                            onSave(nameText, p, s, selectedImageUri)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_product_btn")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancelar",
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
