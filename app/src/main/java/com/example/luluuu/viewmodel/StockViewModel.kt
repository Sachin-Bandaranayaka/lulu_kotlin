package com.example.luluuu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.luluuu.database.AppDatabase
import com.example.luluuu.model.Stock
import com.example.luluuu.repository.StockRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StockRepository
    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        val stockDao = AppDatabase.getDatabase(application).stockDao()
        val stockHistoryDao = AppDatabase.getDatabase(application).stockHistoryDao()
        repository = StockRepository(stockDao, stockHistoryDao)
    }

    val allStocks: StateFlow<List<Stock>> = repository.allStocks
        .combine(sortOrder) { stocks, order ->
            when (order) {
                SortOrder.NAME -> stocks.sortedBy { it.name }
                SortOrder.PRICE -> stocks.sortedByDescending { it.price }
                SortOrder.QUANTITY -> stocks.sortedByDescending { it.quantity }
            }
        }
        .combine(searchQuery) { stocks, query ->
            if (query.isBlank()) {
                stocks
            } else {
                stocks.filter { 
                    it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lowStockItems: StateFlow<List<Stock>> = allStocks
        .map { stocks -> stocks.filter { it.quantity < LOW_STOCK_THRESHOLD } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalStockValue: StateFlow<Double> = allStocks
        .map { stocks -> stocks.sumOf { it.price * it.quantity } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    fun insert(stock: Stock) = viewModelScope.launch {
        repository.insert(stock)
    }

    fun update(stock: Stock) = viewModelScope.launch {
        repository.update(stock)
    }

    fun delete(stock: Stock) = viewModelScope.launch {
        repository.delete(stock)
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