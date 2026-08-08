package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DespachoDistribuidor
import kotlinx.coroutines.flow.Flow

@Dao
interface DespachoDistribuidorDao {
    @Query("SELECT * FROM despachos_distribuidor ORDER BY timestamp DESC")
    fun getAllDespachos(): Flow<List<DespachoDistribuidor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDespacho(despacho: DespachoDistribuidor): Long

    @Update
    suspend fun updateDespacho(despacho: DespachoDistribuidor)

    @Query("DELETE FROM despachos_distribuidor WHERE id = :id")
    suspend fun deleteDespacho(id: Long)

    @Query("DELETE FROM despachos_distribuidor")
    suspend fun deleteAllDespachos()
}
