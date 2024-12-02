package com.example.luluuu.repository

import com.example.luluuu.model.Invoice
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceFirebaseRepository @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val invoicesCollection = db.collection("invoices")

    suspend fun insert(invoice: Invoice) {
        try {
            invoicesCollection.document(invoice.id.toString()).set(invoice).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun update(invoice: Invoice) {
        try {
            invoicesCollection.document(invoice.id.toString()).set(invoice).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun delete(invoice: Invoice) {
        try {
            invoicesCollection.document(invoice.id.toString()).delete().await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getInvoiceById(id: Long): Invoice? {
        return try {
            val document = invoicesCollection.document(id.toString()).get().await()
            document.toObject(Invoice::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
