package com.example.itemremindertool.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.itemremindertool.data.converters.DateConverters
import com.example.itemremindertool.data.converters.StringListConverters
import com.example.itemremindertool.data.converters.ReminderTypeConverters
import com.example.itemremindertool.data.dao.CategoryDao
import com.example.itemremindertool.data.dao.ItemDao
import com.example.itemremindertool.data.dao.ShoppingItemDao
import com.example.itemremindertool.data.dao.WarehouseDao
import com.example.itemremindertool.data.dao.ItemReminderDao
import com.example.itemremindertool.data.model.Category
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.data.model.ItemReminder

@Database(
    entities = [Item::class, Category::class, ShoppingItem::class, Warehouse::class, ItemReminder::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(DateConverters::class, StringListConverters::class, ReminderTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun itemReminderDao(): ItemReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "item_reminder_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE items ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE TABLE items_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, categoryId INTEGER, warehouseId INTEGER, tags TEXT NOT NULL DEFAULT '', purchaseDate INTEGER, expiryDate INTEGER, price REAL, quantity INTEGER NOT NULL DEFAULT 1, barcode TEXT, imageUri TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                database.execSQL("INSERT INTO items_new (id, name, description, categoryId, warehouseId, tags, purchaseDate, expiryDate, price, quantity, barcode, imageUri, createdAt, updatedAt) SELECT id, name, description, categoryId, warehouseId, '', purchaseDate, expiryDate, price, quantity, barcode, imageUri, createdAt, updatedAt FROM items")
                database.execSQL("DROP TABLE items")
                database.execSQL("ALTER TABLE items_new RENAME TO items")
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE items ADD COLUMN featureCode TEXT")
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE shopping_items ADD COLUMN imageUri TEXT")
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE warehouses ADD COLUMN parentId INTEGER")
                database.execSQL("ALTER TABLE warehouses ADD COLUMN level INTEGER NOT NULL DEFAULT 1")
            }
        }
        
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE items ADD COLUMN enableStockAlert INTEGER NOT NULL DEFAULT 1")
            }
        }
        
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS item_reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        reminderType TEXT NOT NULL,
                        reminderTime INTEGER,
                        dailyTime TEXT,
                        monthlyDay INTEGER,
                        monthlyTime TEXT,
                        yearlyMonth INTEGER,
                        yearlyDay INTEGER,
                        yearlyTime TEXT,
                        reason TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
            }
        }
        
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为 shopping_items 表添加 itemId 字段
                database.execSQL("ALTER TABLE shopping_items ADD COLUMN itemId INTEGER")
            }
        }
    }
}
