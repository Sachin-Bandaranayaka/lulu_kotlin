package com.example.luluuu.repository

import android.util.Log
import com.example.luluuu.db.CustomerDao
import com.example.luluuu.model.Customer
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CustomerRepository(private val customerDao: CustomerDao) {
    private val TAG = "CustomerRepository"
    private val db = Firebase.firestore
    private val customersCollection = db.collection("customers")

    suspend fun addCustomer(customer: Customer) {
        try {
            // Generate a new ID if not present
            if (customer.id.isEmpty()) {
                customer.id = UUID.randomUUID().toString()
            }

            // Add to local database
            customerDao.insert(customer)

            // Add to Firebase
            val customerData = hashMapOf(
                "id" to customer.id,
                "name" to customer.name,
                "phoneNumber" to customer.phoneNumber,
                "address" to customer.address
            )

            customersCollection.document(customer.id)
                .set(customerData)
                .await()

            Log.d(TAG, "Customer added successfully: ${customer.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding customer", e)
            throw e
        }
    }

    suspend fun updateCustomer(customer: Customer) {
        try {
            // Update local database
            customerDao.update(customer)

            // Update Firebase
            val customerData = hashMapOf(
                "id" to customer.id,
                "name" to customer.name,
                "phoneNumber" to customer.phoneNumber,
                "address" to customer.address
            )

            customersCollection.document(customer.id)
                .set(customerData)
                .await()

            Log.d(TAG, "Customer updated successfully: ${customer.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating customer", e)
            throw e
        }
    }

    fun searchCustomers(query: String): Flow<List<Customer>> {
        return customerDao.searchCustomers(query)
            .catch { e ->
                Log.e(TAG, "Error searching customers", e)
                emit(emptyList())
            }
    }

    suspend fun searchCustomersFromFirebase(query: String): List<Customer> {
        return try {
            val nameResults = customersCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .get()
                .await()
                .toObjects(Customer::class.java)

            val phoneResults = customersCollection
                .whereGreaterThanOrEqualTo("phoneNumber", query)
                .whereLessThanOrEqualTo("phoneNumber", query + "\uf8ff")
                .get()
                .await()
                .toObjects(Customer::class.java)

            // Combine and deduplicate results
            (nameResults + phoneResults).distinctBy { it.id }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching customers in Firebase", e)
            emptyList()
        }
    }

    suspend fun getCustomerById(id: String): Customer? {
        return try {
            customerDao.getCustomerById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting customer by ID", e)
            null
        }
    }
}
