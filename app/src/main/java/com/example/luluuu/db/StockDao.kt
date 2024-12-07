package com.example.luluuu.db

import androidx.room.*
import com.example.luluuu.model.Stock
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY name ASC")
    fun getAllStocks(): Flow<List<Stock>>

    @Query("SELECT * FROM stocks ORDER BY name ASC")
    suspend fun getAllStocksSync(): List<Stock>

    @Insert
    suspend fun insert(stock: Stock): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<Stock>)

    @Update
    suspend fun update(stock: Stock)

    @Delete
    suspend fun delete(stock: Stock)

    @Query("SELECT * FROM stocks WHERE id = :id")
    suspend fun getStockById(id: Long): Stock?

    @Query("SELECT * FROM stocks WHERE name LIKE :searchQuery OR description LIKE :searchQuery")
    fun searchStocks(searchQuery: String): Flow<List<Stock>>

    @Query("UPDATE stocks SET firebaseId = :firebaseId WHERE id = :localId")
    suspend fun updateFirebaseId(localId: Long, firebaseId: String)

    @Query("DELETE FROM stocks")
    suspend fun clearCache()
}