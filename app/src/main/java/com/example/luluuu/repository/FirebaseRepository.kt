package com.example.luluuu.repository

import android.util.Log
import com.example.luluuu.model.Stock
import com.example.luluuu.model.Invoice
import com.example.luluuu.model.Expense
import com.example.luluuu.model.ProductCategory
import com.example.luluuu.db.StockDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseRepository(private val stockDao: StockDao? = null) {
    private val TAG = "FirebaseRepo"
    private val db = Firebase.firestore
    private val stocksCollection = db.collection("stocks")
    private val auth = FirebaseAuth.getInstance()
    private var stocksListener: ListenerRegistration? = null

    private suspend fun ensureAuthenticated(): Boolean {
        return try {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
                Log.d(TAG, "Anonymous auth successful")
                true
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed: ${e.message}")
            if (e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException) {
                Log.w(TAG, "Network connectivity issue - operating in offline mode")
            }
            false
        }
    }

    private fun handleFirebaseError(e: Exception, operation: String) {
        when {
            e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException -> {
                Log.w(TAG, "Network connectivity issue during $operation - continuing in offline mode", e)
            }
            else -> Log.e(TAG, "Error during $operation", e)
        }
    }

    // Stock Operations
    suspend fun syncStock(stock: Stock) {
        try {
            if (!ensureAuthenticated()) {
                Log.w(TAG, "Operating in offline mode")
                return
            }
            
            // First try to find existing document by localId
            val query = stocksCollection.whereEqualTo("localId", stock.id)
            val querySnapshot = query.get().await()
            
            val docRef = if (!querySnapshot.isEmpty) {
                // Use existing document
                val existingDoc = querySnapshot.documents.first()
                Log.d(TAG, "Found existing stock document with ID: ${existingDoc.id}")
                stocksCollection.document(existingDoc.id)
            } else if (!stock.firebaseId.isNullOrBlank()) {
                // Use specified firebaseId
                Log.d(TAG, "Using provided firebaseId: ${stock.firebaseId}")
                stocksCollection.document(stock.firebaseId)
            } else {
                // Create new document
                Log.d(TAG, "Creating new stock document")
                stocksCollection.document()
            }
            
            val stockData = hashMapOf(
                "localId" to stock.id,
                "name" to stock.name,
                "price" to stock.price,
                "quantity" to stock.quantity,
                "description" to stock.description,
                "category" to stock.category.name,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            
            docRef.set(stockData).await()
            
            // Update the stock's firebaseId if it's a new document
            if (stock.firebaseId != docRef.id) {
                stock.firebaseId = docRef.id
                // Update the local database with the new firebaseId
                stockDao?.updateFirebaseId(stock.id, docRef.id)
            }
            
            Log.d(TAG, "Stock synced successfully. LocalId: ${stock.id}, FirebaseId: ${docRef.id}")
        } catch (e: Exception) {
            if (e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException) {
                Log.w(TAG, "Network unavailable - stock will sync when online")
            } else {
                handleFirebaseError(e, "syncStock")
            }
        }
    }

    fun getAllStocks(): Flow<List<Stock>> = callbackFlow {
        try {
            if (!ensureAuthenticated()) {
                Log.w(TAG, "Operating in offline mode")
                trySend(emptyList())
                return@callbackFlow
            }

            stocksListener = stocksCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("cancelled") == true) {
                        Log.d(TAG, "Stocks listener cancelled normally")
                    } else {
                        handleFirebaseError(error, "getAllStocks")
                    }
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                try {
                    val stocks = snapshot.documents.mapNotNull { doc ->
                        try {
                            Stock(
                                id = (doc.get("localId") as? Number)?.toLong() ?: 0L,
                                name = doc.getString("name") ?: "",
                                quantity = (doc.get("quantity") as? Number)?.toInt() ?: 0,
                                price = (doc.get("price") as? Number)?.toDouble() ?: 0.0,
                                firebaseId = doc.id
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing stock document: ${doc.id}", e)
                            null
                        }
                    }
                    trySend(stocks)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing stocks snapshot", e)
                    trySend(emptyList())
                }
            }

            awaitClose {
                Log.d(TAG, "Closing stocks listener")
                stocksListener?.remove()
                stocksListener = null
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Flow cancelled normally")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAllStocks flow", e)
            trySend(emptyList())
        }
    }

    suspend fun deleteStock(stockId: String) {
        try {
            if (!ensureAuthenticated()) {
                Log.w(TAG, "Operating in offline mode")
                return
            }
            
            if (stockId.isBlank()) {
                return // Skip deletion if no valid Firebase ID
            }
            stocksCollection.document(stockId).delete().await()
        } catch (e: Exception) {
            handleFirebaseError(e, "deleteStock")
        }
    }

    suspend fun updateStock(stockId: String, stock: Stock) {
        try {
            if (!ensureAuthenticated()) {
                Log.w(TAG, "Operating in offline mode")
                return
            }
            
            if (stockId.isBlank()) {
                Log.w(TAG, "No Firebase ID provided for stock update")
                return
            }
            
            Log.d(TAG, "Updating stock in Firebase. ID: $stockId, Name: ${stock.name}")
            
            val stockData = hashMapOf(
                "localId" to stock.id,
                "name" to stock.name,
                "price" to stock.price,
                "quantity" to stock.quantity,
                "description" to stock.description,
                "category" to stock.category.name,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            
            stocksCollection.document(stockId).set(stockData).await()
            Log.d(TAG, "Stock updated successfully in Firebase. ID: $stockId")
        } catch (e: Exception) {
            if (e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException) {
                Log.w(TAG, "Network unavailable - stock update will sync when online")
            } else {
                handleFirebaseError(e, "updateStock")
            }
        }
    }

    suspend fun addStock(stock: Stock): String {
        try {
            if (!ensureAuthenticated()) {
                Log.w(TAG, "Operating in offline mode")
                return ""
            }
            
            Log.d(TAG, "Adding new stock to Firebase: ${stock.name}")
            
            val stockData = hashMapOf(
                "localId" to stock.id,
                "name" to stock.name,
                "price" to stock.price,
                "quantity" to stock.quantity,
                "description" to stock.description,
                "category" to stock.category.name,
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            
            val docRef = stocksCollection.add(stockData).await()
            Log.d(TAG, "Stock added successfully to Firebase. ID: ${docRef.id}")
            return docRef.id
        } catch (e: Exception) {
            if (e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException) {
                Log.w(TAG, "Network unavailable - stock will sync when online")
            } else {
                handleFirebaseError(e, "addStock")
            }
            return ""
        }
    }

    fun searchStocksByName(query: String): Flow<List<Stock>> = callbackFlow {
        try {
            val authenticated = ensureAuthenticated()
            if (!authenticated) {
                Log.w(TAG, "Operating in offline mode")
                trySend(emptyList())
                return@callbackFlow
            }
            
            val registration = stocksCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + '\uf8ff')
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (error.message?.contains("cancelled") == true) {
                            Log.d(TAG, "Stocks listener cancelled normally")
                        } else {
                            handleFirebaseError(error, "searchStocksByName")
                        }
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        try {
                            val stocks = querySnapshot.documents.mapNotNull { doc ->
                                try {
                                    Stock(
                                        id = (doc.get("localId") as? Number)?.toLong() ?: 0L,
                                        name = doc.getString("name") ?: "",
                                        quantity = (doc.get("quantity") as? Number)?.toInt() ?: 0,
                                        price = (doc.get("price") as? Number)?.toDouble() ?: 0.0,
                                        firebaseId = doc.id
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing stock document: ${doc.id}", e)
                                    null
                                }
                            }
                            trySend(stocks)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing stocks snapshot", e)
                            trySend(emptyList())
                        }
                    }
                }

            awaitClose {
                Log.d(TAG, "Closing stocks listener")
                registration.remove()
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Flow cancelled normally")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchStocksByName flow", e)
            trySend(emptyList())
        }
    }

    fun getLowStockItems(threshold: Int = 5): Flow<List<Stock>> = callbackFlow {
        try {
            val authenticated = ensureAuthenticated()
            if (!authenticated) {
                Log.w(TAG, "Operating in offline mode")
                trySend(emptyList())
                return@callbackFlow
            }
            
            val registration = stocksCollection
                .whereLessThanOrEqualTo("quantity", threshold)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (error.message?.contains("cancelled") == true) {
                            Log.d(TAG, "Stocks listener cancelled normally")
                        } else {
                            handleFirebaseError(error, "getLowStockItems")
                        }
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        try {
                            val stocks = querySnapshot.documents.mapNotNull { doc ->
                                try {
                                    Stock(
                                        id = (doc.get("localId") as? Number)?.toLong() ?: 0L,
                                        name = doc.getString("name") ?: "",
                                        quantity = (doc.get("quantity") as? Number)?.toInt() ?: 0,
                                        price = (doc.get("price") as? Number)?.toDouble() ?: 0.0,
                                        firebaseId = doc.id
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing stock document: ${doc.id}", e)
                                    null
                                }
                            }
                            trySend(stocks)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing stocks snapshot", e)
                            trySend(emptyList())
                        }
                    }
                }

            awaitClose {
                Log.d(TAG, "Closing stocks listener")
                registration.remove()
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Flow cancelled normally")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in getLowStockItems flow", e)
            trySend(emptyList())
        }
    }

    suspend fun cleanupTestData() {
        try {
            if (!ensureAuthenticated()) {
                Log.w(TAG, "Operating in offline mode")
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
            handleFirebaseError(e, "cleanupTestData")
        }
    }

    // Invoice Operations
    suspend fun syncInvoice(invoice: Invoice) {
        try {
            if (!ensureAuthenticated()) {
                Log.w(TAG, "Operating in offline mode")
                return
            }
            
            db.collection("invoices")
                .document(invoice.id.toString())
                .set(invoice)
                .await()
        } catch (e: Exception) {
            handleFirebaseError(e, "syncInvoice")
        }
    }

    fun getAllInvoices(): Flow<List<Invoice>> = callbackFlow {
        try {
            val authenticated = ensureAuthenticated()
            if (!authenticated) {
                Log.w(TAG, "Operating in offline mode")
                trySend(emptyList())
                return@callbackFlow
            }
            
            val registration = db.collection("invoices")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (error.message?.contains("cancelled") == true) {
                            Log.d(TAG, "Invoices listener cancelled normally")
                        } else {
                            handleFirebaseError(error, "getAllInvoices")
                        }
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        try {
                            val invoices = querySnapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(Invoice::class.java)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing invoice document: ${doc.id}", e)
                                    null
                                }
                            }
                            trySend(invoices)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing invoices snapshot", e)
                            trySend(emptyList())
                        }
                    }
                }

            awaitClose {
                Log.d(TAG, "Closing invoices listener")
                registration.remove()
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Flow cancelled normally")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAllInvoices flow", e)
            trySend(emptyList())
        }
    }

    // Expense Operations
    suspend fun syncExpense(expense: Expense) {
        try {
            if (!ensureAuthenticated()) {
                Log.w(TAG, "Operating in offline mode")
                return
            }
            
            db.collection("expenses")
                .document(expense.id.toString())
                .set(expense)
                .await()
        } catch (e: Exception) {
            handleFirebaseError(e, "syncExpense")
        }
    }

    fun getAllExpenses(): Flow<List<Expense>> = callbackFlow {
        try {
            val authenticated = ensureAuthenticated()
            if (!authenticated) {
                Log.w(TAG, "Operating in offline mode")
                trySend(emptyList())
                return@callbackFlow
            }
            
            val registration = db.collection("expenses")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (error.message?.contains("cancelled") == true) {
                            Log.d(TAG, "Expenses listener cancelled normally")
                        } else {
                            handleFirebaseError(error, "getAllExpenses")
                        }
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->
                        try {
                            val expenses = querySnapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(Expense::class.java)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing expense document: ${doc.id}", e)
                                    null
                                }
                            }
                            trySend(expenses)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing expenses snapshot", e)
                            trySend(emptyList())
                        }
                    }
                }

            awaitClose {
                Log.d(TAG, "Closing expenses listener")
                registration.remove()
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Flow cancelled normally")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAllExpenses flow", e)
            trySend(emptyList())
        }
    }

    fun cleanup() {
        stocksListener?.remove()
        stocksListener = null
    }
}