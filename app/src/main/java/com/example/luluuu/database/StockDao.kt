package com.example.luluuu.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.luluuu.model.Stock
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks")
    fun getAllStocks(): Flow<List<Stock>>

    @Query("SELECT * FROM stocks")
    suspend fun getAllStocksSync(): List<Stock>

    @Insert
    suspend fun insert(stock: Stock): Long

    @Update
    suspend fun update(stock: Stock)

    @Delete
    suspend fun delete(stock: Stock)

    @Query("SELECT * FROM stocks WHERE id = :id")
    suspend fun getStockById(id: Long): Stock?
} 