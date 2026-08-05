package com.example.data.receiver

import com.example.data.model.SaldoMovil
import java.util.regex.Pattern

object CubacelMessageParser {

    fun parseMessage(body: String, timestamp: Long): SaldoMovil {
        val lowerBody = body.lowercase()

        var saldoCUP = 0.0
        var datosMB = 0.0
        var bonoDatosMB = 0.0
        var fechaVencimiento = ""
        var tipo = "alerta_consumo"

        // 1. Parse CUP Balance
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

        // 2. Parse Data Packages (MB/GB)
        val dataRegex = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(GB|MB)\\s*(?:de\\s+)?(?:navegacion|datos|LTE|internacional)?")
        val dataMatcher = dataRegex.matcher(body)
        while (dataMatcher.find()) {
            val amount = dataMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            val unit = dataMatcher.group(2)?.uppercase() ?: "MB"
            val mbs = if (unit == "GB") amount * 1024.0 else amount

            // Check context of next 25 characters to distinguish national vs international
            val start = dataMatcher.start()
            val end = Math.min(body.length, dataMatcher.end() + 25)
            val context = body.substring(start, end).lowercase()

            // Safe check: "internacional" contains "nacional", so filter out "internacional"
            val isNational = (context.contains("nacional") && !context.contains("internacional")) ||
                             context.contains("bono") ||
                             context.contains(".cu")

            if (isNational) {
                bonoDatosMB += mbs
            } else {
                datosMB += mbs
            }
        }

        // 3. Parse Expiration Date
        // Supporting: "valido hasta", "validos hasta", "vence el", "vence", "vencen", "expira", "validez"
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
            datosMB = if (datosMB > 0) datosMB else if (tipo == "bono_datos") 1024.0 else 0.0,
            bonoDatosMB = bonoDatosMB,
            fechaVencimiento = fechaVencimiento.ifBlank { "30 días" },
            descripcion = body,
            timestamp = timestamp
        )
    }
}
