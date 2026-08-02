package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Producto
import com.example.data.model.Empleado
import com.example.data.model.Contacto
import com.example.data.model.Mensaje
import com.example.data.model.Configuracion
import com.example.data.model.AuditoriaStock
import com.example.data.model.PropuestaCambio
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun getAllProductos(): Flow<List<Producto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducto(producto: Producto): Long

    @Update
    suspend fun updateProducto(producto: Producto)

    @Query("DELETE FROM productos WHERE id = :id")
    suspend fun deleteProducto(id: Long)

    @Query("DELETE FROM productos")
    suspend fun deleteAllProductos()
}

@Dao
interface EmpleadoDao {
    @Query("SELECT * FROM empleados ORDER BY nombre ASC")
    fun getAllEmpleados(): Flow<List<Empleado>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmpleado(empleado: Empleado): Long

    @Update
    suspend fun updateEmpleado(empleado: Empleado)

    @Query("DELETE FROM empleados WHERE id = :id")
    suspend fun deleteEmpleado(id: Long)
}

@Dao
interface ContactoDao {
    @Query("SELECT * FROM contactos ORDER BY hora_ultimo_mensaje DESC, nombre ASC")
    fun getAllContactos(): Flow<List<Contacto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacto(contacto: Contacto): Long

    @Update
    suspend fun updateContacto(contacto: Contacto)

    @Query("SELECT * FROM contactos WHERE telefono = :telefono LIMIT 1")
    suspend fun getContactoByTelefono(telefono: String): Contacto?

    @Query("DELETE FROM contactos WHERE id = :id")
    suspend fun deleteContacto(id: Long)

    @Query("DELETE FROM contactos")
    suspend fun deleteAllContactos()
}

@Dao
interface MensajeDao {
    @Query("SELECT * FROM mensajes WHERE contacto_id = :contactoId ORDER BY timestamp ASC")
    fun getMensajesForContacto(contactoId: Long): Flow<List<Mensaje>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMensaje(mensaje: Mensaje): Long

    @Query("DELETE FROM mensajes WHERE contacto_id = :contactoId")
    suspend fun deleteMensajesForContacto(contactoId: Long)
}

@Dao
interface ConfiguracionDao {
    @Query("SELECT * FROM configuraciones")
    fun getAllConfiguraciones(): Flow<List<Configuracion>>

    @Query("SELECT * FROM configuraciones WHERE clave = :clave LIMIT 1")
    suspend fun getConfiguracionByKey(clave: String): Configuracion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfiguracion(configuracion: Configuracion): Long

    @Query("DELETE FROM configuraciones")
    suspend fun deleteAllConfiguraciones()
}

@Dao
interface AuditoriaStockDao {
    @Query("SELECT * FROM auditorias_stock ORDER BY timestamp DESC")
    fun getAllAuditorias(): Flow<List<AuditoriaStock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditoria(auditoria: AuditoriaStock): Long

    @Query("DELETE FROM auditorias_stock")
    suspend fun deleteAllAuditorias()
}

@Dao
interface PropuestaCambioDao {
    @Query("SELECT * FROM propuestas_cambio ORDER BY timestamp DESC")
    fun getAllPropuestas(): Flow<List<PropuestaCambio>>

    @Query("SELECT * FROM propuestas_cambio WHERE estado = 'pendiente' ORDER BY timestamp DESC")
    fun getPropuestasPendientes(): Flow<List<PropuestaCambio>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPropuesta(propuesta: PropuestaCambio): Long

    @Update
    suspend fun updatePropuesta(propuesta: PropuestaCambio)

    @Query("DELETE FROM propuestas_cambio WHERE id = :id")
    suspend fun deletePropuesta(id: Long)

    @Query("DELETE FROM propuestas_cambio")
    suspend fun deleteAllPropuestas()
}
