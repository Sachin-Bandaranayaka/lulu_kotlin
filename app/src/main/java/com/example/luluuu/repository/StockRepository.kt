package com.example.luluuu.repository

import com.example.luluuu.db.StockDao
import com.example.luluuu.model.Stock
import kotlinx.coroutines.flow.Flow

class StockRepository(private val stockDao: StockDao) {
    val allStocks: Flow<List<Stock>> = stockDao.getAllStocks()

    suspend fun insert(stock: Stock) {
        stockDao.insert(stock)
    }

    suspend fun update(stock: Stock) {
        stockDao.update(stock)
    }

    suspend fun delete(stock: Stock) {
        stockDao.delete(stock)
    }

    suspend fun getStockById(id: Long): Stock? {
        return stockDao.getStockById(id)
    }
} 