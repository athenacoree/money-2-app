package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey
import java.security.MessageDigest

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val imagen_uri: String? = null // opcional, ruta de la imagen
)

@Entity(tableName = "empleados")
data class Empleado(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val telefono: String,
    val estado: String, // "activo" o "inactivo"
    val foto_uri: String? = null,
    val fecha_vinculacion: Long // timestamp
)

@Entity(tableName = "contactos")
data class Contacto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val telefono: String,
    val ultimo_mensaje: String? = null,
    val hora_ultimo_mensaje: Long? = null, // timestamp
    val mensajes_no_leidos: Int = 0,
    val avatarColorHex: String = "#7C3AED"
)

@Entity(tableName = "mensajes")
data class Mensaje(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contacto_id: Long, // foreign key
    val es_enviado: Boolean, // true = yo envié, false = recibí
    val contenido: String,
    val timestamp: Long
)

@Entity(
    tableName = "configuraciones",
    indices = [Index(value = ["clave"], unique = true)]
)
data class Configuracion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clave: String, // "nombre_usuario", "telefono_usuario", "numero_transfermovil", etc.
    val valor: String
)

@Entity(
    tableName = "auditorias_stock",
    foreignKeys = [
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["producto_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["producto_id"])]
)
data class AuditoriaStock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val producto_id: Long?,
    val nombre_producto: String,
    val cambio_stock: Int, // e.g. +5 o -2
    val stock_anterior: Int,
    val stock_resultante: Int,
    val justificacion: String,
    val realizado_por: String, // "Empleador", "Empleado", "Fusión de Rama", etc.
    val dado_por_empleado: String? = null, // Lo que propuso el empleado
    val cambiado_por_empleador: String? = null, // Lo que ajustó el empleador antes de la fusión
    val es_fusion_rama: Boolean = false,
    val timestamp: Long
)

@Entity(
    tableName = "propuestas_cambio",
    foreignKeys = [
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["producto_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["producto_id"])]
)
data class PropuestaCambio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val empleado_nombre: String,
    val producto_id: Long? = null, // null si es producto nuevo
    val nombre_producto: String,
    val precio_propuesto: Double,
    val stock_propuesto: Int,
    val imagen_uri: String? = null,
    val justificacion: String,
    val estado: String = "pendiente", // "pendiente", "aprobado", "rechazado"
    val timestamp: Long = System.currentTimeMillis()
)

enum class AppMode {
    PERSONAL, WORK_EMPLOYER, WORK_EMPLOYEE, WORK_DISTRIBUTOR
}

@Entity(tableName = "despachos_distribuidor")
data class DespachoDistribuidor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val destinatario: String,
    val productoNombre: String,
    val cantidadUnidades: Int,
    val precioPorUnidad: Double,
    val estado: String = "Entregado",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "company_branches")
data class BranchInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val address: String,
    val isMain: Boolean = false,
    val managerName: String = "Administrador"
)

object PinHasher {
    private const val SALT = "CubaFinanzasSalt2026"
    fun hash(pin: String, customSalt: String = SALT): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest((pin + customSalt).toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

@Entity(tableName = "saldo_movil")
data class SaldoMovil(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipo: String, // "saldo_principal", "bono_datos", "promocion", "alerta_consumo"
    val saldoCUP: Double = 0.0,
    val datosMB: Double = 0.0,
    val bonoDatosMB: Double = 0.0,
    val fechaVencimiento: String = "",
    val descripcion: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
