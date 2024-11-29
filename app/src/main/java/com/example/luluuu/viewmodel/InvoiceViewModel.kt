package com.example.luluuu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.luluuu.db.AppDatabase
import com.example.luluuu.model.Invoice
import com.example.luluuu.repository.InvoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: InvoiceRepository
    val allInvoices: Flow<List<Invoice>>

    init {
        val invoiceDao = AppDatabase.getDatabase(application).invoiceDao()
        repository = InvoiceRepository(invoiceDao)
        allInvoices = repository.allInvoices
    }

    fun insert(invoice: Invoice) = viewModelScope.launch {
        repository.insert(invoice)
    }

    fun delete(invoice: Invoice) = viewModelScope.launch {
        repository.delete(invoice)
    }
} 