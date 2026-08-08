package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Mensaje
import kotlinx.coroutines.flow.Flow

@Dao
interface MensajeDao {
    @Query("SELECT * FROM mensajes WHERE contacto_id = :contactoId ORDER BY timestamp ASC")
    fun getMensajesForContacto(contactoId: Long): Flow<List<Mensaje>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMensaje(mensaje: Mensaje): Long

    @Query("DELETE FROM mensajes WHERE contacto_id = :contactoId")
    suspend fun deleteMensajesForContacto(contactoId: Long)
}
