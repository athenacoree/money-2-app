package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PropuestaCambio
import kotlinx.coroutines.flow.Flow

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
