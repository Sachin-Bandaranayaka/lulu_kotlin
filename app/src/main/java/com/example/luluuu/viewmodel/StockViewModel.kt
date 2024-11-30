package com.example.luluuu.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.luluuu.database.AppDatabase
import com.example.luluuu.model.Stock
import com.example.luluuu.repository.StockRepository
import com.example.luluuu.repository.FirebaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StockRepository
    private val firebaseRepository: FirebaseRepository
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    private val _stocks = MutableStateFlow<List<Stock>>(emptyList())

    init {
        val stockDao = AppDatabase.getDatabase(application).stockDao()
        repository = StockRepository(stockDao)
        firebaseRepository = FirebaseRepository()

        // Start collecting stocks
        viewModelScope.launch {
            combine(
                repository.getAllStocks(),
                firebaseRepository.getAllStocks()
            ) { localStocks, firebaseStocks ->
                // Use Firebase data if available, otherwise use local data
                if (firebaseStocks.isNotEmpty()) {
                    firebaseStocks
                } else {
                    localStocks
                }
            }.collect { stocks ->
                _stocks.value = stocks
            }
        }
    }

    val stocks = combine(
        _stocks,
        _searchQuery,
        _sortOrder
    ) { stocks, query, sortOrder ->
        var filteredStocks = stocks.filter {
            it.name.contains(query, ignoreCase = true)
        }
        
        filteredStocks = when (sortOrder) {
            SortOrder.NAME -> filteredStocks.sortedBy { it.name }
            SortOrder.PRICE -> filteredStocks.sortedBy { it.price }
            SortOrder.QUANTITY -> filteredStocks.sortedBy { it.quantity }
        }
        
        filteredStocks
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val lowStockItems: StateFlow<List<Stock>> = stocks
        .map { stocks -> stocks.filter { it.quantity <= LOW_STOCK_THRESHOLD } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalStockValue: StateFlow<Double> = stocks
        .map { stocks -> stocks.sumOf { it.price * it.quantity } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    fun delete(stock: Stock) {
        viewModelScope.launch {
            try {
                // First delete from Firebase if it exists
                if (stock.firebaseId.isNotBlank()) {
                    firebaseRepository.deleteStock(stock.firebaseId)
                }
                // Then delete from local database
                repository.delete(stock)
                // Refresh the stocks list
                refreshStocks()
            } catch (e: Exception) {
                Log.e("StockViewModel", "Failed to delete stock: ${e.message}")
            }
        }
    }

    fun update(stock: Stock) {
        viewModelScope.launch {
            try {
                if (stock.firebaseId.isNotBlank()) {
                    // Update Firebase first
                    firebaseRepository.updateStock(stock.firebaseId, stock)
                    // Then update local database
                    repository.update(stock)
                } else {
                    // If no Firebase ID exists, create new document
                    val firebaseId = firebaseRepository.addStock(stock)
                    // Update local stock with new Firebase ID
                    repository.update(stock.copy(firebaseId = firebaseId))
                }
                // Refresh the stocks list
                refreshStocks()
            } catch (e: Exception) {
                Log.e("StockViewModel", "Failed to update stock: ${e.message}")
            }
        }
    }

    fun insert(stock: Stock) {
        viewModelScope.launch {
            try {
                // First add to Firebase to get ID
                val firebaseId = firebaseRepository.addStock(stock)
                // Then insert into local database with Firebase ID
                val stockWithId = stock.copy(firebaseId = firebaseId)
                repository.insert(stockWithId)
                // Refresh the stocks list
                refreshStocks()
            } catch (e: Exception) {
                Log.e("StockViewModel", "Failed to insert stock: ${e.message}")
            }
        }
    }

    private suspend fun refreshStocks() {
        try {
            val firebaseStocks = firebaseRepository.getAllStocks().first()
            if (firebaseStocks.isNotEmpty()) {
                _stocks.value = firebaseStocks
            } else {
                _stocks.value = repository.getAllStocks().first()
            }
        } catch (e: Exception) {
            Log.e("StockViewModel", "Error refreshing stocks: ${e.message}")
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    enum class SortOrder {
        NAME, PRICE, QUANTITY
    }

    companion object {
        private const val LOW_STOCK_THRESHOLD = 5
    }
}