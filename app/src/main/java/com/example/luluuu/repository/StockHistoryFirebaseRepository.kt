package com.example.luluuu.repository

import android.util.Log
import com.example.luluuu.model.StockHistory
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class StockHistoryFirebaseRepository {
    private val db = Firebase.firestore
    private val stockHistoryCollection = db.collection("stock_history")
    private val TAG = "StockHistoryFirebase"

    private fun handleFirebaseError(e: Exception, operation: String) {
        when {
            e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException -> {
                Log.w(TAG, "Network connectivity issue during $operation - continuing in offline mode", e)
            }
            else -> Log.e(TAG, "Error during $operation", e)
        }
    }

    suspend fun addHistory(stockHistory: StockHistory) {
        try {
            Log.d(TAG, "Adding stock history: stockId=${stockHistory.stockId}, date=${stockHistory.date}, action=${stockHistory.action}")
            
            // First check if this history entry already exists
            val querySnapshot = stockHistoryCollection
                .whereEqualTo("stockId", stockHistory.stockId)
                .whereEqualTo("action", stockHistory.action)
                .whereEqualTo("oldQuantity", stockHistory.oldQuantity)
                .whereEqualTo("newQuantity", stockHistory.newQuantity)
                .whereEqualTo("oldPrice", stockHistory.oldPrice)
                .whereEqualTo("newPrice", stockHistory.newPrice)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                Log.d(TAG, "History entry already exists, skipping")
                return
            }
            
            val historyData = hashMapOf(
                "id" to stockHistory.id,
                "stockId" to stockHistory.stockId,
                "date" to com.google.firebase.Timestamp(stockHistory.date),
                "oldQuantity" to stockHistory.oldQuantity,
                "newQuantity" to stockHistory.newQuantity,
                "oldPrice" to stockHistory.oldPrice,
                "newPrice" to stockHistory.newPrice,
                "action" to stockHistory.action
            )
            
            stockHistoryCollection.document(stockHistory.id).set(historyData).await()
            Log.d(TAG, "Stock history added successfully with ID: ${stockHistory.id}")
        } catch (e: Exception) {
            handleFirebaseError(e, "addHistory")
            // Don't throw the exception - allow offline operation
        }
    }

    fun getHistoryForStock(stockId: Long): Flow<List<StockHistory>> = callbackFlow {
        Log.d(TAG, "Getting history for stock ID: $stockId")
        val registration = stockHistoryCollection
            .whereEqualTo("stockId", stockId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("cancelled") == true) {
                        Log.d(TAG, "History listener cancelled normally")
                    } else {
                        handleFirebaseError(error, "getHistoryForStock")
                    }
                    trySend(emptyList()) // Return empty list on error to allow offline operation
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.d(TAG, "No snapshot available for stock ID: $stockId")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                Log.d(TAG, "Received ${snapshot.documents.size} history entries for stock ID: $stockId")
                val historyList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val timestamp = doc.getTimestamp("date")
                        if (timestamp == null) {
                            Log.w(TAG, "Missing timestamp for document ${doc.id}")
                            return@mapNotNull null
                        }

                        val stockId = doc.get("stockId")
                        if (stockId == null) {
                            Log.w(TAG, "Missing stockId for document ${doc.id}")
                            return@mapNotNull null
                        }

                        StockHistory(
                            id = doc.getString("id") ?: UUID.randomUUID().toString(),
                            stockId = (stockId as Number).toLong(),
                            date = timestamp.toDate(),
                            oldQuantity = (doc.get("oldQuantity") as Number).toInt(),
                            newQuantity = (doc.get("newQuantity") as Number).toInt(),
                            oldPrice = (doc.get("oldPrice") as Number).toDouble(),
                            newPrice = (doc.get("newPrice") as Number).toDouble(),
                            action = doc.getString("action") ?: ""
                        ).also { history ->
                            Log.d(TAG, "Parsed history entry: stockId=${history.stockId}, date=${history.date}, action=${history.action}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing stock history document ${doc.id}", e)
                        null
                    }
                }
                Log.d(TAG, "Sending ${historyList.size} history entries to UI")
                trySend(historyList)
            }

        awaitClose { 
            Log.d(TAG, "Closing history listener for stock ID: $stockId")
            registration.remove() 
        }
    }

    fun getHistoryByDateRange(startDate: Date, endDate: Date): Flow<List<StockHistory>> = callbackFlow {
        val registration = stockHistoryCollection
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.message?.contains("cancelled") == true) {
                        Log.d(TAG, "History listener cancelled normally")
                    } else {
                        handleFirebaseError(error, "getHistoryByDateRange")
                    }
                    trySend(emptyList()) // Return empty list on error to allow offline operation
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val historyList = snapshot.documents.mapNotNull { doc ->
                    try {
                        StockHistory(
                            id = doc.getString("id") ?: UUID.randomUUID().toString(),
                            stockId = (doc.get("stockId") as Number).toLong(),
                            date = doc.getTimestamp("date")?.toDate() ?: Date(),
                            oldQuantity = (doc.get("oldQuantity") as Number).toInt(),
                            newQuantity = (doc.get("newQuantity") as Number).toInt(),
                            oldPrice = (doc.get("oldPrice") as Number).toDouble(),
                            newPrice = (doc.get("newPrice") as Number).toDouble(),
                            action = doc.getString("action") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing stock history document", e)
                        null
                    }
                }
                trySend(historyList)
            }

        awaitClose { registration.remove() }
    }
}
