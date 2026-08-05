package com.example.data.model

data class DynamicIconBadge(
    val id: String,
    val title: String,
    val symbol: String,
    val category: String,
    val colorHex: String,
    val description: String
)

object DynamicIconCatalog {
    val ALL_ICONS = listOf(
        DynamicIconBadge("TREND_UP_1", "+1% Subida Verde", "📈 +1%", "Tendencia", "#10B981", "Muestra gráfica ascendente de +1% en el saldo"),
        DynamicIconBadge("TREND_UP_5", "+5% Subida Fuerte", "🚀 +5%", "Tendencia", "#059669", "Gráfica de crecimiento aceleredao de +5%"),
        DynamicIconBadge("TREND_UP_10", "+10% Máximo Crecimiento", "💎 +10%", "Tendencia", "#047857", "Superávit financiero con icono de diamante y gráfica verde"),
        DynamicIconBadge("TREND_DOWN_1", "-1% Ajuste de Gasto", "📉 -1%", "Tendencia", "#EF4444", "Gráfica descendente suave de -1%"),
        DynamicIconBadge("TREND_DOWN_5", "-5% Caída Temporal", "🔻 -5%", "Tendencia", "#DC2626", "Aviso de reducción de saldo -5%"),
        DynamicIconBadge("QVAPAY_SQP", "Pago QvaPay Recibido", "⚡ SQP", "QvaPay", "#7C3AED", "Notificación instantánea de ingreso QvaPay SQP"),
        DynamicIconBadge("TRANSFERMOVIL_CUP", "SMS Transfermóvil", "📱 CUP", "Local", "#2563EB", "Confirmación de transferencia bancaria por SMS"),
        DynamicIconBadge("CRYPTO_USDT", "Tether USDT TRC20", "💵 USDT", "Cripto", "#0D9488", "Cobro o depósito en moneda estable USDT"),
        DynamicIconBadge("CRYPTO_BTC", "Bitcoin Red Lightning", "🪙 BTC", "Cripto", "#D97706", "Transacción procesada en Bitcoin BTC"),
        DynamicIconBadge("BUSINESS_SALE", "Venta en Catálogo", "🛍️ Venta", "Negocio", "#8B5CF6", "Ingreso registrado por venta de productos"),
        DynamicIconBadge("SECURITY_LOCKED", "AURA Guard Activo", "🔒 Safe", "Seguridad", "#6366F1", "Protección PIN/Biométrica verificada"),
        DynamicIconBadge("SAVINGS_GOAL", "Meta Alcanzada", "🎯 Meta", "Ahorros", "#10B981", "Objetivo de fondo de ahorro completado"),
        DynamicIconBadge("INVOICE_GENERATED", "Factura QvaPay QR", "📄 QR", "QvaPay", "#9333EA", "Enlace de cobro para clientes generado"),
        DynamicIconBadge("SMS_AUTO_READ", "Lector SMS Banco", "📩 Banco", "Local", "#1D4ED8", "Notificación bancaria parseda localmente"),
        DynamicIconBadge("MERCHANT_POS", "Terminal Cobro POS", "💳 POS", "Negocio", "#3B82F6", "Cobro registrado en terminal de punto de venta"),
        DynamicIconBadge("OFFLINE_SYNC", "Caché Sincronizada", "☁️ Sinc", "Sistema", "#059669", "Datos respaldados y listos sin conexión"),
        DynamicIconBadge("RANKING_LEADER", "Líder de Eficiencia", "👑 Top", "Logros", "#EAB308", "Rango oro en control presupuestario"),
        DynamicIconBadge("PEEK_PREVIEW", "Vistas 3D Touch", "👁️ Peek", "Sistema", "#7C3AED", "Previsualización rápida de transacciones activa"),
        DynamicIconBadge("P2P_TRANSFER", "Envío P2P Exitoso", "🔄 P2P", "QvaPay", "#6D28D9", "Transferencia directa entre usuarios completada"),
        DynamicIconBadge("AUDIT_STOCK", "Auditoría de Stock", "📋 Stock", "Negocio", "#F59E0B", "Ajuste e inventario de productos actualizado"),
        DynamicIconBadge("NFC_PAYMENT", "Pago Contactless NFC", "📡 NFC", "Local", "#2563EB", "Cobro aproximado por tarjeta contactless"),
        DynamicIconBadge("TAX_REPORT", "Reporte Fiscal Listo", "📊 Fiscal", "Reportes", "#4B5563", "Cierre contable y resumen exportado"),
        DynamicIconBadge("DISTRIBUTOR_DISPATCH", "Despacho Entregado", "🚛 Cargo", "Negocio", "#D97706", "Carga de mercancía recibida por cliente"),
        DynamicIconBadge("GIFT_BONUS", "Recompensa / Bono", "🎁 Bono", "Ingresos", "#EC4899", "Ingreso extraordinario o comisión añadida"),
        DynamicIconBadge("NIGHT_MODE", "Modo Noche Ahorro", "🌙 Noche", "Sistema", "#374151", "Visualización segura con contraste nocturno"),
        DynamicIconBadge("STABLE_BALANCE", "Saldo Balanceado", "⚖️ Estable", "Tendencia", "#10B981", "Equilibrio financiero perfecto sin variaciones bruscas"),
        DynamicIconBadge("HIGH_ACCURACY", "Fórmula Precisa", "🎯 100%", "Sistema", "#0284C7", "Conciliación matemática libre de errores"),
        DynamicIconBadge("AURA_SHIELD", "Escudo Anti-Fraude", "🛡️ Shield", "Seguridad", "#4F46E5", "Verificación local de integridad financiera"),
        DynamicIconBadge("EMPLOYEE_SYNC", "Propuesta Aprobada", "📝 Aprobado", "Negocio", "#10B981", "Actualización de inventario autorizada"),
        DynamicIconBadge("MOTIVATION_STAR", "Frase Inspiradora", "⭐ Inspirar", "Emocional", "#F59E0B", "Mensaje motivacional de abundancia activado")
    )
}
