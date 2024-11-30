package com.example.luluuu.repository

import com.example.luluuu.model.Stock
import com.example.luluuu.model.Invoice
import com.example.luluuu.model.Expense
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date

class FirebaseRepository {
    private val db = Firebase.firestore
    private val stocksCollection = db.collection("stocks")

    // Stock Operations
    suspend fun syncStock(stock: Stock) {
        db.collection("stocks")
            .document(stock.id.toString())
            .set(stock)
            .await()
    }

    fun getAllStocks(): Flow<List<Stock>> = flow {
        val snapshot = stocksCollection.get().await()
        val stocks = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Stock::class.java)?.copy(id = doc.id.toLongOrNull() ?: 0)
        }
        emit(stocks)
    }

    suspend fun deleteStock(stockId: String) {
        stocksCollection.document(stockId).delete().await()
    }

    suspend fun addStock(stock: Stock): String {
        val docRef = stocksCollection.add(stock).await()
        return docRef.id
    }

    suspend fun updateStock(stockId: String, stock: Stock) {
        stocksCollection.document(stockId).set(stock).await()
    }

    fun searchStocksByName(query: String): Flow<List<Stock>> = flow {
        val snapshot = stocksCollection
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + '\uf8ff')
            .get()
            .await()
        
        val stocks = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Stock::class.java)?.copy(id = doc.id.toLongOrNull() ?: 0)
        }
        emit(stocks)
    }

    fun getLowStockItems(threshold: Int = 5): Flow<List<Stock>> = flow {
        val snapshot = stocksCollection
            .whereLessThanOrEqualTo("quantity", threshold)
            .get()
            .await()
        
        val stocks = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Stock::class.java)?.copy(id = doc.id.toLongOrNull() ?: 0)
        }
        emit(stocks)
    }

    suspend fun cleanupTestData() {
        val snapshot = stocksCollection
            .whereEqualTo("name", "Test Product")
            .get()
            .await()
        
        for (doc in snapshot.documents) {
            doc.reference.delete()
        }
    }

    // Invoice Operations
    suspend fun syncInvoice(invoice: Invoice) {
        db.collection("invoices")
            .document(invoice.id.toString())
            .set(invoice)
            .await()
    }

    fun getAllInvoices(): Flow<List<Invoice>> = flow {
        val snapshot = db.collection("invoices")
            .get()
            .await()
        
        val invoices = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Invoice::class.java)
        }
        emit(invoices)
    }

    // Expense Operations
    suspend fun syncExpense(expense: Expense) {
        db.collection("expenses")
            .document(expense.id.toString())
            .set(expense)
            .await()
    }

    fun getAllExpenses(): Flow<List<Expense>> = flow {
        val snapshot = db.collection("expenses")
            .get()
            .await()
        
        val expenses = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Expense::class.java)
        }
        emit(expenses)
    }
} 