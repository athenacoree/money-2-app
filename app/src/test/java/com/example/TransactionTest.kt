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
    fun testNegativeBalanceCalculation() {
        val income = 200.0
        val expense = 500.0
        val netBalance = income - expense

        assertEquals(-300.0, netBalance, 0.0)
    }

    @Test
    fun testQvaPayExpenseTypeConvention() {
        val tx = Transaction(
            id = 2L,
            tipo = "gasto",
            monto = 50.0,
            moneda = "SQP",
            categoria = "QvaPay",
            descripcion = "Transferencia QvaPay",
            fecha = System.currentTimeMillis(),
            hora = "10:00",
            metodo_pago = "QvaPay",
            es_empleador = false
        )

        assertEquals("gasto", tx.tipo)
        assertEquals("SQP", tx.moneda)
    }
}
