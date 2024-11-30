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

    // Stock Operations
    suspend fun syncStock(stock: Stock) {
        db.collection("stocks")
            .document(stock.id.toString())
            .set(stock)
            .await()
    }

    fun getAllStocks(): Flow<List<Stock>> = flow {
        val snapshot = db.collection("stocks")
            .get()
            .await()
        
        val stocks = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Stock::class.java)
        }
        emit(stocks)
    }

    suspend fun deleteStock(stockId: Long) {
        db.collection("stocks")
            .document(stockId.toString())
            .delete()
            .await()
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