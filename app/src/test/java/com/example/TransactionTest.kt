package com.example

import com.example.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TransactionTest {

    @Test
    fun testTransactionCreation() {
        val timestamp = System.currentTimeMillis()
        val tx = Transaction(
            id = 1L,
            tipo = "ingreso",
            monto = 500.0,
            categoria = "Ventas",
            descripcion = "Venta de café",
            fecha = timestamp,
            hora = "12:30",
            metodo_pago = "Transfermóvil",
            es_empleador = true
        )

        assertEquals(1L, tx.id)
        assertEquals("ingreso", tx.tipo)
        assertEquals(500.0, tx.monto, 0.0)
        assertEquals("Ventas", tx.categoria)
        assertEquals("Venta de café", tx.descripcion)
        assertEquals(timestamp, tx.fecha)
        assertEquals("12:30", tx.hora)
        assertEquals("Transfermóvil", tx.metodo_pago)
        assertTrue(tx.es_empleador)
    }

    @Test
    fun testTransactionCurrencies() {
        val timestamp = System.currentTimeMillis()
        val txCup = Transaction(
            id = 2L,
            tipo = "ingreso",
            monto = 1000.0,
            categoria = "Otros",
            descripcion = "Pago CUP",
            fecha = timestamp,
            hora = "15:00",
            metodo_pago = "Efectivo",
            es_empleador = false,
            moneda = "CUP"
        )
        val txMlc = Transaction(
            id = 3L,
            tipo = "ingreso",
            monto = 50.0,
            categoria = "Otros",
            descripcion = "Pago MLC",
            fecha = timestamp,
            hora = "15:05",
            metodo_pago = "Transfermóvil",
            es_empleador = false,
            moneda = "MLC"
        )

        assertEquals("CUP", txCup.moneda)
        assertEquals("MLC", txMlc.moneda)
    }
}
