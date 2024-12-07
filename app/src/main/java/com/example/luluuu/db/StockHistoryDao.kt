package com.example.luluuu.db

import androidx.room.*
import com.example.luluuu.model.StockHistory
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
@TypeConverters(Converters::class)
interface StockHistoryDao {
    @Query("SELECT * FROM stock_history WHERE stockId = :stockId ORDER BY date DESC")
    fun getHistoryForStock(stockId: Long): Flow<List<StockHistory>>

    @Query("SELECT * FROM stock_history WHERE freeQuantity > 0 ORDER BY date DESC")
    fun getFreeItemsHistory(): Flow<List<StockHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stockHistory: StockHistory)

    @Query("SELECT * FROM stock_history WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getHistoryBetweenDates(startDate: Date, endDate: Date): Flow<List<StockHistory>>

    @Query("SELECT SUM(freeQuantity) FROM stock_history WHERE stockId = :stockId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalFreeItemsForStock(stockId: Long, startDate: Date, endDate: Date): Int?

    @Query("SELECT SUM(regularQuantity) FROM stock_history WHERE stockId = :stockId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalRegularItemsForStock(stockId: Long, startDate: Date, endDate: Date): Int?

    @Query("DELETE FROM stock_history WHERE stockId = :stockId")
    suspend fun deleteHistoryForStock(stockId: Long)

    @Query("DELETE FROM stock_history")
    suspend fun clearCache()
}