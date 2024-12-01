package com.example.luluuu.repository

import android.util.Log
import com.example.luluuu.model.Stock
import com.example.luluuu.model.Invoice
import com.example.luluuu.model.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlinx.coroutines.delay

class FirebaseRepository {
    private val db = Firebase.firestore
    private val stocksCollection = db.collection("stocks")
    private val auth = FirebaseAuth.getInstance()

    private suspend fun ensureAuthenticated(): Boolean {
        return try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
                Log.d("FirebaseRepo", "Anonymous auth successful")
                true
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Anonymous auth failed", e)
            false
        }
    }

    // Stock Operations
    suspend fun syncStock(stock: Stock) {
        try {
            if (!ensureAuthenticated()) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                return
            }
            
            db.collection("stocks")
                .document(stock.id.toString())
                .set(stock)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error syncing stock", e)
        }
    }

    fun getAllStocks(): Flow<List<Stock>> = channelFlow {
        try {
            val authenticated = ensureAuthenticated()
            if (!authenticated) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                send(emptyList())
                return@channelFlow
            }
            
            val registration = stocksCollection
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseRepo", "Listen failed: ${error.message}")
                        launch { send(emptyList()) }
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        launch {
                            val stocks = querySnapshot.documents.mapNotNull { doc ->
                                doc.toObject(Stock::class.java)?.copy(
                                    firebaseId = doc.id,
                                    name = doc.getString("name") ?: "",
                                    price = doc.getDouble("price") ?: 0.0,
                                    quantity = doc.getLong("quantity")?.toInt() ?: 0,
                                    description = doc.getString("description") ?: ""
                                )
                            }
                            send(stocks)
                        }
                    }
                }

            awaitClose { registration.remove() }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error in real-time stocks stream: ${e.message}")
            launch { send(emptyList()) }
        }
    }

    suspend fun deleteStock(stockId: String) {
        try {
            if (!ensureAuthenticated()) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                return
            }
            
            if (stockId.isBlank()) {
                return // Skip deletion if no valid Firebase ID
            }
            stocksCollection.document(stockId).delete().await()
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error deleting stock", e)
        }
    }

    suspend fun addStock(stock: Stock): String {
        try {
            if (!ensureAuthenticated()) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                return ""
            }
            
            val stockData = hashMapOf(
                "name" to stock.name,
                "price" to stock.price,
                "quantity" to stock.quantity,
                "description" to stock.description
            )
            val docRef = stocksCollection.add(stockData).await()
            return docRef.id
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error adding stock", e)
            return ""
        }
    }

    suspend fun updateStock(stockId: String, stock: Stock) {
        try {
            if (!ensureAuthenticated()) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                return
            }
            
            if (stockId.isBlank()) {
                return // Skip update if no valid Firebase ID
            }
            
            val stockData = hashMapOf(
                "name" to stock.name,
                "price" to stock.price,
                "quantity" to stock.quantity,
                "description" to stock.description
            )
            stocksCollection.document(stockId).set(stockData).await()
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error updating stock", e)
        }
    }

    fun searchStocksByName(query: String): Flow<List<Stock>> = channelFlow {
        try {
            val authenticated = ensureAuthenticated()
            if (!authenticated) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                send(emptyList())
                return@channelFlow
            }
            
            val registration = stocksCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + '\uf8ff')
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseRepo", "Listen failed: ${error.message}")
                        launch { send(emptyList()) }
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        launch {
                            val stocks = querySnapshot.documents.mapNotNull { doc ->
                                doc.toObject(Stock::class.java)?.copy(
                                    firebaseId = doc.id
                                )
                            }
                            send(stocks)
                        }
                    }
                }

            awaitClose { registration.remove() }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error in search stocks stream: ${e.message}")
            launch { send(emptyList()) }
        }
    }

    fun getLowStockItems(threshold: Int = 5): Flow<List<Stock>> = channelFlow {
        try {
            val authenticated = ensureAuthenticated()
            if (!authenticated) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                send(emptyList())
                return@channelFlow
            }
            
            val registration = stocksCollection
                .whereLessThanOrEqualTo("quantity", threshold)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseRepo", "Listen failed: ${error.message}")
                        launch { send(emptyList()) }
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        launch {
                            val stocks = querySnapshot.documents.mapNotNull { doc ->
                                doc.toObject(Stock::class.java)?.copy(
                                    firebaseId = doc.id
                                )
                            }
                            send(stocks)
                        }
                    }
                }

            awaitClose { registration.remove() }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error in real-time low stock stream: ${e.message}")
            launch { send(emptyList()) }
        }
    }

    suspend fun cleanupTestData() {
        try {
            if (!ensureAuthenticated()) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                return
            }
            
            val snapshot = stocksCollection
                .whereEqualTo("name", "Test Product")
                .get()
                .await()
            
            for (doc in snapshot.documents) {
                doc.reference.delete()
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error cleaning up test data", e)
        }
    }

    // Invoice Operations
    suspend fun syncInvoice(invoice: Invoice) {
        try {
            if (!ensureAuthenticated()) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                return
            }
            
            db.collection("invoices")
                .document(invoice.id.toString())
                .set(invoice)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error syncing invoice", e)
        }
    }

    fun getAllInvoices(): Flow<List<Invoice>> = channelFlow {
        try {
            val authenticated = ensureAuthenticated()
            if (!authenticated) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                send(emptyList())
                return@channelFlow
            }
            
            val registration = db.collection("invoices")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseRepo", "Listen failed: ${error.message}")
                        launch { send(emptyList()) }
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        launch {
                            val invoices = querySnapshot.documents.mapNotNull { doc ->
                                doc.toObject(Invoice::class.java)
                            }
                            send(invoices)
                        }
                    }
                }

            awaitClose { registration.remove() }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error in real-time invoices stream: ${e.message}")
            launch { send(emptyList()) }
        }
    }

    // Expense Operations
    suspend fun syncExpense(expense: Expense) {
        try {
            if (!ensureAuthenticated()) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                return
            }
            
            db.collection("expenses")
                .document(expense.id.toString())
                .set(expense)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error syncing expense", e)
        }
    }

    fun getAllExpenses(): Flow<List<Expense>> = channelFlow {
        try {
            val authenticated = ensureAuthenticated()
            if (!authenticated) {
                Log.w("FirebaseRepo", "Operating in offline mode")
                send(emptyList())
                return@channelFlow
            }
            
            val registration = db.collection("expenses")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseRepo", "Listen failed: ${error.message}")
                        launch { send(emptyList()) }
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        launch {
                            val expenses = querySnapshot.documents.mapNotNull { doc ->
                                doc.toObject(Expense::class.java)
                            }
                            send(expenses)
                        }
                    }
                }

            awaitClose { registration.remove() }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error in real-time expenses stream: ${e.message}")
            launch { send(emptyList()) }
        }
    }
}