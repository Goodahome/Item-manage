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
import com.example.itemremindertool.data.dao.DeletedRecordDao
import com.example.itemremindertool.data.model.Category
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.data.model.DeletedRecord

@Database(
    entities = [Item::class, Category::class, ShoppingItem::class, Warehouse::class, ItemReminder::class, DeletedRecord::class],
    version = 9,
    exportSchema = false
)
@TypeConverters(DateConverters::class, StringListConverters::class, ReminderTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun itemReminderDao(): ItemReminderDao
    abstract fun deletedRecordDao(): DeletedRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // 先检查现有实例是否有效
            val currentInstance = INSTANCE
            if (currentInstance != null) {
                // 尝试验证连接是否有效
                try {
                    // 通过尝试打开数据库来验证连接
                    currentInstance.openHelper.readableDatabase
                    return currentInstance
                } catch (e: Exception) {
                    // 连接已关闭，需要重新创建
                    android.util.Log.w("AppDatabase", "数据库连接已关闭，重新创建实例", e)
                    synchronized(this) {
                        // 双重检查
                        if (INSTANCE === currentInstance) {
                            try {
                                INSTANCE?.close()
                            } catch (e: Exception) {
                                // 忽略关闭时的异常
                            }
                            INSTANCE = null
                        }
                    }
                }
            }
            
            return synchronized(this) {
                // 再次检查，避免重复创建
                val existingInstance = INSTANCE
                if (existingInstance != null) {
                    try {
                        existingInstance.openHelper.readableDatabase
                        return@synchronized existingInstance
                    } catch (e: Exception) {
                        // 连接无效，需要重新创建
                        try {
                            existingInstance.close()
                        } catch (e: Exception) {
                            // 忽略关闭时的异常
                        }
                        INSTANCE = null
                    }
                }
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "item_reminder_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // 确保 Room 内部的 invalidation 表存在，避免 "no such table: room_table_modification_log"
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS room_table_modification_log (
                                    table_id INTEGER PRIMARY KEY,
                                    invalidated INTEGER NOT NULL DEFAULT 0
                                )
                                """.trimIndent()
                            )
                        }
                    })
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 重置数据库实例（用于数据库恢复后重新初始化）
         */
        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
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
        
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建删除记录表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS deleted_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId INTEGER NOT NULL,
                        deletedAt INTEGER NOT NULL,
                        UNIQUE(entityType, entityId)
                    )
                """)
            }
        }
    }
}
