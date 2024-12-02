package com.example.luluuu.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.luluuu.db.AppDatabase
import com.example.luluuu.model.Invoice
import com.example.luluuu.model.Customer
import com.example.luluuu.repository.InvoiceRepository
import com.example.luluuu.repository.InvoiceFirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: InvoiceRepository
    val allInvoices: Flow<List<Invoice>>
    private val _invoices = MutableLiveData<List<Invoice>>()
    val invoices: LiveData<List<Invoice>> = _invoices
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var currentFilter: Triple<Customer?, Date?, Date?>? = null

    init {
        Log.d("InvoiceViewModel", "Initializing ViewModel")
        val invoiceDao = AppDatabase.getDatabase(application).invoiceDao()
        val firebaseRepository = InvoiceFirebaseRepository()
        repository = InvoiceRepository(invoiceDao, firebaseRepository)
        allInvoices = repository.allInvoices
        loadInvoices()
    }

    fun insert(invoice: Invoice) = viewModelScope.launch {
        try {
            repository.insert(invoice)
        } catch (e: Exception) {
            _error.value = e.message ?: "Error inserting invoice"
        }
    }

    fun update(invoice: Invoice) = viewModelScope.launch {
        try {
            repository.update(invoice)
        } catch (e: Exception) {
            _error.value = e.message ?: "Error updating invoice"
        }
    }

    fun delete(invoice: Invoice) = viewModelScope.launch {
        try {
            repository.delete(invoice)
        } catch (e: Exception) {
            _error.value = e.message ?: "Error deleting invoice"
        }
    }

    suspend fun getLastInvoiceNumber(): Int {
        return try {
            repository.getLastInvoiceNumber()
        } catch (e: Exception) {
            _error.value = e.message ?: "Error getting last invoice number"
            0
        }
    }

    fun linkCustomerToInvoice(invoiceId: Long, customer: Customer) {
        viewModelScope.launch {
            try {
                repository.updateCustomerInfo(invoiceId, customer)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error linking customer to invoice"
            }
        }
    }

    fun getInvoicesForCustomer(customerId: String): Flow<List<Invoice>> {
        return repository.getInvoicesForCustomer(customerId)
    }

    fun applyFilter(customer: Customer?, fromDate: Date?, toDate: Date?) {
        currentFilter = Triple(customer, fromDate, toDate)
        viewModelScope.launch {
            try {
                val allInvoices = repository.getAllInvoicesList()
                val filteredInvoices = allInvoices.filter { invoice ->
                    var matches = true
                    
                    if (customer != null) {
                        matches = matches && invoice.customerId == customer.id
                    }
                    
                    if (fromDate != null && toDate != null) {
                        matches = matches && (invoice.date in fromDate..toDate)
                    }
                    
                    matches
                }
                _invoices.value = filteredInvoices
            } catch (e: Exception) {
                _error.value = e.message ?: "Error applying filter"
            }
        }
    }

    fun clearFilter() {
        currentFilter = null
        loadInvoices()
    }

    private fun loadInvoices() {
        viewModelScope.launch {
            try {
                _invoices.value = repository.getAllInvoicesList()
            } catch (e: Exception) {
                _error.value = e.message ?: "Error loading invoices"
            }
        }
    }

    suspend fun getLastInvoice(): Invoice? {
        return try {
            repository.getAllInvoicesList().maxByOrNull { it.date }
        } catch (e: Exception) {
            _error.value = e.message ?: "Error getting last invoice"
            null
        }
    }
}