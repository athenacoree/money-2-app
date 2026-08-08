package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Empleado
import kotlinx.coroutines.flow.Flow

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
