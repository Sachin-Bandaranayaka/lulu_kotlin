package com.example.luluuu.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.luluuu.db.AppDatabase
import com.example.luluuu.model.Customer
import com.google.firebase.auth.FirebaseAuth
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
    private val auth = FirebaseAuth.getInstance()

    private val _searchResults = MutableStateFlow<List<Customer>>(emptyList())
    val searchResults: StateFlow<List<Customer>> = _searchResults

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus

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
            try {
                // Check if user is authenticated
                if (auth.currentUser == null) {
                    Log.e("CustomerViewModel", "User not authenticated")
                    return@launch
                }

                val customer = Customer(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    phoneNumber = phoneNumber,
                    address = address
                )

                Log.d("CustomerViewModel", "Adding customer: $customer")

                // Add to Room database
                customerDao.insert(customer)
                Log.d("CustomerViewModel", "Customer added to local database")

                // Add to Firebase
                customersCollection.document(customer.id)
                    .set(customer)
                    .addOnSuccessListener {
                        Log.d("CustomerViewModel", "Customer added to Firestore successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("CustomerViewModel", "Failed to add customer to Firestore", e)
                    }
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Error in addCustomer", e)
            }
        }
    }

    fun syncWithFirebase() {
        viewModelScope.launch {
            try {
                Log.d("CustomerViewModel", "Starting Firebase sync")
                _syncStatus.value = "Syncing..."

                customersCollection.get()
                    .addOnSuccessListener { documents ->
                        viewModelScope.launch {
                            try {
                                Log.d("CustomerViewModel", "Retrieved ${documents.size()} customers from Firestore")
                                var syncedCount = 0
                                
                                for (document in documents) {
                                    try {
                                        val customer = document.toObject(Customer::class.java)
                                        customerDao.insert(customer)
                                        syncedCount++
                                        Log.d("CustomerViewModel", "Synced customer: ${customer.name}")
                                    } catch (e: Exception) {
                                        Log.e("CustomerViewModel", "Error syncing customer ${document.id}", e)
                                    }
                                }
                                
                                _syncStatus.value = "Synced $syncedCount customers"
                                Log.d("CustomerViewModel", "Sync completed. Synced $syncedCount customers")
                            } catch (e: Exception) {
                                Log.e("CustomerViewModel", "Error during sync", e)
                                _syncStatus.value = "Sync failed: ${e.message}"
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CustomerViewModel", "Failed to get customers from Firestore", e)
                        _syncStatus.value = "Sync failed: ${e.message}"
                    }
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Error initiating sync", e)
                _syncStatus.value = "Sync failed: ${e.message}"
            }
        }
    }
}
