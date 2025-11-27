package com.example.itemremindertool.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.itemremindertool.data.converters.DateConverters
import com.example.itemremindertool.data.dao.CategoryDao
import com.example.itemremindertool.data.dao.ItemDao
import com.example.itemremindertool.data.dao.ShoppingItemDao
import com.example.itemremindertool.data.dao.WarehouseDao
import com.example.itemremindertool.data.model.Category
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.data.model.Warehouse

@Database(
    entities = [Item::class, Category::class, ShoppingItem::class, Warehouse::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun warehouseDao(): WarehouseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "item_reminder_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

