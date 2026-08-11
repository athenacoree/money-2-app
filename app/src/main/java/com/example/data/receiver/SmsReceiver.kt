package com.example.data.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.local.AppDatabase
import com.example.data.model.Transaction
import com.example.data.model.Configuracion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val sender = message.displayOriginatingAddress ?: ""
                val body = message.displayMessageBody ?: ""
                val timestamp = message.timestampMillis

                Log.d("SmsReceiver", "Received SMS from $sender: $body")

                val isPagoXMovil = sender.equals("PAGOxMOVIL", ignoreCase = true)
                val isEnZona = sender.contains("EnZona", ignoreCase = true) || body.contains("EnZona", ignoreCase = true)
                val isCubacel = sender.equals("CUBACEL", ignoreCase = true) ||
                                sender.equals("ETECSA", ignoreCase = true) ||
                                body.contains("ETECSA", ignoreCase = true) ||
                                body.contains("Cubacel", ignoreCase = true)

                if (isPagoXMovil || isEnZona) {
                    processSms(context, sender, body, timestamp, isEnZona)
                } else if (isCubacel) {
                    processCubacelSms(context, sender, body, timestamp)
                }
            }
        }
    }

    private fun processCubacelSms(context: Context, sender: String, body: String, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val parsed = CubacelMessageParser.parseMessage(body, timestamp, context)
                val db = AppDatabase.getDatabase(context)

                if (parsed.tipo == "saldo_principal" && parsed.saldoCUP > 0.0) {
                    db.configuracionDao().insertConfiguracion(Configuracion(clave = "etecsa_saldo_cup", valor = parsed.saldoCUP.toString()))
                    db.configuracionDao().insertConfiguracion(Configuracion(clave = "etecsa_vencimiento", valor = parsed.fechaVencimiento))
                }

                // Show balance/promo notification
                val title = when (parsed.tipo) {
                    "saldo_principal" -> "¡Saldo Cubacel Actualizado!"
                    "bono_datos" -> "¡Paquete de Datos Cubacel!"
                    "promocion" -> "¡Nueva Promoción ETECSA!"
                    else -> "Información de Cubacel"
                }

                val content = when (parsed.tipo) {
                    "saldo_principal" -> "Saldo: ${parsed.saldoCUP} CUP, vence: ${parsed.fechaVencimiento}"
                    "bono_datos" -> "Datos: ${parsed.datosMB} MB LTE, Bono: ${parsed.bonoDatosMB} MB"
                    "promocion" -> parsed.descripcion.take(80) + "..."
                    else -> parsed.descripcion.take(80)
                }

                showNotification(context, title, content, "cubacel_balance")
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error processing Cubacel SMS", e)
            }
        }
    }

    private fun processSms(context: Context, sender: String, body: String, timestamp: Long, isEnZona: Boolean) {
        var tipo = "gasto"
        var amount: Double? = null
        var currency = "CUP"
        var isLowConfidence = false

        // 1. Run the TFLite model first
        try {
            val parser = SmsModelParser(context)
            val result = parser.parseMessage(body)
            parser.close()

            if (result.tipo != "sin_clasificar") {
                tipo = result.tipo
                currency = result.moneda
                isLowConfidence = (result.confianza == "baja")
                if (result.monto > 0.0) {
                    amount = result.monto
                }
            } else {
                isLowConfidence = true
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error running SmsModelParser, falling back to regex", e)
            isLowConfidence = true
        }

        // 2. Fallback to Regex if amount is not found or model parsing threw exception
        if (amount == null) {
            val amountPattern = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:CUP|MLC|USD)?")
            val matcher = amountPattern.matcher(body)
            if (matcher.find()) {
                amount = matcher.group(1)?.toDoubleOrNull()
            }

            val lowerBody = body.lowercase()
            tipo = if (lowerBody.contains("recibido") || lowerBody.contains("recibi") || lowerBody.contains("ingreso")) {
                "ingreso"
            } else {
                "gasto"
            }
        }

        // 3. Validation and anti-hallucination check
        if (amount == null || amount <= 0.0) {
            Log.d("SmsReceiver", "No valid amount found in SMS, ignoring.")
            return
        }

        // Check if amount actually exists in body text (anti-hallucination)
        val cleanAmountStr = amount.toString()
        val cleanAmountIntStr = amount.toInt().toString()
        val inBody = body.contains(cleanAmountStr) || body.contains(cleanAmountIntStr) || body.replace(",", ".").contains(cleanAmountStr)
        if (!inBody) {
            Log.w("SmsReceiver", "Anti-hallucination failed: parsed amount $amount not found in original SMS text. Forcing low confidence.")
            isLowConfidence = true
        }

        val paymentMethod = if (isEnZona) "EnZona" else "Transfermóvil"

        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dateFormater = SimpleDateFormat("HH:mm", Locale.getDefault())
        val hora = dateFormater.format(calendar.time)

        val category = if (tipo == "ingreso") "Ventas" else "Servicios"
        val description = "SMS Auto: $body"

        // If confidence is low or anti-hallucination failed, trigger manual classification notification and DO NOT auto-insert
        if (isLowConfidence) {
            Log.d("SmsReceiver", "Transaction parsed with low confidence or failed validation, showing manual classification notification.")
            showManualClassificationNotification(context, amount, currency, paymentMethod)
            return
        }

        val transaction = Transaction(
            tipo = tipo,
            monto = amount,
            categoria = category,
            descripcion = description,
            fecha = timestamp,
            hora = hora,
            metodo_pago = paymentMethod,
            es_empleador = false,
            moneda = currency
        )

        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.transactionDao().insertTransaction(transaction)
                showNotification(context, "Transacción Registrada", "Se registró un $tipo de $amount $currency vía $paymentMethod automáticamente.", "sms_transactions")
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error saving transaction from SMS", e)
            }
        }
    }

    private fun showManualClassificationNotification(context: Context, amount: Double, currency: String, method: String) {
        showNotification(
            context,
            "Transacción por Clasificar",
            "Se detectó un posible movimiento de $amount $currency vía $method. Toca para clasificar manualmente.",
            "sms_transactions"
        )
    }

    private fun showNotification(context: Context, title: String, content: String, channelId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Transacciones y Saldo Automáticos SMS",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de transacciones y saldos detectados por SMS"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
