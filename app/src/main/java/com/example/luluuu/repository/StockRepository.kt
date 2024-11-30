package com.example.luluuu.repository

import com.example.luluuu.db.StockDao
import com.example.luluuu.model.Stock
import kotlinx.coroutines.flow.Flow
import com.example.luluuu.repository.FirebaseRepository

class StockRepository(
    private val stockDao: StockDao,
    private val firebaseRepository: FirebaseRepository
) {
    val allStocks: Flow<List<Stock>> = stockDao.getAllStocks()

    suspend fun insert(stock: Stock) {
        val id = stockDao.insert(stock)
        // Sync with Firebase
        firebaseRepository.syncStock(stock.copy(id = id))
    }

    suspend fun update(stock: Stock) {
        stockDao.update(stock)
        // Sync with Firebase
        firebaseRepository.syncStock(stock)
    }

    suspend fun delete(stock: Stock) {
        stockDao.delete(stock)
        // Delete from Firebase
        firebaseRepository.deleteStock(stock.id)
    }

    suspend fun getStockById(id: Long): Stock? {
        return stockDao.getStockById(id)
    }
} 