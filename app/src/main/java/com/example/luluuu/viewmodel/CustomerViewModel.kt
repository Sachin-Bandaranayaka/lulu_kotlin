package com.example.luluuu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.luluuu.db.AppDatabase
import com.example.luluuu.model.Customer
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class CustomerViewModel(application: Application) : AndroidViewModel(application) {
    private val customerDao = AppDatabase.getDatabase(application).customerDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val customersCollection = firestore.collection("customers")

    private val _searchResults = MutableStateFlow<List<Customer>>(emptyList())
    val searchResults: StateFlow<List<Customer>> = _searchResults

    fun getAllCustomers() = customerDao.getAllCustomers()

    suspend fun getCustomerByName(name: String) = customerDao.getCustomerByName(name)

    fun searchCustomers(query: String) {
        viewModelScope.launch {
            customerDao.searchCustomers(query).collectLatest {
                _searchResults.value = it
            }
        }
    }

    fun addCustomer(name: String, phoneNumber: String, address: String) {
        viewModelScope.launch {
            val customer = Customer(
                id = UUID.randomUUID().toString(),
                name = name,
                phoneNumber = phoneNumber,
                address = address
            )

            // Add to Room database
            customerDao.insert(customer)

            // Add to Firebase
            customersCollection.document(customer.id)
                .set(customer)
                .addOnSuccessListener {
                    // Customer added successfully
                }
                .addOnFailureListener { e ->
                    // Handle the error
                }
        }
    }

    fun syncWithFirebase() {
        customersCollection.get()
            .addOnSuccessListener { documents ->
                viewModelScope.launch {
                    for (document in documents) {
                        val customer = document.toObject(Customer::class.java)
                        customerDao.insert(customer)
                    }
                }
            }
    }
}
