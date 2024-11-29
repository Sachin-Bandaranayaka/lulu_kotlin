package com.example.luluuu.repository

import com.example.luluuu.db.InvoiceDao
import com.example.luluuu.model.Invoice
import kotlinx.coroutines.flow.Flow

class InvoiceRepository(private val invoiceDao: InvoiceDao) {
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()

    suspend fun insert(invoice: Invoice) {
        invoiceDao.insert(invoice)
    }

    suspend fun delete(invoice: Invoice) {
        invoiceDao.delete(invoice)
    }

    suspend fun getInvoiceById(id: Long): Invoice? {
        return invoiceDao.getInvoiceById(id)
    }
} 