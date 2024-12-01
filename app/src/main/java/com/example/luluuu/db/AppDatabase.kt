package com.example.luluuu.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.luluuu.model.Expense
import com.example.luluuu.model.Invoice
import com.example.luluuu.model.Stock
import com.example.luluuu.model.StockHistory

@Database(
    entities = [Stock::class, StockHistory::class, Expense::class, Invoice::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun stockHistoryDao(): StockHistoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration() // Force a clean rebuild of the database
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to stocks table
                db.execSQL(
                    """
                    ALTER TABLE stocks 
                    ADD COLUMN price REAL NOT NULL DEFAULT 0.0
                    """
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to invoices table
                db.execSQL(
                    """
                    ALTER TABLE invoices 
                    ADD COLUMN invoiceNumber TEXT NOT NULL DEFAULT ''
                    """
                )
                db.execSQL(
                    """
                    ALTER TABLE invoices 
                    ADD COLUMN customerName TEXT NOT NULL DEFAULT ''
                    """
                )
                db.execSQL(
                    """
                    ALTER TABLE invoices 
                    ADD COLUMN customerMobile TEXT NOT NULL DEFAULT ''
                    """
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add firebase_id column to invoices table
                db.execSQL(
                    """
                    ALTER TABLE invoices 
                    ADD COLUMN firebase_id TEXT NOT NULL DEFAULT ''
                    """
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No need to add firebaseId column to stocks table as it's already defined in the entity
                // This migration can be used for other changes in the future
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new migration for version 6
            }
        }
    }
}