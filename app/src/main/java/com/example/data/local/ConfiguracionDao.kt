package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Configuracion
import kotlinx.coroutines.flow.Flow

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
