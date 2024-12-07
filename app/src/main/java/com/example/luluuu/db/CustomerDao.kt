package com.example.luluuu.db

import androidx.room.*
import com.example.luluuu.model.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): Customer?

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE name = :name LIMIT 1")
    suspend fun getCustomerByName(name: String): Customer?

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersSync(): List<Customer>

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer)

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)
}
