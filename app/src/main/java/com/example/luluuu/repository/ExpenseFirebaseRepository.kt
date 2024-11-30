package com.example.luluuu.repository

import android.util.Log
import com.example.luluuu.model.Expense
import com.example.luluuu.model.ExpenseCategory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date

class ExpenseFirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val expensesCollection = db.collection("expenses")
    private val TAG = "ExpenseFirebaseRepo"

    suspend fun insertExpense(expense: Expense): Expense {
        try {
            val expenseMap = mapOf(
                "description" to expense.description,
                "amount" to expense.amount,
                "category" to expense.category.name,
                "date" to expense.date,
                "id" to expense.id
            ) as Map<String, Any>

            val documentRef = expensesCollection.add(expenseMap).await()
            Log.d(TAG, "Expense added successfully with ID: ${documentRef.id}")
            
            // Return a new expense with the Firebase ID
            return expense.copy(firebaseId = documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting expense: ${e.message}")
            when {
                e.message?.contains("PERMISSION_DENIED") == true -> 
                    throw Exception("Permission denied. Please check Firebase security rules.", e)
                e.message?.contains("NOT_FOUND") == true ->
                    throw Exception("Database not found. Please check your Firebase configuration.", e)
                else -> throw Exception("Failed to save expense: ${e.message}", e)
            }
        }
    }

    suspend fun updateExpense(expense: Expense) {
        try {
            if (expense.firebaseId.isBlank()) {
                throw Exception("Cannot update expense: No Firebase ID found")
            }

            val expenseMap = mapOf(
                "description" to expense.description,
                "amount" to expense.amount,
                "category" to expense.category.name,
                "date" to expense.date,
                "id" to expense.id
            ) as Map<String, Any>
            
            expensesCollection.document(expense.firebaseId)
                .update(expenseMap)
                .await()
            
            Log.d(TAG, "Expense updated successfully in Firebase with ID: ${expense.firebaseId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating expense: ${e.message}")
            when {
                e.message?.contains("PERMISSION_DENIED") == true -> 
                    throw Exception("Permission denied. Please check Firebase security rules.", e)
                e.message?.contains("NOT_FOUND") == true ->
                    throw Exception("Expense not found in Firebase.", e)
                else -> throw Exception("Failed to update expense in Firebase: ${e.message}", e)
            }
        }
    }

    suspend fun deleteExpense(expense: Expense) {
        try {
            // If no Firebase ID, just log a warning and continue
            if (expense.firebaseId.isBlank()) {
                Log.w(TAG, "Deleting expense without Firebase ID - skipping Firebase deletion")
                return
            }

            expensesCollection.document(expense.firebaseId)
                .delete()
                .await()
            
            Log.d(TAG, "Expense deleted successfully from Firebase with ID: ${expense.firebaseId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting expense: ${e.message}")
            throw when {
                e.message?.contains("PERMISSION_DENIED") == true -> 
                    Exception("Permission denied. Please check Firebase security rules.", e)
                e.message?.contains("NOT_FOUND") == true ->
                    Exception("Expense not found in Firebase.", e)
                else -> Exception("Failed to delete expense from Firebase: ${e.message}", e)
            }
        }
    }

    fun getAllExpenses(): Flow<List<Expense>> = flow {
        try {
            val snapshot = expensesCollection
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val expenses = snapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data
                    if (data != null) {
                        Expense(
                            id = (data["id"] as? Number)?.toLong() ?: 0L,
                            description = data["description"] as? String ?: "",
                            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                            category = ExpenseCategory.valueOf(data["category"] as? String ?: ExpenseCategory.OTHER.name),
                            date = (data["date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                            firebaseId = document.id
                        )
                    } else null
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing expense document: ${e.message}")
                    null
                }
            }
            emit(expenses)
            Log.d(TAG, "Successfully retrieved ${expenses.size} expenses from Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all expenses: ${e.message}")
            emit(emptyList())
        }
    }

    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>> = flow {
        try {
            val snapshot = expensesCollection
                .whereEqualTo("category", category.name)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val expenses = snapshot.documents.mapNotNull { document ->
                try {
                    Expense(
                        description = document.getString("description") ?: "",
                        amount = document.getDouble("amount") ?: 0.0,
                        category = ExpenseCategory.valueOf(document.getString("category") ?: ExpenseCategory.OTHER.name),
                        date = document.getDate("date") ?: Date(),
                        firebaseId = document.id
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing expense document: ${e.message}")
                    null
                }
            }
            emit(expenses)
            Log.d(TAG, "Successfully retrieved ${expenses.size} expenses for category $category")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting expenses by category: ${e.message}")
            emit(emptyList())
        }
    }

    fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>> = flow {
        try {
            val snapshot = expensesCollection
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val expenses = snapshot.documents.mapNotNull { document ->
                try {
                    Expense(
                        description = document.getString("description") ?: "",
                        amount = document.getDouble("amount") ?: 0.0,
                        category = ExpenseCategory.valueOf(document.getString("category") ?: ExpenseCategory.OTHER.name),
                        date = document.getDate("date") ?: Date(),
                        firebaseId = document.id
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing expense document: ${e.message}")
                    null
                }
            }
            emit(expenses)
            Log.d(TAG, "Successfully retrieved ${expenses.size} expenses for date range")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting expenses by date range: ${e.message}")
            emit(emptyList())
        }
    }
}
