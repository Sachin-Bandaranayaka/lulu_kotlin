package com.example.luluuu.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.luluuu.db.AppDatabase
import com.example.luluuu.db.StockDao
import com.example.luluuu.db.StockHistoryDao
import com.example.luluuu.model.Stock
import com.example.luluuu.model.StockHistory
import com.example.luluuu.model.ProductCategory
import com.example.luluuu.repository.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.*

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "StockViewModel"
    private val database = AppDatabase.getDatabase(application)
    private val stockDao: StockDao = database.stockDao()
    private val stockHistoryDao: StockHistoryDao = database.stockHistoryDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val stocksCollection = firestore.collection("stocks")
    private val stockHistoryCollection = firestore.collection("stock_history")
    private val firebaseRepository = FirebaseRepository(stockDao)
    
    init {
        Log.d(TAG, "Initializing StockViewModel")
        viewModelScope.launch {
            syncFromFirestore()
        }
        setupFirestoreListener()
    }

    private fun setupFirestoreListener() {
        Log.d(TAG, "Setting up Firestore listener")
        stocksCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to Firestore changes", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                Log.d(TAG, "Firestore changes detected, documents count: ${snapshot.documents.size}")
                viewModelScope.launch {
                    syncFromFirestore()
                }
            }
        }
    }

    private suspend fun syncFromFirestore() {
        Log.d(TAG, "Starting Firestore sync")
        try {
            // Get all local stocks first
            val localStocks = stockDao.getAllStocksSync()
            Log.d(TAG, "Local stocks count: ${localStocks.size}")

            // Get all Firestore stocks
            val snapshot = stocksCollection.get().await()
            Log.d(TAG, "Firestore stocks count: ${snapshot.documents.size}")

            val firestoreStockIds = mutableSetOf<String>()
            val processedLocalIds = mutableSetOf<Long>()

            // Process Firestore documents
            snapshot.documents.forEach { doc ->
                try {
                    Log.d(TAG, "Processing Firestore document: ${doc.id}")
                    val localId = (doc.get("localId") as? Number)?.toLong()
                    
                    // Skip if we've already processed this local ID
                    if (localId != null && localId in processedLocalIds) {
                        Log.d(TAG, "Skipping duplicate local ID: $localId")
                        return@forEach
                    }

                    val stock = Stock(
                        id = localId ?: 0L,
                        name = doc.getString("name") ?: "",
                        quantity = (doc.get("quantity") as? Number)?.toInt() ?: 0,
                        price = (doc.get("price") as? Number)?.toDouble() ?: 0.0,
                        description = doc.getString("description") ?: "",
                        category = try {
                            ProductCategory.valueOf(doc.getString("category") ?: "SOAP_BAR")
                        } catch (e: Exception) {
                            ProductCategory.SOAP_BAR
                        },
                        firebaseId = doc.id
                    )

                    firestoreStockIds.add(doc.id)
                    if (localId != null) {
                        processedLocalIds.add(localId)
                    }

                    // Find existing stock by Firebase ID first
                    val existingStockByFirebaseId = localStocks.find { it.firebaseId == doc.id }
                    val existingStockByLocalId = if (localId != null) stockDao.getStockById(localId) else null
                    
                    when {
                        existingStockByFirebaseId != null -> {
                            Log.d(TAG, "Updating existing stock by Firebase ID: ${stock.name}")
                            stockDao.update(stock.copy(id = existingStockByFirebaseId.id))
                        }
                        existingStockByLocalId != null -> {
                            Log.d(TAG, "Updating existing stock by Local ID: ${stock.name}")
                            stockDao.update(stock)
                        }
                        else -> {
                            Log.d(TAG, "Inserting new stock: ${stock.name}")
                            val newId = stockDao.insert(stock)
                            // Update Firestore with the new local ID
                            stocksCollection.document(doc.id).update("localId", newId)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing stock document: ${doc.id}", e)
                }
            }

            // Remove local stocks that don't exist in Firestore
            localStocks.forEach { localStock ->
                if (localStock.firebaseId !in firestoreStockIds) {
                    Log.d(TAG, "Deleting local stock that's not in Firestore: ${localStock.name}")
                    stockDao.delete(localStock)
                }
            }

            Log.d(TAG, "Firestore sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncFromFirestore", e)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.NAME_ASC)
    
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val stocks = combine(
        stockDao.getAllStocks(),
        searchQuery,
        sortOrder
    ) { stocks, query, order ->
        var filteredStocks = stocks.filter {
            it.name.contains(query, ignoreCase = true)
        }
        
        when (order) {
            SortOrder.NAME_ASC -> filteredStocks.sortedBy { it.name }
            SortOrder.NAME_DESC -> filteredStocks.sortedByDescending { it.name }
            SortOrder.QUANTITY_ASC -> filteredStocks.sortedBy { it.quantity }
            SortOrder.QUANTITY_DESC -> filteredStocks.sortedByDescending { it.quantity }
            SortOrder.PRICE_ASC -> filteredStocks.sortedBy { it.price }
            SortOrder.PRICE_DESC -> filteredStocks.sortedByDescending { it.price }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val lowStockItems = stocks.map { stockList ->
        stockList.filter { it.quantity <= LOW_STOCK_THRESHOLD }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalStockValue = stocks.map { stockList ->
        stockList.sumOf { it.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    companion object {
        const val LOW_STOCK_THRESHOLD = 5
    }

    suspend fun getStockById(id: Long) = stockDao.getStockById(id)

    suspend fun refreshStocks() {
        withContext(Dispatchers.IO) {
            try {
                // Clear local caches
                stockDao.clearCache()
                stockHistoryDao.clearCache()
                
                // Fetch fresh data from Firebase
                val stocks = stocksCollection.get().await().documents.mapNotNull { doc ->
                    doc.toObject(Stock::class.java)?.apply {
                        firebaseId = doc.id  // Store the Firebase document ID
                    }
                }
                
                // Update local database
                stockDao.insertAll(stocks)
                
                // Sync stock history
                val historySnapshot = stockHistoryCollection.get().await()
                historySnapshot.documents.forEach { doc ->
                    try {
                        val stockHistory = StockHistory(
                            id = doc.id,
                            stockId = (doc.get("stockId") as Number).toLong(),
                            date = doc.getTimestamp("date")?.toDate() ?: Date(),
                            oldQuantity = (doc.get("oldQuantity") as Number).toInt(),
                            newQuantity = (doc.get("newQuantity") as Number).toInt(),
                            oldPrice = (doc.get("oldPrice") as Number).toDouble(),
                            newPrice = (doc.get("newPrice") as Number).toDouble(),
                            action = doc.getString("action") ?: "",
                            invoiceNumber = (doc.get("invoiceNumber") as? Number)?.toInt(),
                            regularQuantity = (doc.get("regularQuantity") as? Number)?.toInt() ?: 0,
                            freeQuantity = (doc.get("freeQuantity") as? Number)?.toInt() ?: 0,
                            description = doc.getString("description") ?: ""
                        )
                        stockHistoryDao.insert(stockHistory)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing stock history document: ${doc.id}", e)
                    }
                }
                
                Log.d(TAG, "Successfully refreshed ${stocks.size} stocks and ${historySnapshot.size()} history records")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing stocks", e)
                throw e
            }
        }
    }

    fun update(stock: Stock) = viewModelScope.launch {
        try {
            // Get the old stock data before updating
            val oldStock = stockDao.getStockById(stock.id)
            
            // Update in Room database
            stockDao.update(stock)
            
            // Create stock history entry if quantity or price changed
            oldStock?.let { old ->
                if (old.quantity != stock.quantity || old.price != stock.price) {
                    val stockHistory = StockHistory(
                        stockId = stock.id,
                        date = Date(),
                        oldQuantity = old.quantity,
                        newQuantity = stock.quantity,
                        oldPrice = old.price,
                        newPrice = stock.price,
                        action = "UPDATE",
                        description = "Stock updated"
                    )
                    
                    // Add history to local database
                    stockHistoryDao.insert(stockHistory)
                    
                    // Add history to Firestore
                    stockHistoryCollection.document(stockHistory.id)
                        .set(stockHistory)
                        .addOnSuccessListener {
                            Log.d("StockViewModel", "Stock history added to Firestore")
                        }
                        .addOnFailureListener { e ->
                            Log.e("StockViewModel", "Error adding stock history to Firestore: ${e.message}")
                        }
                }
            }
            
            // Update in Firestore
            if (stock.firebaseId.isNotEmpty()) {
                stocksCollection.document(stock.firebaseId)
                    .set(stock)
                    .addOnSuccessListener {
                        Log.d("StockViewModel", "Stock updated successfully in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e("StockViewModel", "Error updating stock in Firestore: ${e.message}")
                    }
            } else {
                Log.w("StockViewModel", "Stock has no Firebase ID, creating new document")
                val newFirebaseId = stocksCollection.document().id
                val updatedStock = stock.copy(firebaseId = newFirebaseId)
                
                // Update local stock with new Firebase ID
                stockDao.update(updatedStock)
                
                // Add to Firestore
                stocksCollection.document(newFirebaseId)
                    .set(updatedStock)
                    .addOnSuccessListener {
                        Log.d("StockViewModel", "Stock created successfully in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e("StockViewModel", "Error creating stock in Firestore: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e("StockViewModel", "Error updating stock: ${e.message}")
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun insert(stock: Stock) = viewModelScope.launch {
        try {
            val newStock = stock.copy(firebaseId = stocksCollection.document().id)
            val id = stockDao.insert(newStock)
            
            // Create stock history entry for new stock
            val stockHistory = StockHistory(
                stockId = id,
                date = Date(),
                oldQuantity = 0,
                newQuantity = stock.quantity,
                oldPrice = 0.0,
                newPrice = stock.price,
                action = "CREATE",
                description = "Initial stock creation"
            )
            
            // Add history to local database
            stockHistoryDao.insert(stockHistory)
            
            // Add history to Firestore
            stockHistoryCollection.document(stockHistory.id)
                .set(stockHistory)
                .addOnSuccessListener {
                    Log.d("StockViewModel", "Stock history added to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("StockViewModel", "Error adding stock history to Firestore: ${e.message}")
                }
            
            // Update Firestore
            stocksCollection.document(newStock.firebaseId)
                .set(newStock)
                .addOnFailureListener { e ->
                    Log.e("StockViewModel", "Error adding stock to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("StockViewModel", "Error inserting stock: ${e.message}")
        }
    }

    fun delete(stock: Stock) = viewModelScope.launch {
        try {
            // Create stock history entry for deletion
            val stockHistory = StockHistory(
                stockId = stock.id,
                date = Date(),
                oldQuantity = stock.quantity,
                newQuantity = 0,
                oldPrice = stock.price,
                newPrice = 0.0,
                action = "DELETE",
                description = "Stock deleted"
            )
            
            // Add history to local database
            stockHistoryDao.insert(stockHistory)
            
            // Add history to Firestore
            stockHistoryCollection.document(stockHistory.id)
                .set(stockHistory)
                .addOnSuccessListener {
                    Log.d("StockViewModel", "Stock history added to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("StockViewModel", "Error adding stock history to Firestore: ${e.message}")
                }
            
            // Delete from Room database
            stockDao.delete(stock)
            
            // Delete from Firestore
            stock.firebaseId?.let { id ->
                stocksCollection.document(id)
                    .delete()
                    .addOnFailureListener { e ->
                        Log.e("StockViewModel", "Error deleting stock from Firestore: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e("StockViewModel", "Error deleting stock: ${e.message}")
        }
    }

    suspend fun addStockHistory(stockHistory: StockHistory) {
        stockHistoryDao.insert(stockHistory)
    }

    fun updateFirestore(stock: Stock, stockHistory: StockHistory) {
        // Update stock in Firestore
        stocksCollection.document(stock.firebaseId)
            .set(stock)
            .addOnSuccessListener {
                // Stock updated successfully
            }
            .addOnFailureListener { e ->
                Log.e("StockViewModel", "Error updating stock in Firestore: ${e.message}")
            }

        // Add stock history to Firestore
        stockHistoryCollection.document(stockHistory.id)
            .set(stockHistory)
            .addOnSuccessListener {
                // Stock history added successfully
            }
            .addOnFailureListener { e ->
                Log.e("StockViewModel", "Error adding stock history to Firestore: ${e.message}")
            }
    }

    // Get stock history for a specific stock
    fun getStockHistory(stockId: Long) = stockHistoryDao.getHistoryForStock(stockId)

    // Get all free items given
    fun getFreeItemsHistory() = stockHistoryDao.getFreeItemsHistory()

    enum class SortOrder {
        NAME_ASC, NAME_DESC, QUANTITY_ASC, QUANTITY_DESC, PRICE_ASC, PRICE_DESC
    }
}