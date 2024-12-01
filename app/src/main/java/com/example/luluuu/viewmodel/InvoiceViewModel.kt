package com.example.luluuu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.luluuu.db.AppDatabase
import com.example.luluuu.model.Invoice
import com.example.luluuu.repository.InvoiceRepository
import com.example.luluuu.repository.InvoiceFirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: InvoiceRepository
    val allInvoices: Flow<List<Invoice>>

    init {
        val invoiceDao = AppDatabase.getDatabase(application).invoiceDao()
        val firebaseRepository = InvoiceFirebaseRepository()
        repository = InvoiceRepository(invoiceDao, firebaseRepository)
        allInvoices = repository.allInvoices
    }

    fun insert(invoice: Invoice) = viewModelScope.launch {
        repository.insert(invoice)
    }

    fun update(invoice: Invoice) = viewModelScope.launch {
        repository.update(invoice)
    }

    fun delete(invoice: Invoice) = viewModelScope.launch {
        repository.delete(invoice)
    }

    fun getLastPrintedInvoice(): Invoice? {
        return repository.getLastPrintedInvoice()
    }

    suspend fun getLastInvoice(): Invoice? {
        return repository.getLastInvoice()
    }
}