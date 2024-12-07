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

    init {
        // Initialize real-time listener
        setupFirestoreListener()
    }

    private fun setupFirestoreListener() {
        viewModelScope.launch {
            try {
                customersCollection.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("CustomerViewModel", "Error listening to customers", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        viewModelScope.launch {
                            try {
                                // Get all current Firestore customer IDs
                                val firestoreIds = snapshot.documents.mapNotNull { it.id }.toSet()
                                
                                // Get all local customer IDs
                                val localCustomers = customerDao.getAllCustomersSync()
                                val localIds = localCustomers.map { it.id }.toSet()
                                
                                // Delete customers that exist locally but not in Firestore
                                val idsToDelete = localIds - firestoreIds
                                idsToDelete.forEach { id ->
                                    customerDao.deleteById(id)
                                    Log.d("CustomerViewModel", "Deleted local customer with id: $id")
                                }

                                // Update or insert customers from Firestore
                                val customers = snapshot.documents.mapNotNull { doc ->
                                    try {
                                        doc.toObject(Customer::class.java)?.apply {
                                            id = doc.id
                                        }
                                    } catch (e: Exception) {
                                        Log.e("CustomerViewModel", "Error parsing customer doc", e)
                                        null
                                    }
                                }
                                
                                // If Firestore is empty, clear local database
                                if (customers.isEmpty() && snapshot.documents.isEmpty()) {
                                    customerDao.deleteAll()
                                    Log.d("CustomerViewModel", "Cleared all local customers as Firestore is empty")
                                } else {
                                    // Update Room database
                                    customers.forEach { customer ->
                                        customerDao.insert(customer)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("CustomerViewModel", "Error processing customers snapshot", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Error setting up Firestore listener", e)
            }
        }
    }

    fun getAllCustomers() = customerDao.getAllCustomers()

    suspend fun getCustomerByName(name: String) = customerDao.getCustomerByName(name)

    fun searchCustomers(query: String) {
        viewModelScope.launch {
            try {
                // Search both local and Firestore
                val localResults = customerDao.searchCustomers(query)
                
                // Combine with Firestore search
                customersCollection
                    .whereGreaterThanOrEqualTo("name", query)
                    .whereLessThanOrEqualTo("name", query + "\uf8ff")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        viewModelScope.launch {
                            try {
                                val firebaseResults = snapshot.documents.mapNotNull { doc ->
                                    doc.toObject(Customer::class.java)?.apply {
                                        id = doc.id
                                    }
                                }
                                 
                                // Combine and deduplicate results
                                localResults.collectLatest { localCustomers ->
                                    val combined = (localCustomers + firebaseResults)
                                        .distinctBy { it.id }
                                        .sortedBy { it.name }
                                    _searchResults.value = combined
                                }
                            } catch (e: Exception) {
                                Log.e("CustomerViewModel", "Error processing search results", e)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CustomerViewModel", "Error searching Firestore", e)
                        // Fall back to local results only
                        viewModelScope.launch {
                            localResults.collectLatest {
                                _searchResults.value = it
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Error in searchCustomers", e)
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
