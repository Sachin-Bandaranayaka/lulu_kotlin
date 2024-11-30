package com.example.luluuu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.luluuu.db.AppDatabase
import com.example.luluuu.model.Stock
import com.example.luluuu.repository.StockRepository
import com.example.luluuu.repository.FirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StockRepository
    private val firebaseRepository: FirebaseRepository
    val allStocks: Flow<List<Stock>>

    init {
        val stockDao = AppDatabase.getDatabase(application).stockDao()
        firebaseRepository = FirebaseRepository()
        repository = StockRepository(stockDao, firebaseRepository)
        allStocks = repository.allStocks
    }

    fun insert(stock: Stock) = viewModelScope.launch {
        repository.insert(stock)
    }

    fun update(stock: Stock) = viewModelScope.launch {
        repository.update(stock)
    }

    fun delete(stock: Stock) = viewModelScope.launch {
        repository.delete(stock)
    }
} 