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

    @Insert
    suspend fun insert(invoice: Invoice): Long

    @Update
    suspend fun update(invoice: Invoice)

    @Delete
    suspend fun delete(invoice: Invoice)
}