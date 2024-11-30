package com.example.luluuu.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.luluuu.model.StockHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface StockHistoryDao {
    @Query("SELECT * FROM stock_history WHERE stockId = :stockId ORDER BY date DESC")
    fun getHistoryForStock(stockId: Long): Flow<List<StockHistory>>

    @Insert
    suspend fun insert(stockHistory: StockHistory)
} 