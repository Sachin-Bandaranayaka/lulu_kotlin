package com.example.luluuu.repository

import com.example.luluuu.database.StockDao
import com.example.luluuu.model.Stock
import java.util.Date
import kotlinx.coroutines.flow.Flow
import android.util.Log
import kotlinx.coroutines.flow.combine

class StockRepository(private val stockDao: StockDao) {
    fun getAllStocks() = stockDao.getAllStocks()

    suspend fun insert(stock: Stock) = stockDao.insert(stock)

    suspend fun update(stock: Stock) = stockDao.update(stock)

    suspend fun delete(stock: Stock) {
        try {
            stockDao.delete(stock)
        } catch (e: Exception) {
            // Log error or handle exception
        }
    }
} 