package com.example.luluuu.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.luluuu.model.Stock
import com.example.luluuu.model.StockHistory

@Database(
    entities = [
        Stock::class,
        StockHistory::class
    ],
    version = 6
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun stockHistoryDao(): StockHistoryDao

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
                .addMigrations(
                    MIGRATION_4_5,
                    MIGRATION_5_6
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE stocks ADD COLUMN firebaseId TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE stocks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        firebaseId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        price REAL NOT NULL,
                        quantity INTEGER NOT NULL,
                        description TEXT NOT NULL
                    )
                """)

                database.execSQL("""
                    INSERT INTO stocks_new (id, firebaseId, name, price, quantity, description)
                    SELECT id, '' as firebaseId, name, price, quantity, description 
                    FROM stocks
                """)

                database.execSQL("DROP TABLE stocks")

                database.execSQL("ALTER TABLE stocks_new RENAME TO stocks")
            }
        }
    }
} 