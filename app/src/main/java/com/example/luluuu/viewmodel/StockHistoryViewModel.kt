package com.example.luluuu.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.luluuu.db.AppDatabase
import com.example.luluuu.model.StockHistory
import com.example.luluuu.repository.StockHistoryFirebaseRepository
import com.example.luluuu.repository.StockRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class StockHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "StockHistoryVM"
    private val stockDao = AppDatabase.getDatabase(application).stockDao()
    private val stockHistoryDao = AppDatabase.getDatabase(application).stockHistoryDao()
    private val firebaseHistoryRepository = StockHistoryFirebaseRepository()
    private val repository = StockRepository(stockDao, stockHistoryDao, firebaseHistoryRepository)

    private val _stockHistory = MutableLiveData<List<StockHistory>>()
    val stockHistory: LiveData<List<StockHistory>> = _stockHistory

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

    fun loadStockHistory(stockId: Long) {
        Log.d(TAG, "Loading stock history for stock ID: $stockId")
        viewModelScope.launch {
            try {
                repository.getStockHistory(stockId)
                    .catch { e ->
                        if (e is CancellationException) {
                            Log.d(TAG, "Stock history loading cancelled")
                            throw e
                        } else {
                            Log.e(TAG, "Error loading stock history", e)
                            _stockHistory.value = emptyList()
                        }
                    }
                    .collect { historyList ->
                        Log.d(TAG, "Received ${historyList.size} history entries")
                        historyList.forEach { history ->
                            Log.d(TAG, "History entry: ${formatHistoryEntry(history)}")
                        }
                        _stockHistory.value = historyList.sortedByDescending { it.date }
                    }
            } catch (e: Exception) {
                when (e) {
                    is CancellationException -> {
                        Log.d(TAG, "Stock history loading cancelled")
                        throw e
                    }
                    else -> {
                        Log.e(TAG, "Error in loadStockHistory", e)
                        _stockHistory.value = emptyList()
                    }
                }
            }
        }
    }

    private fun formatHistoryEntry(history: StockHistory): String {
        return buildString {
            append("Date: ${dateFormat.format(history.date)}, ")
            append("Action: ${history.action}, ")
            when (history.action) {
                "CREATE" -> {
                    append("Initial quantity: ${history.newQuantity}, ")
                    append("Initial price: $${String.format("%.2f", history.newPrice)}")
                }
                "UPDATE" -> {
                    val quantityDiff = history.newQuantity - history.oldQuantity
                    val quantitySign = if (quantityDiff >= 0) "+" else ""
                    append("Quantity: ${history.oldQuantity} → ${history.newQuantity} ($quantitySign$quantityDiff), ")

                    val priceDiff = history.newPrice - history.oldPrice
                    val priceSign = if (priceDiff >= 0) "+" else ""
                    append("Price: $${String.format("%.2f", history.oldPrice)} → ")
                    append("$${String.format("%.2f", history.newPrice)} ")
                    append("($priceSign$${String.format("%.2f", priceDiff)})")
                }
                "DELETE" -> {
                    append("Final quantity: ${history.oldQuantity}, ")
                    append("Final price: $${String.format("%.2f", history.oldPrice)}")
                }
            }
        }
    }

    public fun cleanup() {
        viewModelScope.launch {
            // Cancel any ongoing operations
            Log.d(TAG, "Cleaning up ViewModel resources")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}