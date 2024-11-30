package com.example.luluuu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.luluuu.db.AppDatabase
import com.example.luluuu.model.Expense
import com.example.luluuu.model.ExpenseCategory
import com.example.luluuu.repository.ExpenseRepository
import com.example.luluuu.repository.ExpenseFirebaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExpenseRepository
    private val firebaseRepository: ExpenseFirebaseRepository
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val allExpenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    init {
        val expenseDao = AppDatabase.getDatabase(application).expenseDao()
        repository = ExpenseRepository(expenseDao)
        firebaseRepository = ExpenseFirebaseRepository()
        
        // Start collecting expenses
        viewModelScope.launch {
            combine(
                repository.allExpenses,
                firebaseRepository.getAllExpenses()
            ) { localExpenses, firebaseExpenses ->
                // Use Firebase data if available, otherwise use local data
                if (firebaseExpenses.isNotEmpty()) {
                    firebaseExpenses
                } else {
                    localExpenses
                }
            }.collect { expenses ->
                _expenses.value = expenses
            }
        }
    }

    fun insert(expense: Expense) = viewModelScope.launch {
        try {
            // First save to Firebase to get the ID
            val expenseWithId = firebaseRepository.insertExpense(expense)
            // Then save to local database with the Firebase ID
            repository.insert(expenseWithId)
            // Refresh the expenses list
            refreshExpenses()
        } catch (e: Exception) {
            Log.e("ExpenseViewModel", "Error inserting expense: ${e.message}")
            throw e
        }
    }

    fun update(expense: Expense) = viewModelScope.launch {
        try {
            if (expense.firebaseId.isNotBlank()) {
                // Update Firebase first
                firebaseRepository.updateExpense(expense)
                // Then update local database
                repository.update(expense)
            } else {
                // If no Firebase ID, treat it as a new expense in Firebase
                val expenseWithId = firebaseRepository.insertExpense(expense)
                // Update local expense with the new Firebase ID
                repository.update(expenseWithId)
            }
            // Refresh the expenses list
            refreshExpenses()
        } catch (e: Exception) {
            Log.e("ExpenseViewModel", "Error updating expense: ${e.message}")
            // Only throw if it's not a Firebase-related error
            if (e.message?.contains("Firebase") != true) {
                throw e
            }
        }
    }

    fun delete(expense: Expense) = viewModelScope.launch {
        try {
            // First delete from Firebase if it exists
            if (expense.firebaseId.isNotBlank()) {
                firebaseRepository.deleteExpense(expense)
            }
            // Then delete from local database
            repository.delete(expense)
            // Refresh the expenses list
            refreshExpenses()
        } catch (e: Exception) {
            Log.e("ExpenseViewModel", "Error deleting expense: ${e.message}")
            // Only throw if it's not a Firebase-related error
            if (e.message?.contains("Firebase") != true) {
                throw e
            }
        }
    }

    private suspend fun refreshExpenses() {
        try {
            val firebaseExpenses = firebaseRepository.getAllExpenses().first()
            if (firebaseExpenses.isNotEmpty()) {
                _expenses.value = firebaseExpenses
            } else {
                _expenses.value = repository.allExpenses.first()
            }
        } catch (e: Exception) {
            Log.e("ExpenseViewModel", "Error refreshing expenses: ${e.message}")
        }
    }

    // Firebase specific functions
    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>> {
        return firebaseRepository.getExpensesByCategory(category)
    }

    fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>> {
        return firebaseRepository.getExpensesByDateRange(startDate, endDate)
    }
}