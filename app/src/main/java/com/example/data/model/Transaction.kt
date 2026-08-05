package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipo: String, // "ingreso" o "gasto"
    val monto: Double,
    val moneda: String = "CUP", // "CUP", "MLC", "USD", "SQP"
    val categoria: String, // Comida, Transporte, Salario, Ventas, Servicios, Educación, Salud, Entretenimiento, Ahorros, Otros
    val descripcion: String, // opcional
    val fecha: Long, // timestamp
    val hora: String, // "HH:mm"
    val metodo_pago: String, // "Transfermóvil", "EnZona", "Efectivo", "Otro"
    val es_empleador: Boolean // true si es del negocio, false si es personal
)
