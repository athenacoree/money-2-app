package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SaldoMovil
import kotlinx.coroutines.flow.Flow

@Dao
interface SaldoMovilDao {
    @Query("SELECT * FROM saldo_movil ORDER BY timestamp DESC")
    fun getAllSaldoMovil(): Flow<List<SaldoMovil>>

    @Query("SELECT * FROM saldo_movil WHERE tipo = 'promocion' ORDER BY timestamp DESC")
    fun getAllPromociones(): Flow<List<SaldoMovil>>

    @Query("SELECT * FROM saldo_movil ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSaldoMovil(): SaldoMovil?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaldoMovil(saldo: SaldoMovil): Long

    @Query("DELETE FROM saldo_movil")
    suspend fun deleteAllSaldoMovil()
}
