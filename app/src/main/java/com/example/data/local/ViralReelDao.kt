package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ViralReelDao {
    @Query("SELECT * FROM viral_reels ORDER BY createdAtMillis DESC")
    fun observeAllViralReels(): Flow<List<ViralReelEntity>>

    @Query("SELECT COUNT(*) FROM viral_reels WHERE UPPER(easypaisaTid) = UPPER(:tid)")
    suspend fun countByTransactionId(tid: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViralReel(entity: ViralReelEntity)

    @Query("UPDATE viral_reels SET viralShares = viralShares + 1 WHERE id = :id")
    suspend fun incrementViralShares(id: String)

    @Query("DELETE FROM viral_reels WHERE id = :id")
    suspend fun deleteViralReel(id: String)
}
