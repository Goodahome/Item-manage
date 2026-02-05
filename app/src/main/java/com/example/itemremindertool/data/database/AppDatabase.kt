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
import com.example.itemremindertool.data.converters.ActivityEventTypeConverters
import com.example.itemremindertool.data.converters.SyncOperationConverters
import com.example.itemremindertool.data.dao.CategoryDao
import com.example.itemremindertool.data.dao.ItemDao
import com.example.itemremindertool.data.dao.ShoppingItemDao
import com.example.itemremindertool.data.dao.WarehouseDao
import com.example.itemremindertool.data.dao.ItemReminderDao
import com.example.itemremindertool.data.dao.DeletedRecordDao
import com.example.itemremindertool.data.dao.SyncQueueDao
import com.example.itemremindertool.data.model.Category
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.data.model.DeletedRecord
import com.example.itemremindertool.data.model.ActivityEvent
import com.example.itemremindertool.data.model.SyncQueueItem
import com.example.itemremindertool.data.model.IconLibraryItem
import com.example.itemremindertool.data.dao.ActivityEventDao
import com.example.itemremindertool.data.dao.IconLibraryDao

@Database(
    entities = [Item::class, Category::class, ShoppingItem::class, Warehouse::class, ItemReminder::class, DeletedRecord::class, ActivityEvent::class, SyncQueueItem::class, IconLibraryItem::class],
    version = 22,
    exportSchema = true
)
@TypeConverters(DateConverters::class, StringListConverters::class, ReminderTypeConverters::class, ActivityEventTypeConverters::class, SyncOperationConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun itemReminderDao(): ItemReminderDao
    abstract fun deletedRecordDao(): DeletedRecordDao
    abstract fun activityEventDao(): ActivityEventDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun iconLibraryDao(): IconLibraryDao

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
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedSampleData(db)
                        }

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
                    .fallbackToDestructiveMigration(dropAllTables = true)
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE items_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, categoryId INTEGER, warehouseId INTEGER, tags TEXT NOT NULL DEFAULT '', purchaseDate INTEGER, expiryDate INTEGER, price REAL, quantity INTEGER NOT NULL DEFAULT 1, barcode TEXT, imageUri TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("INSERT INTO items_new (id, name, description, categoryId, warehouseId, tags, purchaseDate, expiryDate, price, quantity, barcode, imageUri, createdAt, updatedAt) SELECT id, name, description, categoryId, warehouseId, '', purchaseDate, expiryDate, price, quantity, barcode, imageUri, createdAt, updatedAt FROM items")
                db.execSQL("DROP TABLE items")
                db.execSQL("ALTER TABLE items_new RENAME TO items")
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN featureCode TEXT")
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN imageUri TEXT")
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE warehouses ADD COLUMN parentId INTEGER")
                db.execSQL("ALTER TABLE warehouses ADD COLUMN level INTEGER NOT NULL DEFAULT 1")
            }
        }
        
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN enableStockAlert INTEGER NOT NULL DEFAULT 1")
            }
        }
        
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 shopping_items 表添加 itemId 字段
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN itemId INTEGER")
            }
        }
        
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建删除记录表
                db.execSQL("""
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
        
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建动态事件表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS activity_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        targetId INTEGER,
                        targetName TEXT NOT NULL,
                        iconType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        metadata TEXT NOT NULL
                    )
                """)
            }
        }
        
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 warehouses 表添加 imageUri 字段
                db.execSQL("ALTER TABLE warehouses ADD COLUMN imageUri TEXT")
            }
        }
        
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为 warehouses 表添加 createdAt 字段，默认值为当前时间
                val currentTimeMillis = System.currentTimeMillis()
                db.execSQL("ALTER TABLE warehouses ADD COLUMN createdAt INTEGER NOT NULL DEFAULT $currentTimeMillis")
            }
        }
        
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建同步队列表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityUuid TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        entityJson TEXT NOT NULL,
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        maxRetries INTEGER NOT NULL DEFAULT 5,
                        lastAttemptAt INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                // 为 items 表添加 uuid 字段
                db.execSQL("ALTER TABLE items ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                // 为现有数据生成 UUID（使用 id 作为临时值，实际使用时会在代码中生成真正的 UUID）
                db.execSQL("UPDATE items SET uuid = 'item-' || id WHERE uuid = ''")
                
                // 为 categories 表添加 uuid 字段
                db.execSQL("ALTER TABLE categories ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE categories SET uuid = 'category-' || id WHERE uuid = ''")
                
                // 为 warehouses 表添加 uuid 字段
                db.execSQL("ALTER TABLE warehouses ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE warehouses SET uuid = 'warehouse-' || id WHERE uuid = ''")
                
                // 为 shopping_items 表添加 uuid 字段
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE shopping_items SET uuid = 'shopping-' || id WHERE uuid = ''")
                
                // 为 item_reminders 表添加 uuid 字段
                db.execSQL("ALTER TABLE item_reminders ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE item_reminders SET uuid = 'reminder-' || id WHERE uuid = ''")
                
                // 为 deleted_records 表添加 uuid 字段
                db.execSQL("ALTER TABLE deleted_records ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE deleted_records SET uuid = 'deleted-' || id WHERE uuid = ''")
                
                // 为 activity_events 表添加 uuid 字段
                db.execSQL("ALTER TABLE activity_events ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE activity_events SET uuid = 'event-' || id WHERE uuid = ''")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN imageKeys TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN isSample INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE warehouses ADD COLUMN isSample INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN isSample INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE warehouses ADD COLUMN imageKey TEXT")
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN imageKey TEXT")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. warehouses: parentId (INTEGER) -> parentUuid (TEXT)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS warehouses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        location TEXT NOT NULL DEFAULT '',
                        capacity INTEGER,
                        parentUuid TEXT,
                        level INTEGER NOT NULL DEFAULT 1,
                        imageUri TEXT,
                        imageKey TEXT,
                        createdAt INTEGER NOT NULL,
                        isSample INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO warehouses_new (id, uuid, name, description, location, capacity, parentUuid, level, imageUri, imageKey, createdAt, isSample)
                    SELECT w.id, w.uuid, w.name, w.description, w.location, w.capacity,
                        (SELECT p.uuid FROM warehouses p WHERE p.id = w.parentId),
                        w.level, w.imageUri, w.imageKey, w.createdAt, w.isSample
                    FROM warehouses w
                """.trimIndent())
                db.execSQL("DROP TABLE warehouses")
                db.execSQL("ALTER TABLE warehouses_new RENAME TO warehouses")

                // 2. items: categoryId, warehouseId -> categoryUuid, warehouseUuid
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        categoryUuid TEXT,
                        warehouseUuid TEXT,
                        tags TEXT NOT NULL DEFAULT '',
                        purchaseDate INTEGER,
                        expiryDate INTEGER,
                        price REAL,
                        quantity INTEGER NOT NULL DEFAULT 1,
                        barcode TEXT,
                        imageUri TEXT,
                        imageUris TEXT,
                        imageKeys TEXT NOT NULL DEFAULT '',
                        isSample INTEGER NOT NULL DEFAULT 0,
                        primaryImageIndex INTEGER NOT NULL DEFAULT 0,
                        featureCode TEXT,
                        enableStockAlert INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO items_new (id, uuid, name, description, categoryUuid, warehouseUuid, tags, purchaseDate, expiryDate, price, quantity, barcode, imageUri, imageUris, imageKeys, isSample, primaryImageIndex, featureCode, enableStockAlert, createdAt, updatedAt)
                    SELECT i.id, i.uuid, i.name, i.description,
                        (SELECT c.uuid FROM categories c WHERE c.id = i.categoryId),
                        (SELECT w.uuid FROM warehouses w WHERE w.id = i.warehouseId),
                        i.tags, i.purchaseDate, i.expiryDate, i.price, i.quantity, i.barcode, i.imageUri, i.imageUris, i.imageKeys, i.isSample, i.primaryImageIndex, i.featureCode, i.enableStockAlert, i.createdAt, i.updatedAt
                    FROM items i
                """.trimIndent())
                db.execSQL("DROP TABLE items")
                db.execSQL("ALTER TABLE items_new RENAME TO items")

                // 3. shopping_items: itemId -> itemUuid
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        quantity INTEGER NOT NULL DEFAULT 1,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        priority TEXT NOT NULL DEFAULT 'MEDIUM',
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        imageUri TEXT,
                        imageKey TEXT,
                        itemUuid TEXT,
                        isSample INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO shopping_items_new (id, uuid, name, description, quantity, isCompleted, priority, createdAt, completedAt, imageUri, imageKey, itemUuid, isSample)
                    SELECT s.id, s.uuid, s.name, s.description, s.quantity, s.isCompleted, s.priority, s.createdAt, s.completedAt, s.imageUri, s.imageKey,
                        (SELECT i.uuid FROM items i WHERE i.id = s.itemId),
                        s.isSample
                    FROM shopping_items s
                """.trimIndent())
                db.execSQL("DROP TABLE shopping_items")
                db.execSQL("ALTER TABLE shopping_items_new RENAME TO shopping_items")

                // 4. item_reminders: itemId -> itemUuid
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS item_reminders_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uuid TEXT NOT NULL,
                        itemUuid TEXT NOT NULL,
                        reminderType TEXT NOT NULL,
                        reminderTime INTEGER,
                        dailyTime TEXT,
                        monthlyDay INTEGER,
                        monthlyTime TEXT,
                        yearlyMonth INTEGER,
                        yearlyDay INTEGER,
                        yearlyTime TEXT,
                        reason TEXT NOT NULL DEFAULT '',
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO item_reminders_new (id, uuid, itemUuid, reminderType, reminderTime, dailyTime, monthlyDay, monthlyTime, yearlyMonth, yearlyDay, yearlyTime, reason, isEnabled, createdAt, updatedAt)
                    SELECT r.id, r.uuid,
                        (SELECT i.uuid FROM items i WHERE i.id = r.itemId),
                        r.reminderType, r.reminderTime, r.dailyTime, r.monthlyDay, r.monthlyTime, r.yearlyMonth, r.yearlyDay, r.yearlyTime, r.reason, r.isEnabled, r.createdAt, r.updatedAt
                    FROM item_reminders r
                """.trimIndent())
                db.execSQL("DROP TABLE item_reminders")
                db.execSQL("ALTER TABLE item_reminders_new RENAME TO item_reminders")

                // 5. activity_events: targetId -> targetUuid (targetId 可能为 item 或 warehouse)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS activity_events_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uuid TEXT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        targetUuid TEXT,
                        targetName TEXT NOT NULL DEFAULT '',
                        iconType TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        metadata TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO activity_events_new (id, uuid, type, title, description, targetUuid, targetName, iconType, createdAt, metadata)
                    SELECT e.id, e.uuid, e.type, e.title, e.description,
                        COALESCE(
                            (SELECT i.uuid FROM items i WHERE i.id = e.targetId),
                            (SELECT w.uuid FROM warehouses w WHERE w.id = e.targetId)
                        ),
                        e.targetName, e.iconType, e.createdAt, e.metadata
                    FROM activity_events e
                """.trimIndent())
                db.execSQL("DROP TABLE activity_events")
                db.execSQL("ALTER TABLE activity_events_new RENAME TO activity_events")

                // 6. deleted_records: entityId -> entityUuid
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS deleted_records_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uuid TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityUuid TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO deleted_records_new (id, uuid, entityType, entityUuid, deletedAt)
                    SELECT d.id, d.uuid, d.entityType,
                        CASE d.entityType
                            WHEN 'item' THEN (SELECT uuid FROM items WHERE id = d.entityId)
                            WHEN 'warehouse' THEN (SELECT uuid FROM warehouses WHERE id = d.entityId)
                            WHEN 'category' THEN (SELECT uuid FROM categories WHERE id = d.entityId)
                            WHEN 'shopping_item' THEN (SELECT uuid FROM shopping_items WHERE id = d.entityId)
                            WHEN 'reminder' THEN (SELECT uuid FROM item_reminders WHERE id = d.entityId)
                            ELSE ''
                        END,
                        d.deletedAt
                    FROM deleted_records d
                """.trimIndent())
                db.execSQL("DROP TABLE deleted_records")
                db.execSQL("ALTER TABLE deleted_records_new RENAME TO deleted_records")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. categories: uuid 为主键，移除 id
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories_new (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        color TEXT NOT NULL DEFAULT '#6200EE',
                        icon TEXT NOT NULL DEFAULT 'category'
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO categories_new (uuid, name, description, color, icon) SELECT uuid, name, description, color, icon FROM categories")
                db.execSQL("DROP TABLE categories")
                db.execSQL("ALTER TABLE categories_new RENAME TO categories")

                // 2. warehouses: uuid 为主键，移除 id
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS warehouses_new (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        location TEXT NOT NULL DEFAULT '',
                        capacity INTEGER,
                        parentUuid TEXT,
                        level INTEGER NOT NULL DEFAULT 1,
                        imageUri TEXT,
                        imageKey TEXT,
                        createdAt INTEGER NOT NULL,
                        isSample INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO warehouses_new (uuid, name, description, location, capacity, parentUuid, level, imageUri, imageKey, createdAt, isSample) SELECT uuid, name, description, location, capacity, parentUuid, level, imageUri, imageKey, createdAt, isSample FROM warehouses")
                db.execSQL("DROP TABLE warehouses")
                db.execSQL("ALTER TABLE warehouses_new RENAME TO warehouses")

                // 3. items: uuid 为主键，移除 id
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS items_new (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        categoryUuid TEXT,
                        warehouseUuid TEXT,
                        tags TEXT NOT NULL DEFAULT '',
                        purchaseDate INTEGER,
                        expiryDate INTEGER,
                        price REAL,
                        quantity INTEGER NOT NULL DEFAULT 1,
                        barcode TEXT,
                        imageUri TEXT,
                        imageUris TEXT,
                        imageKeys TEXT NOT NULL DEFAULT '',
                        isSample INTEGER NOT NULL DEFAULT 0,
                        primaryImageIndex INTEGER NOT NULL DEFAULT 0,
                        featureCode TEXT,
                        enableStockAlert INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO items_new (uuid, name, description, categoryUuid, warehouseUuid, tags, purchaseDate, expiryDate, price, quantity, barcode, imageUri, imageUris, imageKeys, isSample, primaryImageIndex, featureCode, enableStockAlert, createdAt, updatedAt) SELECT uuid, name, description, categoryUuid, warehouseUuid, tags, purchaseDate, expiryDate, price, quantity, barcode, imageUri, imageUris, imageKeys, isSample, primaryImageIndex, featureCode, enableStockAlert, createdAt, updatedAt FROM items")
                db.execSQL("DROP TABLE items")
                db.execSQL("ALTER TABLE items_new RENAME TO items")

                // 4. shopping_items: uuid 为主键，移除 id
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_items_new (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        quantity INTEGER NOT NULL DEFAULT 1,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        priority TEXT NOT NULL DEFAULT 'MEDIUM',
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        imageUri TEXT,
                        imageKey TEXT,
                        itemUuid TEXT,
                        isSample INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO shopping_items_new (uuid, name, description, quantity, isCompleted, priority, createdAt, completedAt, imageUri, imageKey, itemUuid, isSample) SELECT uuid, name, description, quantity, isCompleted, priority, createdAt, completedAt, imageUri, imageKey, itemUuid, isSample FROM shopping_items")
                db.execSQL("DROP TABLE shopping_items")
                db.execSQL("ALTER TABLE shopping_items_new RENAME TO shopping_items")

                // 5. item_reminders: uuid 为主键，移除 id
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS item_reminders_new (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        itemUuid TEXT NOT NULL,
                        reminderType TEXT NOT NULL,
                        reminderTime INTEGER,
                        dailyTime TEXT,
                        monthlyDay INTEGER,
                        monthlyTime TEXT,
                        yearlyMonth INTEGER,
                        yearlyDay INTEGER,
                        yearlyTime TEXT,
                        reason TEXT NOT NULL DEFAULT '',
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO item_reminders_new (uuid, itemUuid, reminderType, reminderTime, dailyTime, monthlyDay, monthlyTime, yearlyMonth, yearlyDay, yearlyTime, reason, isEnabled, createdAt, updatedAt) SELECT uuid, itemUuid, reminderType, reminderTime, dailyTime, monthlyDay, monthlyTime, yearlyMonth, yearlyDay, yearlyTime, reason, isEnabled, createdAt, updatedAt FROM item_reminders")
                db.execSQL("DROP TABLE item_reminders")
                db.execSQL("ALTER TABLE item_reminders_new RENAME TO item_reminders")

                // 6. activity_events: uuid 为主键，移除 id
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS activity_events_new (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        targetUuid TEXT,
                        targetName TEXT NOT NULL DEFAULT '',
                        iconType TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        metadata TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO activity_events_new (uuid, type, title, description, targetUuid, targetName, iconType, createdAt, metadata) SELECT uuid, type, title, description, targetUuid, targetName, iconType, createdAt, metadata FROM activity_events")
                db.execSQL("DROP TABLE activity_events")
                db.execSQL("ALTER TABLE activity_events_new RENAME TO activity_events")

                // 7. deleted_records: uuid 为主键，移除 id
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS deleted_records_new (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        entityType TEXT NOT NULL,
                        entityUuid TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO deleted_records_new (uuid, entityType, entityUuid, deletedAt) SELECT uuid, entityType, entityUuid, deletedAt FROM deleted_records")
                db.execSQL("DROP TABLE deleted_records")
                db.execSQL("ALTER TABLE deleted_records_new RENAME TO deleted_records")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE warehouses ADD COLUMN itemsSuffix TEXT")
                db.execSQL("ALTER TABLE warehouses ADD COLUMN hideUseButton INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE warehouses ADD COLUMN hideDetailsButton INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE warehouses ADD COLUMN hideQuantity INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE warehouses ADD COLUMN hideQuantitySlider INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN quantityUnit TEXT")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建图标库表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS icon_library (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        imagePath TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为图标库表添加 iconKey 和 updatedAt 字段
                db.execSQL("ALTER TABLE icon_library ADD COLUMN iconKey TEXT")
                val currentTimeMillis = System.currentTimeMillis()
                db.execSQL("ALTER TABLE icon_library ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $currentTimeMillis")
            }
        }

        private fun seedSampleData(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L
            val warehouseUuid1 = java.util.UUID.randomUUID().toString()
            val warehouseUuid2 = java.util.UUID.randomUUID().toString()
            val itemUuid1 = java.util.UUID.randomUUID().toString()
            val itemUuid2 = java.util.UUID.randomUUID().toString()
            val shoppingUuid1 = java.util.UUID.randomUUID().toString()

            db.execSQL(
                """
                INSERT INTO warehouses (uuid, name, description, location, capacity, parentUuid, level, imageUri, imageKey, createdAt, isSample, itemsSuffix, hideUseButton, hideDetailsButton, hideQuantity, hideQuantitySlider)
                VALUES ('$warehouseUuid1', '示例容器', '用于演示的示例容器', '家中', 50, NULL, 1, NULL, NULL, $now, 1, NULL, 0, 0, 0, 0)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO warehouses (uuid, name, description, location, capacity, parentUuid, level, imageUri, imageKey, createdAt, isSample, itemsSuffix, hideUseButton, hideDetailsButton, hideQuantity, hideQuantitySlider)
                VALUES ('$warehouseUuid2', '子容器', '子容器示例', '', NULL, '$warehouseUuid1', 2, NULL, NULL, $now, 1, NULL, 0, 0, 0, 0)
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO items (uuid, name, description, categoryUuid, warehouseUuid, tags, purchaseDate, expiryDate, price, quantity, barcode, imageUri, imageUris, imageKeys, isSample, primaryImageIndex, featureCode, enableStockAlert, createdAt, updatedAt)
                VALUES ('$itemUuid1', '示例物品A', '用于演示标签筛选和信息卡片', NULL, '$warehouseUuid1', '常用,食品',
                    ${now - dayMillis * 2}, ${now + dayMillis * 5}, 12.5, 3,
                    NULL, NULL, '', '', 1, 0, NULL, 1, $now, $now)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO items (uuid, name, description, categoryUuid, warehouseUuid, tags, purchaseDate, expiryDate, price, quantity, barcode, imageUri, imageUris, imageKeys, isSample, primaryImageIndex, featureCode, enableStockAlert, createdAt, updatedAt)
                VALUES ('$itemUuid2', '示例物品B', '点击网格卡片查看详情', NULL, '$warehouseUuid1', '工具,示例',
                    ${now - dayMillis * 3}, NULL, NULL, 1,
                    NULL, NULL, '', '', 1, 0, NULL, 1, $now, $now)
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO shopping_items (uuid, name, description, quantity, isCompleted, priority, createdAt, completedAt, imageUri, imageKey, itemUuid, isSample)
                VALUES ('$shoppingUuid1', '示例待购物品', '用于演示待购列表', 2, 0, 'MEDIUM', $now, NULL, NULL, NULL, NULL, 1)
                """.trimIndent()
            )
        }
    }
}
