package com.example.luluuu.repository

import android.util.Log
import com.example.luluuu.db.StockDao
import com.example.luluuu.db.StockHistoryDao
import com.example.luluuu.model.Stock
import com.example.luluuu.model.StockHistory
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class StockRepository(
    private val stockDao: StockDao,
    private val stockHistoryDao: StockHistoryDao,
    private val firebaseHistoryRepository: StockHistoryFirebaseRepository
) {
    fun getAllStocks() = stockDao.getAllStocks()

    suspend fun insert(stock: Stock): Long {
        val id = stockDao.insert(stock)
        val history = StockHistory(
            stockId = id,
            date = Date(),
            oldQuantity = 0,
            newQuantity = stock.quantity,
            oldPrice = 0.0,
            newPrice = stock.price,
            action = "CREATE"
        )
        
        try {
            // Add to local database
            stockHistoryDao.insert(history)
            // Add to Firebase
            firebaseHistoryRepository.addHistory(history)
        } catch (e: Exception) {
            Log.e("StockRepository", "Error saving stock history", e)
        }
        
        return id
    }

    suspend fun update(stock: Stock) {
        Log.d("StockRepository", "Starting stock update for ID: ${stock.id}")
        
        try {
            // If stock ID is 0, treat it as an insert
            if (stock.id == 0L) {
                Log.d("StockRepository", "Stock ID is 0, treating as new stock")
                insert(stock)
                return
            }
            
            val oldStock = stockDao.getStockById(stock.id)
            Log.d("StockRepository", "Old stock: ${oldStock?.quantity} units at ${oldStock?.price}, New stock: ${stock.quantity} units at ${stock.price}")
            
            // Update local database first
            stockDao.update(stock)
            Log.d("StockRepository", "Stock updated in local database")
            
            // Only create history if there are actual changes in quantity or price
            if (oldStock != null && (oldStock.quantity != stock.quantity || oldStock.price != stock.price)) {
                // Create history
                val history = StockHistory(
                    stockId = stock.id,
                    date = Date(),
                    oldQuantity = oldStock.quantity,
                    newQuantity = stock.quantity,
                    oldPrice = oldStock.price,
                    newPrice = stock.price,
                    action = "UPDATE"
                )
                
                // Add history to local database first
                stockHistoryDao.insert(history)
                Log.d("StockRepository", "Stock history added to local database")
                
                try {
                    // Try to sync with Firebase, but don't block on failure
                    firebaseHistoryRepository.addHistory(history)
                    Log.d("StockRepository", "Stock history synced to Firebase")
                } catch (e: Exception) {
                    // Log the error but don't fail the update - we'll sync later when online
                    Log.w("StockRepository", "Failed to sync history with Firebase (will retry when online): ${e.message}")
                }
            } else {
                Log.d("StockRepository", "No changes in quantity or price, skipping history creation")
            }
            
            try {
                val firebaseRepo = FirebaseRepository(stockDao)
                firebaseRepo.syncStock(stock)
                Log.d("StockRepository", "Stock synced to Firebase")
            } catch (e: Exception) {
                // Log the error but don't fail the update - we'll sync later when online
                Log.w("StockRepository", "Failed to sync stock with Firebase (will retry when online): ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("StockRepository", "Error during stock update", e)
            throw e // Rethrow to notify UI of failure
        }
    }

    suspend fun delete(stock: Stock) {
        try {
            val history = StockHistory(
                stockId = stock.id,
                date = Date(),
                oldQuantity = stock.quantity,
                newQuantity = 0,
                oldPrice = stock.price,
                newPrice = 0.0,
                action = "DELETE"
            )
            
            // Add to local database
            stockHistoryDao.insert(history)
            // Add to Firebase
            firebaseHistoryRepository.addHistory(history)
            
            stockDao.delete(stock)
        } catch (e: Exception) {
            Log.e("StockRepository", "Error deleting stock", e)
            throw e
        }
    }

    suspend fun getStockById(id: Long): Stock? {
        return stockDao.getStockById(id)
    }

    fun getStockHistory(stockId: Long): Flow<List<StockHistory>> {
        // Combine local and Firebase history
        return combine(
            stockHistoryDao.getHistoryForStock(stockId),
            firebaseHistoryRepository.getHistoryForStock(stockId)
        ) { localHistory, firebaseHistory ->
            // Merge and sort by date
            (localHistory + firebaseHistory)
                .distinctBy { "${it.date}${it.action}" } // Remove duplicates
                .sortedByDescending { it.date }
        }
    }
}