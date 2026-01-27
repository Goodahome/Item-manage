package com.example.itemremindertool.data.repository

import android.content.Context
import androidx.room.Transaction
import com.example.itemremindertool.data.dao.ItemDao
import com.example.itemremindertool.data.dao.DeletedRecordDao
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemStatus
import com.example.itemremindertool.data.model.DeletedRecord
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.sync.SyncManager
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ItemRepository(
    private val itemDao: ItemDao,
    private val deletedRecordDao: DeletedRecordDao? = null,
    private val context: Context? = null
) {
    private val syncManager: SyncManager? by lazy {
        context?.let { SyncManager.getInstance(it) }
    }
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
        // 1. 本地写入
        val itemId = itemDao.insertItem(item)
        val savedItem = itemDao.getItemById(itemId) ?: item
        
        // 2. 记录动态
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
        
        // 3. 远端同步（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncItemToRemote(savedItem)
                } catch (e: Exception) {
                    android.util.Log.e("ItemRepository", "同步物品到远端失败", e)
                }
            }
        }
        
        return itemId
    }

    suspend fun updateItem(item: Item) {
        // 1. 本地更新
        itemDao.updateItem(item)
        
        // 2. 记录动态
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
        
        // 3. 远端同步（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncItemToRemote(item)
                } catch (e: Exception) {
                    android.util.Log.e("ItemRepository", "同步物品到远端失败", e)
                }
            }
        }
    }
    
    /**
     * 使用物品（减少数量并记录使用事件）
     */
    suspend fun useItem(item: Item, usedQuantity: Int) {
        val newQuantity = (item.quantity - usedQuantity).coerceAtLeast(0)
        val updatedItem = item.copy(quantity = newQuantity, updatedAt = Date())
        itemDao.updateItem(updatedItem)
        // 记录使用物品动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.ITEM_USED,
                    title = it.getString(com.example.itemremindertool.R.string.event_used_item),
                    description = "${item.name} × $usedQuantity",
                    targetId = item.id,
                    targetName = item.name,
                    iconType = "use_item",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
            } catch (e: Exception) {
                android.util.Log.e("ItemRepository", "记录使用物品动态失败", e)
            }
        }
    }

    @androidx.room.Transaction
    suspend fun deleteItem(item: Item) {
        val uuid = item.uuid
        
        // 1. 本地删除和记录删除操作
        itemDao.deleteItem(item)
        deletedRecordDao?.insertDeletedRecord(
            DeletedRecord(
                entityType = "item",
                entityId = item.id,
                deletedAt = Date()
            )
        )
        
        // 2. 记录动态
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
        
        // 3. 远端同步删除（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.deleteItemFromRemote(uuid)
                } catch (e: Exception) {
                    android.util.Log.e("ItemRepository", "同步删除物品到远端失败", e)
                }
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

