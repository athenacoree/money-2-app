package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BranchInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {
    @Query("SELECT * FROM company_branches ORDER BY id ASC")
    fun getAllBranches(): Flow<List<BranchInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchInfo): Long

    @Update
    suspend fun updateBranch(branch: BranchInfo)

    @Query("DELETE FROM company_branches WHERE id = :id")
    suspend fun deleteBranch(id: Int)

    @Query("DELETE FROM company_branches")
    suspend fun deleteAllBranches()
}
