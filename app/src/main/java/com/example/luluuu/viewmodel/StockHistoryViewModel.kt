package com.example.luluuu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.luluuu.database.AppDatabase
import com.example.luluuu.model.StockHistory
import kotlinx.coroutines.flow.Flow

class StockHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val stockHistoryDao = database.stockHistoryDao()

    fun getStockHistory(stockId: Long): Flow<List<StockHistory>> {
        return stockHistoryDao.getHistoryForStock(stockId)
    }
} 