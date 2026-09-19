package com.example.glyphmuseum.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GifDao {
    @Query("SELECT * FROM gifs ORDER BY added_at DESC")
    fun getAllGifs(): Flow<List<GifEntity>>

    @Query("SELECT * FROM gifs WHERE id = :id LIMIT 1")
    suspend fun getGifById(id: Long): GifEntity?

    @Insert
    suspend fun insertGif(gif: GifEntity): Long

    @Delete
    suspend fun deleteGif(gif: GifEntity)
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY priority ASC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE is_enabled = 1 ORDER BY priority ASC")
    suspend fun getActiveRulesSync(): List<RuleEntity>

    @Insert
    suspend fun insertRule(rule: RuleEntity): Long

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Delete
    suspend fun deleteRule(rule: RuleEntity)
}
