package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Contacto
import kotlinx.coroutines.flow.Flow

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
