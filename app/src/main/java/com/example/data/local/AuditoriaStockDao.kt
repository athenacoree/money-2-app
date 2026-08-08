package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AuditoriaStock
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditoriaStockDao {
    @Query("SELECT * FROM auditorias_stock ORDER BY timestamp DESC")
    fun getAllAuditorias(): Flow<List<AuditoriaStock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditoria(auditoria: AuditoriaStock): Long

    @Query("DELETE FROM auditorias_stock")
    suspend fun deleteAllAuditorias()
}
