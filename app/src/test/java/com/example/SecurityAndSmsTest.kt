package com.example

import com.example.data.model.PinHasher
import org.junit.Assert.*
import org.junit.Test
import java.util.regex.Pattern

class SecurityAndSmsTest {

    @Test
    fun testPinHasher() {
        val hash1 = PinHasher.hash("1234")
        val hash2 = PinHasher.hash("1234")
        val hash3 = PinHasher.hash("4321")

        assertEquals(64, hash1.length) // Hex SHA-256 length is 64
        assertEquals(hash1, hash2) // Deterministic
        assertNotEquals(hash1, hash3) // Different PINs have different hashes
    }

    @Test
    fun testSmsParserRegex() {
        // Real-world example 1: Transfermóvil payment
        val body1 = "Pagué utilizando Transfermovil en la Entidad: EESEC. YDE SERV. DEPROD.UNIVERSALES LA HABANA Id Compra: 9665792441 Importe: 268.00 CUP Importe pagado: 268.00 CUP No. Transaccion: MM4004WKVI987"

        // 1. Extract phone (none in body1)
        val phoneRegex = Pattern.compile("(?i)(?:\\+53\\s*)?\\b([56]\\d{7})\\b")
        val phoneMatcher = phoneRegex.matcher(body1)
        assertFalse(phoneMatcher.find())

        // 2. Extract amount next to "CUP" or "MLC"
        val amountCurrencyRegex = Pattern.compile("(?i)(?:(CUP|MLC)\\s*(\\d+(?:\\.\\d+)?))|(?:(\\d+(?:\\.\\d+)?)\\s*(CUP|MLC))")
        val acMatcher = amountCurrencyRegex.matcher(body1)
        assertTrue(acMatcher.find())
        val amount = acMatcher.group(3)?.toDoubleOrNull()
        val currency = acMatcher.group(4)
        assertEquals(268.00, amount)
        assertEquals("CUP", currency)

        // Real-world example 2: Electricity payment with currency
        val body2 = "Factura Pagada: 2301221444524 Importe Pagado: 530.59 CUP Importe Factura: 547.00 CUP Nro. Transaccion: MM301085HT987"
        val acMatcher2 = amountCurrencyRegex.matcher(body2)
        assertTrue(acMatcher2.find())
        val amount2 = acMatcher2.group(3)?.toDoubleOrNull()
        val currency2 = acMatcher2.group(4)
        assertEquals(530.59, amount2)
        assertEquals("CUP", currency2)

        // Real-world example 3: Phone number ignoring
        val body3 = "Se ha realizado una transferencia a la cuenta 9225xxxxxxxxxxxx de 150.00 CUP. Móvil Origen +53 58436656."

        // Extract phone number first
        var cleanedBody = body3
        val phoneMatcher3 = phoneRegex.matcher(body3)
        assertTrue(phoneMatcher3.find())
        val extractedPhone = phoneMatcher3.group(1)
        assertEquals("58436656", extractedPhone)
        cleanedBody = phoneMatcher3.replaceAll("[PHONE]")

        // Match amount next to currency on cleaned body
        val acMatcher3 = amountCurrencyRegex.matcher(cleanedBody)
        assertTrue(acMatcher3.find())
        val amount3 = acMatcher3.group(3)?.toDoubleOrNull()
        val currency3 = acMatcher3.group(4)
        assertEquals(150.00, amount3)
        assertEquals("CUP", currency3)
    }

    @Test
    fun testIncomeExpenseSmsClassification() {
        val bodyIncome1 = "Ha recibido una transferencia de 100.00 CUP de la tarjeta 9225xxxxxxxxxxxx"
        val bodyIncome2 = "Se ha recibido un deposito de 50.00 MLC"
        val bodyExpense1 = "Se ha realizado una transferencia a la cuenta xxxx de 250.00 CUP"
        val bodyExpense2 = "Ha realizado un pago de 20.00 CUP"

        val lower1 = bodyIncome1.lowercase()
        val lower2 = bodyIncome2.lowercase()
        val lower3 = bodyExpense1.lowercase()
        val lower4 = bodyExpense2.lowercase()

        val isIncome1 = lower1.contains("recibido") || lower1.contains("recibi") || lower1.contains("ingreso") || lower1.contains("acreditado") || lower1.contains("depósito") || lower1.contains("deposito")
        val isIncome2 = lower2.contains("recibido") || lower2.contains("recibi") || lower2.contains("ingreso") || lower2.contains("acreditado") || lower2.contains("depósito") || lower2.contains("deposito")
        val isIncome3 = lower3.contains("recibido") || lower3.contains("recibi") || lower3.contains("ingreso") || lower3.contains("acreditado") || lower3.contains("depósito") || lower3.contains("deposito")
        val isIncome4 = lower4.contains("recibido") || lower4.contains("recibi") || lower4.contains("ingreso") || lower4.contains("acreditado") || lower4.contains("depósito") || lower4.contains("deposito")

        assertTrue(isIncome1)
        assertTrue(isIncome2)
        assertFalse(isIncome3)
        assertFalse(isIncome4)
    }

    @Test
    fun testCubacelSmsParser() {
        val parser = com.example.data.receiver.CubacelMessageParser

        // Test Case 1: Main Balance
        val body1 = "ETECSA informa: Su saldo principal es de 125.00 CUP, valido hasta 30/12/2026."
        val parsed1 = parser.parseMessage(body1, 1000L)
        assertEquals("saldo_principal", parsed1.tipo)
        assertEquals(125.0, parsed1.saldoCUP, 0.01)
        assertEquals("30/12/2026", parsed1.fechaVencimiento)

        // Test Case 2: Data Packages
        val body2 = "Su paquete de datos tiene 2.4 GB de navegacion internacional LTE y 300 MB de navegacion nacional, validos hasta 25/11/2026."

        // Print matching details
        val pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(GB|MB)\\s*(?:de\\s+)?(?:navegacion|datos|LTE|internacional)?")
        val matcher = pattern.matcher(body2)
        println("--- MATCHER DEBUG ---")
        while(matcher.find()) {
            println("Matched group: '${matcher.group()}' at ${matcher.start()}-${matcher.end()}")
            println("Group 1: '${matcher.group(1)}', Group 2: '${matcher.group(2)}'")
            val start = matcher.start()
            val end = Math.min(body2.length, matcher.end() + 25)
            val context = body2.substring(start, end).lowercase()
            println("Context: '$context'")
            println("Contains nacional: ${context.contains("nacional")} | bono: ${context.contains("bono")} | .cu: ${context.contains(".cu")}")
        }

        val parsed2 = parser.parseMessage(body2, 1000L)
        println("Parsed - Type: ${parsed2.tipo}, Balance: ${parsed2.saldoCUP}, DatosMB: ${parsed2.datosMB}, BonoMB: ${parsed2.bonoDatosMB}")
        assertEquals("bono_datos", parsed2.tipo)
        assertEquals(2457.6, parsed2.datosMB, 0.01) // 2.4 * 1024 = 2457.6
        assertEquals(300.0, parsed2.bonoDatosMB, 0.01)
        assertEquals("25/11/2026", parsed2.fechaVencimiento)

        // Test Case 3: ETECSA Promotion
        val body3 = "ETECSA Promocion Internacional: Del 5 al 9 de agosto, si recargas desde el exterior recibes 25 GB + Datos ilimitados de 12 a 7 am!"
        val parsed3 = parser.parseMessage(body3, 1000L)
        assertEquals("promocion", parsed3.tipo)
        assertTrue(parsed3.descripcion.contains("Promocion Internacional"))
    }
}
