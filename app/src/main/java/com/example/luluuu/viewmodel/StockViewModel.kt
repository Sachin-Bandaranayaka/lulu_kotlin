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

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StockRepository
    private val firebaseRepository: FirebaseRepository
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.NAME)

    init {
        val stockDao = AppDatabase.getDatabase(application).stockDao()
        repository = StockRepository(stockDao)
        firebaseRepository = FirebaseRepository()
    }

    val stocks = combine(
        repository.getAllStocks(),
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
            // Delete from local database
            repository.delete(stock)
            // Delete from Firebase
            try {
                if (stock.firebaseId.isNotBlank()) {
                    firebaseRepository.deleteStock(stock.firebaseId)
                }
            } catch (e: Exception) {
                Log.e("StockViewModel", "Failed to delete from Firebase: ${e.message}")
            }
        }
    }

    fun update(stock: Stock) {
        viewModelScope.launch {
            // Update local database
            repository.update(stock)
            // Sync with Firebase
            try {
                if (stock.firebaseId.isNotBlank()) {
                    firebaseRepository.updateStock(stock.firebaseId, stock)
                } else {
                    // If no Firebase ID exists, create new document
                    val firebaseId = firebaseRepository.addStock(stock)
                    // Update local stock with new Firebase ID
                    repository.update(stock.copy(firebaseId = firebaseId))
                }
            } catch (e: Exception) {
                Log.e("StockViewModel", "Failed to sync with Firebase: ${e.message}")
            }
        }
    }

    fun insert(stock: Stock) {
        viewModelScope.launch {
            // First insert into local database
            val id = repository.insert(stock)
            // Then sync with Firebase
            try {
                val stockWithId = stock.copy(id = id)
                firebaseRepository.addStock(stockWithId)
            } catch (e: Exception) {
                Log.e("StockViewModel", "Failed to sync with Firebase: ${e.message}")
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    enum class SortOrder {
        NAME, PRICE, QUANTITY
    }

    companion object {
        private const val LOW_STOCK_THRESHOLD = 5
    }
} 