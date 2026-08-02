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

                if (isPagoXMovil || isEnZona) {
                    processSms(context, sender, body, timestamp, isEnZona)
                }
            }
        }
    }

    private fun processSms(context: Context, sender: String, body: String, timestamp: Long, isEnZona: Boolean) {
        val amountPattern = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:CUP)?")
        val matcher = amountPattern.matcher(body)
        var amount: Double? = null
        if (matcher.find()) {
            amount = matcher.group(1)?.toDoubleOrNull()
        }

        if (amount == null || amount <= 0) {
            Log.d("SmsReceiver", "No valid amount found in SMS, ignoring.")
            return
        }

        val lowerBody = body.lowercase()
        val tipo = if (lowerBody.contains("recibido") || lowerBody.contains("recibi") || lowerBody.contains("ingreso")) {
            "ingreso"
        } else {
            "gasto"
        }

        val paymentMethod = if (isEnZona) "EnZona" else "Transfermóvil"

        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dateFormater = SimpleDateFormat("HH:mm", Locale.getDefault())
        val hora = dateFormater.format(calendar.time)

        val category = if (tipo == "ingreso") "Ventas" else "Servicios"
        val description = "SMS Auto: $body"

        val transaction = Transaction(
            tipo = tipo,
            monto = amount,
            categoria = category,
            descripcion = description,
            fecha = timestamp,
            hora = hora,
            metodo_pago = paymentMethod,
            es_empleador = false
        )

        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.transactionDao().insertTransaction(transaction)
                showNotification(context, tipo, amount, paymentMethod)
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error saving transaction from SMS", e)
            }
        }
    }

    private fun showNotification(context: Context, tipo: String, amount: Double, method: String) {
        val channelId = "sms_transactions"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Transacciones Automáticas SMS",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de transacciones detectadas por SMS"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (tipo == "ingreso") "¡Nuevo Ingreso Registrado!" else "¡Nuevo Gasto Registrado!"
        val content = "Se registró un $tipo de $amount CUP vía $method automáticamente."

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
