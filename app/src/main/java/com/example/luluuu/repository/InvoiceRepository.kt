package com.example.luluuu.repository

import com.example.luluuu.db.InvoiceDao
import com.example.luluuu.model.Invoice
import com.example.luluuu.model.Customer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val firebaseRepository: InvoiceFirebaseRepository
) {
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()

    suspend fun insert(invoice: Invoice) {
        val localId = invoiceDao.insert(invoice)
        firebaseRepository.insert(invoice.copy(id = localId))
    }

    suspend fun update(invoice: Invoice) {
        invoiceDao.update(invoice)
        firebaseRepository.update(invoice)
    }

    suspend fun delete(invoice: Invoice) {
        invoiceDao.delete(invoice)
        firebaseRepository.delete(invoice)
    }

    suspend fun getInvoiceById(id: Long): Invoice? {
        return invoiceDao.getInvoiceById(id)
    }

    suspend fun getLastInvoiceNumber(): Int {
        val lastInvoice = invoiceDao.getLastInvoice()
        return lastInvoice?.invoiceNumber ?: 0
    }

    fun getInvoicesForCustomer(customerId: String): Flow<List<Invoice>> {
        return invoiceDao.getInvoicesForCustomer(customerId)
    }

    suspend fun getAllInvoicesList(): List<Invoice> {
        return allInvoices.first()
    }

    suspend fun updateCustomerInfo(invoiceId: Long, customer: Customer) {
        invoiceDao.updateCustomerInfo(
            invoiceId = invoiceId,
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phoneNumber
        )
    }
}