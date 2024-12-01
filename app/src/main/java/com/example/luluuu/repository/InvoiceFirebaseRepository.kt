package com.example.luluuu.repository

import android.util.Log
import com.example.luluuu.model.Invoice
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceFirebaseRepository @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val invoicesCollection = db.collection("invoices")
    private var lastPrintedInvoice: Invoice? = null

    fun getAllInvoices(): Flow<List<Invoice>> = callbackFlow {
        val subscription = invoicesCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val invoices = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Invoice::class.java)?.copy(
                                firebaseId = doc.id
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deserializing invoice: ${e.message}")
                            null
                        }
                    }
                    trySend(invoices)
                }
            }

        awaitClose { subscription.remove() }
    }

    suspend fun insert(invoice: Invoice) {
        try {
            // Create a map of the invoice data without the firebaseId
            val invoiceData = mapOf(
                "id" to invoice.id,
                "invoiceNumber" to invoice.invoiceNumber,
                "customerName" to invoice.customerName,
                "customerMobile" to invoice.customerMobile,
                "date" to invoice.date,
                "items" to invoice.items,
                "total" to invoice.total
            )
            
            val docRef = invoicesCollection.add(invoiceData).await()
            lastPrintedInvoice = invoice.copy(firebaseId = docRef.id)
            Log.d(TAG, "Invoice added successfully with ID: ${docRef.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting invoice: ${e.message}")
            throw e
        }
    }

    suspend fun update(invoice: Invoice) {
        try {
            if (invoice.firebaseId.isEmpty()) {
                Log.w(TAG, "Cannot update invoice: No Firebase ID")
                return
            }

            // Create a map of the invoice data without the firebaseId
            val invoiceData = mapOf(
                "id" to invoice.id,
                "invoiceNumber" to invoice.invoiceNumber,
                "customerName" to invoice.customerName,
                "customerMobile" to invoice.customerMobile,
                "date" to invoice.date,
                "items" to invoice.items,
                "total" to invoice.total
            )

            invoicesCollection.document(invoice.firebaseId)
                .set(invoiceData)
                .await()

            if (lastPrintedInvoice?.firebaseId == invoice.firebaseId) {
                lastPrintedInvoice = invoice
            }
            Log.d(TAG, "Invoice updated successfully with ID: ${invoice.firebaseId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating invoice: ${e.message}")
            throw e
        }
    }

    suspend fun delete(invoice: Invoice) {
        try {
            if (invoice.firebaseId.isEmpty()) {
                Log.w(TAG, "Cannot delete invoice: No Firebase ID")
                return
            }

            invoicesCollection.document(invoice.firebaseId)
                .delete()
                .await()

            if (lastPrintedInvoice?.firebaseId == invoice.firebaseId) {
                lastPrintedInvoice = null
            }
            Log.d(TAG, "Invoice deleted successfully with ID: ${invoice.firebaseId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting invoice: ${e.message}")
            throw e
        }
    }

    fun getLastPrintedInvoice(): Invoice? = lastPrintedInvoice

    suspend fun getLastInvoice(): Invoice? {
        return invoicesCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.let { doc ->
                try {
                    doc.toObject(Invoice::class.java)?.copy(
                        firebaseId = doc.id
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing invoice: ${e.message}")
                    null
                }
            }
    }

    companion object {
        private const val TAG = "InvoiceFirebaseRepo"
    }
}
