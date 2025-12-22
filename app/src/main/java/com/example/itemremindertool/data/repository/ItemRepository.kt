package com.example.itemremindertool.data.repository

import android.content.Context
import androidx.room.Transaction
import com.example.itemremindertool.data.dao.ItemDao
import com.example.itemremindertool.data.dao.DeletedRecordDao
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemStatus
import com.example.itemremindertool.data.model.DeletedRecord
import com.example.itemremindertool.data.database.AppDatabase
import java.util.Date
import kotlinx.coroutines.flow.Flow

class ItemRepository(
    private val itemDao: ItemDao,
    private val deletedRecordDao: DeletedRecordDao? = null,
    private val context: Context? = null
) {
    fun getAllItems(): Flow<List<Item>> = itemDao.getAllItems()

    suspend fun getItemById(id: Long): Item? = itemDao.getItemById(id)

    fun getItemsByCategory(categoryId: Long): Flow<List<Item>> = itemDao.getItemsByCategory(categoryId)

    fun getItemsByWarehouse(warehouseId: Long): Flow<List<Item>> = itemDao.getItemsByWarehouse(warehouseId)

    fun getExpiredItems(currentTime: Long = System.currentTimeMillis()): Flow<List<Item>> = itemDao.getExpiredItems(currentTime)

    fun getItemsExpiringBetween(startTime: Long, endTime: Long): Flow<List<Item>> = itemDao.getItemsExpiringBetween(startTime, endTime)

    suspend fun getItemByBarcode(barcode: String): Item? = itemDao.getItemByBarcode(barcode)
    
    fun searchItemsByName(query: String): Flow<List<Item>> = itemDao.searchItemsByName(query)
    
    suspend fun getAllItemsList(): List<Item> = itemDao.getAllItemsList()

    fun getItemCount(): Flow<Int> = itemDao.getItemCount()

    fun getNormalItemCount(currentTime: Long = System.currentTimeMillis()): Flow<Int> = itemDao.getNormalItemCount(currentTime)

    fun getExpiredItemCount(currentTime: Long = System.currentTimeMillis()): Flow<Int> = itemDao.getExpiredItemCount(currentTime)

    suspend fun insertItem(item: Item): Long {
        val itemId = itemDao.insertItem(item)
        // 记录动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.ITEM_ADDED,
                    title = it.getString(com.example.itemremindertool.R.string.event_added_item),
                    description = item.name,
                    targetId = itemId,
                    targetName = item.name,
                    iconType = "add_item",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
            } catch (e: Exception) {
                android.util.Log.e("ItemRepository", "记录添加物品动态失败", e)
            }
        }
        return itemId
    }

    suspend fun updateItem(item: Item) {
        itemDao.updateItem(item)
        // 记录动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.ITEM_UPDATED,
                    title = it.getString(com.example.itemremindertool.R.string.event_updated_item),
                    description = item.name,
                    targetId = item.id,
                    targetName = item.name,
                    iconType = "update_item",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
            } catch (e: Exception) {
                android.util.Log.e("ItemRepository", "记录更新物品动态失败", e)
            }
        }
    }

    @androidx.room.Transaction
    suspend fun deleteItem(item: Item) {
        // 在事务中删除数据和记录删除操作，确保原子性
        itemDao.deleteItem(item)
        // 记录删除操作
        deletedRecordDao?.insertDeletedRecord(
            DeletedRecord(
                entityType = "item",
                entityId = item.id,
                deletedAt = Date()
            )
        )
        // 记录动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.ITEM_DELETED,
                    title = it.getString(com.example.itemremindertool.R.string.event_deleted_item),
                    description = item.name,
                    targetId = item.id,
                    targetName = item.name,
                    iconType = "delete_item",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
            } catch (e: Exception) {
                android.util.Log.e("ItemRepository", "记录删除物品动态失败", e)
            }
        }
    }

    @androidx.room.Transaction
    suspend fun deleteItemById(id: Long) {
        // 在事务中删除数据和记录删除操作，确保原子性
        itemDao.deleteItemById(id)
        // 记录删除操作
        deletedRecordDao?.insertDeletedRecord(
            DeletedRecord(
                entityType = "item",
                entityId = id,
                deletedAt = Date()
            )
        )
    }
}

