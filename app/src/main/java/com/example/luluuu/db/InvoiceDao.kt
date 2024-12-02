package com.example.luluuu.db

import androidx.room.*
import com.example.luluuu.model.Invoice
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): Invoice?

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY date DESC")
    fun getInvoicesForCustomer(customerId: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices ORDER BY date DESC LIMIT 1")
    suspend fun getLastInvoice(): Invoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: Invoice): Long

    @Update
    suspend fun update(invoice: Invoice)

    @Delete
    suspend fun delete(invoice: Invoice)

    @Query("UPDATE invoices SET customerId = :customerId, customerName = :customerName, customerPhone = :customerPhone WHERE id = :invoiceId")
    suspend fun updateCustomerInfo(invoiceId: Long, customerId: String, customerName: String, customerPhone: String)
}