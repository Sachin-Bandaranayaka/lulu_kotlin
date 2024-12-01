package com.example.luluuu.repository

import com.example.luluuu.db.InvoiceDao
import com.example.luluuu.model.Invoice
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val firebaseRepository: InvoiceFirebaseRepository
) {
    val allInvoices: Flow<List<Invoice>> = firebaseRepository.getAllInvoices()

    suspend fun insert(invoice: Invoice) {
        // Save to local database
        val localId = invoiceDao.insert(invoice)
        
        // Save to Firebase
        firebaseRepository.insert(invoice.copy(id = localId))
    }

    suspend fun update(invoice: Invoice) {
        // Update local database
        invoiceDao.update(invoice)
        
        // Update Firebase
        firebaseRepository.update(invoice)
    }

    suspend fun delete(invoice: Invoice) {
        // Delete from local database
        invoiceDao.delete(invoice)
        
        // Delete from Firebase
        firebaseRepository.delete(invoice)
    }

    suspend fun getInvoiceById(id: Long): Invoice? {
        return invoiceDao.getInvoiceById(id)
    }

    fun getLastPrintedInvoice(): Invoice? {
        return firebaseRepository.getLastPrintedInvoice()
    }

    suspend fun getLastInvoice(): Invoice? {
        return firebaseRepository.getLastInvoice()
    }
}