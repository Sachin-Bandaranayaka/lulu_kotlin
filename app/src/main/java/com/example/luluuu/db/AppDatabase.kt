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
    version = 2
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
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add firebaseId column to expenses table
                database.execSQL(
                    "ALTER TABLE expenses ADD COLUMN firebaseId TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}