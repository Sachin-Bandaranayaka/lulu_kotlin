package com.example.luluuu.db

import androidx.room.*
import com.example.luluuu.model.StockHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface StockHistoryDao {
    @Query("SELECT * FROM stock_history WHERE stockId = :stockId ORDER BY date DESC")
    fun getHistoryForStock(stockId: Long): Flow<List<StockHistory>>

    @Query("SELECT * FROM stock_history ORDER BY date DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<StockHistory>>

    @Insert
    suspend fun insert(history: StockHistory)

    @Query("DELETE FROM stock_history WHERE stockId = :stockId")
    suspend fun deleteHistoryForStock(stockId: Long)
} 