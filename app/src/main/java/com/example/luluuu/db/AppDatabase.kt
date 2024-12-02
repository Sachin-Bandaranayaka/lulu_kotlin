package com.example.luluuu.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.luluuu.model.Stock
import com.example.luluuu.model.StockHistory
import com.example.luluuu.model.Invoice
import com.example.luluuu.model.Customer
import com.example.luluuu.model.Expense

@Database(
    entities = [Stock::class, StockHistory::class, Expense::class, Invoice::class, Customer::class],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun stockHistoryDao(): StockHistoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add customerId column to invoices table
                database.execSQL(
                    "ALTER TABLE invoices ADD COLUMN customerId TEXT"
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop and recreate the table with the correct schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS invoices_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        invoiceNumber INTEGER NOT NULL,
                        customerId TEXT,
                        customerName TEXT NOT NULL DEFAULT '',
                        customerPhone TEXT NOT NULL DEFAULT '',
                        date INTEGER NOT NULL,
                        items TEXT NOT NULL DEFAULT '[]',
                        total REAL NOT NULL DEFAULT 0,
                        returnAmount REAL NOT NULL DEFAULT 0,
                        returnDescription TEXT NOT NULL DEFAULT ''
                    )
                """)

                // Copy data from old table to new table
                database.execSQL("""
                    INSERT INTO invoices_new (
                        id, invoiceNumber, customerId, customerName, customerPhone,
                        date, items, total, returnAmount, returnDescription
                    )
                    SELECT 
                        id, invoiceNumber, customerId, customerName, customerPhone,
                        COALESCE(date, 0), COALESCE(items, '[]'), 
                        COALESCE(total, 0), COALESCE(returnAmount, 0), 
                        COALESCE(returnDescription, '')
                    FROM invoices
                """)

                // Drop old table
                database.execSQL("DROP TABLE invoices")

                // Rename new table to original name
                database.execSQL("ALTER TABLE invoices_new RENAME TO invoices")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create temporary table with new schema
                database.execSQL("""
                    CREATE TABLE invoices_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        invoiceNumber INTEGER NOT NULL DEFAULT 0,
                        customerId TEXT,
                        customerName TEXT NOT NULL DEFAULT '',
                        customerPhone TEXT NOT NULL DEFAULT '',
                        date INTEGER,
                        items TEXT NOT NULL DEFAULT '[]',
                        total REAL NOT NULL DEFAULT 0,
                        returnAmount REAL NOT NULL DEFAULT 0,
                        returnDescription TEXT NOT NULL DEFAULT ''
                    )
                """)

                // Copy data from old table to new table, converting invoiceNumber to INTEGER
                database.execSQL("""
                    INSERT INTO invoices_temp (
                        id, invoiceNumber, customerId, customerName, customerPhone,
                        date, items, total, returnAmount, returnDescription
                    )
                    SELECT 
                        id,
                        CAST(CASE 
                            WHEN invoiceNumber = '' THEN '0'
                            ELSE invoiceNumber
                        END AS INTEGER),
                        customerId, customerName, customerPhone,
                        date, items, total, returnAmount, returnDescription
                    FROM invoices
                """)

                // Drop old table
                database.execSQL("DROP TABLE invoices")

                // Rename new table to original name
                database.execSQL("ALTER TABLE invoices_temp RENAME TO invoices")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add discount column to invoices table
                database.execSQL("ALTER TABLE invoices ADD COLUMN discount REAL NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Step 1: Create new table with minimal required columns
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS stock_history_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        stockId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        action TEXT NOT NULL
                    )
                """)

                // Step 2: Copy basic data
                database.execSQL("""
                    INSERT INTO stock_history_new (
                        id, stockId, date, action
                    )
                    SELECT 
                        id, stockId, date, action
                    FROM stock_history
                """)

                // Step 3: Drop old table
                database.execSQL("DROP TABLE stock_history")

                // Step 4: Create final table with all columns
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS stock_history (
                        id TEXT PRIMARY KEY NOT NULL,
                        stockId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        oldQuantity INTEGER NOT NULL DEFAULT 0,
                        newQuantity INTEGER NOT NULL DEFAULT 0,
                        oldPrice REAL NOT NULL DEFAULT 0,
                        newPrice REAL NOT NULL DEFAULT 0,
                        action TEXT NOT NULL DEFAULT '',
                        invoiceNumber INTEGER,
                        regularQuantity INTEGER NOT NULL DEFAULT 0,
                        freeQuantity INTEGER NOT NULL DEFAULT 0,
                        description TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(stockId) REFERENCES stocks(id) ON DELETE CASCADE
                    )
                """)

                // Step 5: Create index on stockId
                database.execSQL("""
                    CREATE INDEX index_stock_history_stockId ON stock_history(stockId)
                """)

                // Step 6: Copy data to final table
                database.execSQL("""
                    INSERT INTO stock_history (
                        id, stockId, date, action
                    )
                    SELECT 
                        id, stockId, date, action
                    FROM stock_history_new
                """)

                // Step 7: Drop temporary table
                database.execSQL("DROP TABLE stock_history_new")

                // Step 8: Backup stocks table
                database.execSQL("""
                    CREATE TABLE stocks_backup AS SELECT * FROM stocks
                """)

                // Step 9: Drop and recreate stocks table to ensure schema consistency
                database.execSQL("DROP TABLE stocks")
                database.execSQL("""
                    CREATE TABLE stocks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        firebaseId TEXT NOT NULL DEFAULT '',
                        name TEXT NOT NULL DEFAULT '',
                        price REAL NOT NULL DEFAULT 0.0,
                        quantity INTEGER NOT NULL DEFAULT 0,
                        description TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT 'SOAP_BAR'
                    )
                """)

                // Step 10: Restore stocks data
                database.execSQL("""
                    INSERT INTO stocks (
                        id, firebaseId, name, price, quantity, description, category
                    )
                    SELECT 
                        id, firebaseId, name, price, quantity, description, category
                    FROM stocks_backup
                """)

                // Step 11: Drop backup table
                database.execSQL("DROP TABLE stocks_backup")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}