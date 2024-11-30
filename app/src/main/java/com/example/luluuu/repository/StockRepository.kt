package com.example.luluuu.repository

import com.example.luluuu.database.StockDao
import com.example.luluuu.database.StockHistoryDao
import com.example.luluuu.model.Stock
import com.example.luluuu.model.StockHistory
import java.util.Date
import kotlinx.coroutines.flow.Flow
import android.util.Log

class StockRepository(
    private val stockDao: StockDao,
    private val stockHistoryDao: StockHistoryDao
) {
    val allStocks: Flow<List<Stock>> = stockDao.getAllStocks()
    private val firebaseRepository = FirebaseRepository()

    suspend fun insert(stock: Stock) {
        val stockId = stockDao.insert(stock)
        stockHistoryDao.insert(
            StockHistory(
                stockId = stockId,
                date = Date(),
                oldQuantity = 0,
                newQuantity = stock.quantity,
                oldPrice = 0.0,
                newPrice = stock.price,
                action = "CREATE"
            )
        )
        val firebaseId = firebaseRepository.addStock(stock.copy(id = stockId))
        stockDao.update(stock.copy(id = stockId, firebaseId = firebaseId))
    }

    suspend fun update(stock: Stock) {
        val oldStock = stockDao.getStockById(stock.id)
        stockDao.update(stock)
        
        oldStock?.let {
            stockHistoryDao.insert(
                StockHistory(
                    stockId = stock.id,
                    date = Date(),
                    oldQuantity = it.quantity,
                    newQuantity = stock.quantity,
                    oldPrice = it.price,
                    newPrice = stock.price,
                    action = "EDIT"
                )
            )
        }
        firebaseRepository.updateStock(stock.firebaseId, stock)
    }

    suspend fun delete(stock: Stock) {
        try {
            if (stock.firebaseId.isNotBlank()) {
                firebaseRepository.deleteStock(stock.firebaseId)
            }
            
            stockHistoryDao.insert(
                StockHistory(
                    stockId = stock.id,
                    date = Date(),
                    oldQuantity = stock.quantity,
                    newQuantity = 0,
                    oldPrice = stock.price,
                    newPrice = 0.0,
                    action = "DELETE"
                )
            )
            
            stockDao.delete(stock)
        } catch (e: Exception) {
            Log.e("StockRepository", "Error deleting stock", e)
            throw e
        }
    }
} 