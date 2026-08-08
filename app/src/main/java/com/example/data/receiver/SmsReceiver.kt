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
import com.example.data.model.SaldoMovil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    companion object {
        val pendingTransferAlert = kotlinx.coroutines.flow.MutableSharedFlow<Transaction>(extraBufferCapacity = 1)
        val employeeConfirmationSmsReceived = kotlinx.coroutines.flow.MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 1)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val sender = message.displayOriginatingAddress ?: ""
                val body = message.displayMessageBody ?: ""
                val timestamp = message.timestampMillis

                Log.d("SmsReceiver", "Received SMS from $sender: $body")

                if (body.startsWith("MONEYAPP-CONF|")) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getDatabase(context)
                        val employerPhone = db.configuracionDao().getConfiguracionByKey("telefono_empleador")?.valor ?: ""

                        // Robust comparison of sender and employerPhone
                        val cleanSender = sender.replace("[^0-9]".toRegex(), "").takeLast(8)
                        val cleanEmployer = employerPhone.replace("[^0-9]".toRegex(), "").takeLast(8)

                        if (cleanEmployer.isNotEmpty() && cleanSender == cleanEmployer) {
                            Log.d("SmsReceiver", "Accepted MONEYAPP-CONF from Employer $sender")
                            // Parse message body
                            val parts = body.split("|")
                            val payload = mutableMapOf<String, String>()
                            payload["sender"] = sender
                            for (part in parts) {
                                val key = part.substringBefore(":")
                                val value = part.substringAfter(":")
                                if (key.isNotEmpty() && value.isNotEmpty()) {
                                    payload[key] = value
                                }
                            }
                            employeeConfirmationSmsReceived.emit(payload)
                        } else {
                            Log.d("SmsReceiver", "Rejected MONEYAPP-CONF from unauthorized sender $sender (Employer phone is $employerPhone)")
                        }
                    }
                    continue // Don't parse as regular transaction
                }

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
                val parsed = CubacelMessageParser.parseMessage(body, timestamp)
                val db = AppDatabase.getDatabase(context)
                db.saldoMovilDao().insertSaldoMovil(parsed)

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
        // 1. Extract and ignore phone number
        var cleanedBody = body
        val phoneRegex = Pattern.compile("(?i)(?:\\+53\\s*)?\\b([56]\\d{7})\\b")
        val phoneMatcher = phoneRegex.matcher(body)
        var extractedPhone: String? = null
        if (phoneMatcher.find()) {
            extractedPhone = phoneMatcher.group(1)
            cleanedBody = phoneMatcher.replaceAll("[PHONE]")
        }

        // 2. Extract amount next to "CUP" or "MLC"
        val amountCurrencyRegex = Pattern.compile("(?i)(?:(CUP|MLC)\\s*(\\d+(?:\\.\\d+)?))|(?:(\\d+(?:\\.\\d+)?)\\s*(CUP|MLC))")
        val acMatcher = amountCurrencyRegex.matcher(cleanedBody)
        var amount: Double? = null
        var currency = "CUP"
        if (acMatcher.find()) {
            if (acMatcher.group(2) != null) {
                amount = acMatcher.group(2)?.toDoubleOrNull()
                currency = acMatcher.group(1)?.uppercase() ?: "CUP"
            } else if (acMatcher.group(3) != null) {
                amount = acMatcher.group(3)?.toDoubleOrNull()
                currency = acMatcher.group(4)?.uppercase() ?: "CUP"
            }
        }

        if (amount == null || amount <= 0) {
            Log.d("SmsReceiver", "No valid amount found next to currency in SMS, triggering manual review notification.")
            showNotification(context, "SMS de Pago Recibido", "Recibiste un SMS de pago pero no pudimos identificar el monto exacto con confianza. Toca para registrarlo manualmente.", "sms_transactions")
            return
        }

        // 3. Classify incoming vs outgoing based on detailed structure
        val lowerBody = body.lowercase()
        val tipo = when {
            // Incoming transfers/payments/deposits
            lowerBody.contains("recibido") ||
            lowerBody.contains("recibi") ||
            lowerBody.contains("ingreso") ||
            lowerBody.contains("acreditado") ||
            lowerBody.contains("acred") ||
            lowerBody.contains("depósito") ||
            lowerBody.contains("deposito") -> "ingreso"

            // Outgoing transfers/payments/purchases
            lowerBody.contains("realizado") ||
            lowerBody.contains("envió") ||
            lowerBody.contains("envio") ||
            lowerBody.contains("pagado") ||
            lowerBody.contains("pago realizado") ||
            lowerBody.contains("debited") ||
            lowerBody.contains("debitado") ||
            lowerBody.contains("db") -> "gasto"

            else -> "sin_clasificar" // Unclassified
        }

        val paymentMethod = if (isEnZona) "EnZona" else "Transfermóvil"

        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dateFormater = SimpleDateFormat("HH:mm", Locale.getDefault())
        val hora = dateFormater.format(calendar.time)

        val category = if (tipo == "ingreso") "Ventas" else if (tipo == "gasto") "Servicios" else "Sin clasificar"

        // Append phone to description if found
        val description = if (extractedPhone != null) {
            "SMS Auto: $body [Tel Ref: $extractedPhone]"
        } else {
            "SMS Auto: $body"
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
                // Check duplicate within 5 seconds window
                val minTime = timestamp - 5000
                val maxTime = timestamp + 5000
                val exists = db.transactionDao().countTransactionsNearTime(amount, paymentMethod, tipo, minTime, maxTime) > 0
                if (exists) {
                    Log.d("SmsReceiver", "Duplicate transaction detected (same amount $amount, method $paymentMethod, type $tipo near $timestamp), ignoring.")
                    return@launch
                }

                val isEmployerMode = db.configuracionDao().getConfiguracionByKey("modo_empleador_activo")?.valor?.toBoolean() ?: false
                val finalTx = transaction.copy(es_empleador = isEmployerMode)

                db.transactionDao().insertTransaction(finalTx)

                if (isEmployerMode && tipo == "ingreso") {
                    Log.d("SmsReceiver", "Employer mode active, triggering pendingTransferAlert for income transaction.")
                    pendingTransferAlert.emit(finalTx)
                }

                val titleText = if (tipo == "sin_clasificar") "Confirmar Transacción Recibida" else "Nueva Transacción Registrada"
                val descText = if (tipo == "sin_clasificar") "Recibiste un SMS de pago de $amount $currency. Toca para clasificar como Ingreso o Gasto." else "Se registró un $tipo de $amount $currency vía $paymentMethod automáticamente."
                showNotification(context, titleText, descText, "sms_transactions")
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error saving transaction from SMS", e)
            }
        }
    }

    private fun showNotification(context: Context, title: String, content: String, channelId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Transacciones y Saldo Automáticos SMS",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de transacciones y saldos de Cubacel/ETECSA detectados por SMS"
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
