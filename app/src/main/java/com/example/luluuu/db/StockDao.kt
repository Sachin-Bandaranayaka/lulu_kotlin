package com.example.luluuu.db

import androidx.room.*
import com.example.luluuu.model.Stock
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY name ASC")
    fun getAllStocks(): Flow<List<Stock>>

    @Insert
    suspend fun insert(stock: Stock): Long

    @Update
    suspend fun update(stock: Stock)

    @Delete
    suspend fun delete(stock: Stock)

    @Query("SELECT * FROM stocks WHERE id = :id")
    suspend fun getStockById(id: Long): Stock?
} 