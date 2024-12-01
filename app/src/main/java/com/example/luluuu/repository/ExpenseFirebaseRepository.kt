package com.example.luluuu.repository

import android.util.Log
import com.example.luluuu.model.Expense
import com.example.luluuu.model.ExpenseCategory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
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
                "id" to expense.id,
                "firebaseId" to ""  // Add this to ensure consistency
            )

            val documentRef = expensesCollection.add(expenseMap).await()
            Log.d(TAG, "Expense added successfully with ID: ${documentRef.id}")
            
            // Update the document with its own ID
            expensesCollection.document(documentRef.id)
                .update("firebaseId", documentRef.id)
                .await()
            
            // Return a new expense with the Firebase ID
            return expense.copy(firebaseId = documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting expense: ${e.message}")
            throw when {
                e.message?.contains("PERMISSION_DENIED") == true -> 
                    Exception("Permission denied. Please check Firebase security rules.", e)
                e.message?.contains("NOT_FOUND") == true ->
                    Exception("Database not found. Please check your Firebase configuration.", e)
                else -> Exception("Failed to save expense: ${e.message}", e)
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
                "id" to expense.id,
                "firebaseId" to expense.firebaseId  // Add this to ensure consistency
            )
            
            expensesCollection.document(expense.firebaseId)
                .set(expenseMap)  // Use set instead of update to ensure all fields are written
                .await()
            
            Log.d(TAG, "Expense updated successfully in Firebase with ID: ${expense.firebaseId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating expense: ${e.message}")
            throw when {
                e.message?.contains("PERMISSION_DENIED") == true -> 
                    Exception("Permission denied. Please check Firebase security rules.", e)
                e.message?.contains("NOT_FOUND") == true ->
                    Exception("Expense not found in Firebase.", e)
                else -> Exception("Failed to update expense in Firebase: ${e.message}", e)
            }
        }
    }

    suspend fun deleteExpense(expense: Expense) {
        try {
            if (expense.firebaseId.isBlank()) {
                Log.w(TAG, "Deleting expense without Firebase ID - skipping Firebase deletion")
                return
            }

            // First verify the document exists
            val docSnapshot = expensesCollection.document(expense.firebaseId).get().await()
            if (!docSnapshot.exists()) {
                Log.w(TAG, "Document ${expense.firebaseId} does not exist in Firebase")
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

    fun getAllExpenses(): Flow<List<Expense>> = channelFlow {
        try {
            val registration = expensesCollection
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listen failed: ${error.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        launch {
                            val expenses = querySnapshot.documents.mapNotNull { document ->
                                try {
                                    Expense(
                                        id = (document.getLong("id") ?: 0L),
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
                            send(expenses)
                        }
                    }
                }

            awaitClose { registration.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error in real-time expenses stream: ${e.message}")
            launch { send(emptyList()) }
        }
    }

    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>> = channelFlow {
        try {
            val registration = expensesCollection
                .whereEqualTo("category", category.name)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listen failed: ${error.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        launch {
                            val expenses = querySnapshot.documents.mapNotNull { document ->
                                try {
                                    Expense(
                                        id = (document.getLong("id") ?: 0L),
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
                            send(expenses)
                        }
                    }
                }

            awaitClose { registration.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error in real-time category expenses stream: ${e.message}")
            launch { send(emptyList()) }
        }
    }

    fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>> = channelFlow {
        try {
            val registration = expensesCollection
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listen failed: ${error.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        launch {
                            val expenses = querySnapshot.documents.mapNotNull { document ->
                                try {
                                    Expense(
                                        id = (document.getLong("id") ?: 0L),
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
                            send(expenses)
                        }
                    }
                }

            awaitClose { registration.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error in real-time date range expenses stream: ${e.message}")
            launch { send(emptyList()) }
        }
    }
}
