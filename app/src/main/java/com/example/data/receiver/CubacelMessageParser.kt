package com.example.data.receiver

import android.content.Context
import android.util.Log
import java.util.regex.Pattern

data class SaldoMovil(
    val id: Long = 0,
    val tipo: String, // "saldo_principal", "bono_datos", "promocion", "alerta_consumo"
    val saldoCUP: Double = 0.0,
    val datosMB: Double = 0.0,
    val bonoDatosMB: Double = 0.0,
    val fechaVencimiento: String = "",
    val descripcion: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object CubacelMessageParser {

    fun parseMessage(body: String, timestamp: Long, context: Context? = null): SaldoMovil {
        val lowerBody = body.lowercase()

        // Default outputs
        var saldoCUP = 0.0
        var datosMB = 0.0
        var bonoDatosMB = 0.0
        var fechaVencimiento = ""
        var tipo = "alerta_consumo"

        // Attempt Model Parsing first as instructed (using Model Parser)
        var modelResult: SmsModelResult? = null
        if (context != null) {
            try {
                val modelParser = SmsModelParser(context)
                modelResult = modelParser.parseMessage(body)
                modelParser.close()
            } catch (e: Exception) {
                Log.e("CubacelMessageParser", "Error running model fallback to regex.", e)
            }
        }

        // 1. Parse CUP Balance (Regex fallback)
        val balanceRegex = Pattern.compile("(?i)(?:saldo(?: principal)?(?: es(?: de)?)?\\s*[:=]?\\s*)(\\d+(?:\\.\\d+)?)\\s*(?:CUP|$)")
        val balMatcher = balanceRegex.matcher(body)
        if (balMatcher.find()) {
            saldoCUP = balMatcher.group(1)?.toDoubleOrNull() ?: 0.0
        } else {
            val cupFallback = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*CUP")
            val fallbackMatcher = cupFallback.matcher(body)
            if (fallbackMatcher.find()) {
                saldoCUP = fallbackMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            }
        }

        // Overwrite balance with model if model found it with high/medium confidence
        if (modelResult != null && modelResult.confianza != "baja" && modelResult.monto > 0.0 && lowerBody.contains("saldo")) {
            saldoCUP = modelResult.monto
        }

        // 2. Parse Data Packages (MB/GB) (Regex fallback)
        val dataRegex = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(GB|MB)\\s*(?:de\\s+)?(?:navegacion|datos|LTE|internacional)?")
        val dataMatcher = dataRegex.matcher(body)
        while (dataMatcher.find()) {
            val amount = dataMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            val unit = dataMatcher.group(2)?.uppercase() ?: "MB"
            val mbs = if (unit == "GB") amount * 1024.0 else amount

            // Context window of 70 characters
            val start = dataMatcher.start()
            val end = Math.min(body.length, dataMatcher.end() + 70)
            val contextText = body.substring(start, end).lowercase()

            val isNational = (contextText.contains("nacional") && !contextText.contains("internacional")) ||
                             contextText.contains("bono") ||
                             contextText.contains(".cu")

            if (isNational) {
                bonoDatosMB += mbs
            } else {
                datosMB += mbs
            }
        }

        // 3. Parse Expiration Date
        val dateRegex = Pattern.compile("(?i)(?:valido|validos|vence|vencen|expira|hasta|validez)\\s*(?:hasta|el)?\\s*(\\d{2}/\\d{2}/\\d{2,4})")
        val dateMatcher = dateRegex.matcher(body)
        if (dateMatcher.find()) {
            fechaVencimiento = dateMatcher.group(1) ?: ""
        }

        // 4. Classify Message Type
        tipo = when {
            lowerBody.contains("promocion") || lowerBody.contains("promoción") || lowerBody.contains("recarga internacional") || lowerBody.contains("bonifica") -> {
                "promocion"
            }
            saldoCUP > 0.0 -> {
                "saldo_principal"
            }
            datosMB > 0.0 || bonoDatosMB > 0.0 -> {
                "bono_datos"
            }
            else -> {
                "alerta_consumo"
            }
        }

        return SaldoMovil(
            tipo = tipo,
            saldoCUP = saldoCUP,
            datosMB = if (datosMB > 0) datosMB else 0.0,
            bonoDatosMB = bonoDatosMB,
            fechaVencimiento = fechaVencimiento.ifBlank { "30 días" },
            descripcion = body,
            timestamp = timestamp
        )
    }
}
