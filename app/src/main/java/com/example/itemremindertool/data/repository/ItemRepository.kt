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
import com.example.itemremindertool.utils.formatQuantityWithUnit
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

    suspend fun getItemByUuid(uuid: String): Item? = itemDao.getItemByUuid(uuid)

    fun getItemsByCategory(categoryUuid: String): Flow<List<Item>> = itemDao.getItemsByCategory(categoryUuid)

    fun getItemsByWarehouse(warehouseUuid: String): Flow<List<Item>> = itemDao.getItemsByWarehouse(warehouseUuid)

    fun getExpiredItems(currentTime: Long = System.currentTimeMillis()): Flow<List<Item>> = itemDao.getExpiredItems(currentTime)

    fun getItemsExpiringBetween(startTime: Long, endTime: Long): Flow<List<Item>> = itemDao.getItemsExpiringBetween(startTime, endTime)

    suspend fun getItemByBarcode(barcode: String): Item? = itemDao.getItemByBarcode(barcode)
    
    fun searchItemsByName(query: String): Flow<List<Item>> = itemDao.searchItemsByName(query)
    
    suspend fun getAllItemsList(): List<Item> = itemDao.getAllItemsList()

    fun getItemCount(): Flow<Int> = itemDao.getItemCount()

    fun getNormalItemCount(currentTime: Long = System.currentTimeMillis()): Flow<Int> = itemDao.getNormalItemCount(currentTime)

    fun getExpiredItemCount(currentTime: Long = System.currentTimeMillis()): Flow<Int> = itemDao.getExpiredItemCount(currentTime)

    suspend fun insertItem(item: Item) {
        // 1. 本地写入
        itemDao.insertItem(item)
        val savedItem = itemDao.getItemByUuid(item.uuid) ?: item
        
        // 2. 记录动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.ITEM_ADDED,
                    title = it.getString(com.example.itemremindertool.R.string.event_added_item),
                    description = item.name,
                    targetUuid = savedItem.uuid,
                    targetName = item.name,
                    iconType = "add_item",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
                syncManager?.let { manager ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            manager.syncActivityEventToRemote(event)
                        } catch (e: Exception) {
                            android.util.Log.e("ItemRepository", "同步添加物品动态失败", e)
                        }
                    }
                }
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
                    targetUuid = item.uuid,
                    targetName = item.name,
                    iconType = "update_item",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
                syncManager?.let { manager ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            manager.syncActivityEventToRemote(event)
                        } catch (e: Exception) {
                            android.util.Log.e("ItemRepository", "同步更新物品动态失败", e)
                        }
                    }
                }
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
                val quantityText = formatQuantityWithUnit(usedQuantity, item.quantityUnit)
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.ITEM_USED,
                    title = it.getString(com.example.itemremindertool.R.string.event_used_item),
                    description = "${item.name} × $quantityText",
                    targetUuid = item.uuid,
                    targetName = item.name,
                    iconType = "use_item",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
                syncManager?.let { manager ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            manager.syncActivityEventToRemote(event)
                        } catch (e: Exception) {
                            android.util.Log.e("ItemRepository", "同步使用物品动态失败", e)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ItemRepository", "记录使用物品动态失败", e)
            }
        }
        // 远端同步（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncItemToRemote(updatedItem)
                } catch (e: Exception) {
                    android.util.Log.e("ItemRepository", "同步物品到远端失败", e)
                }
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
                entityUuid = item.uuid,
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
                    targetUuid = item.uuid,
                    targetName = item.name,
                    iconType = "delete_item",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
                syncManager?.let { manager ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            manager.syncActivityEventToRemote(event)
                        } catch (e: Exception) {
                            android.util.Log.e("ItemRepository", "同步删除物品动态失败", e)
                        }
                    }
                }
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
    suspend fun deleteItemByUuid(uuid: String) {
        val item = itemDao.getItemByUuid(uuid)
        if (item != null) {
            deleteItem(item)
        } else {
            itemDao.deleteItemByUuid(uuid)
        }
    }
    
    /**
     * 批量删除物品（合并为单条动态记录）
     */
    @androidx.room.Transaction
    suspend fun batchDeleteItems(items: List<Item>) {
        if (items.isEmpty()) return
        
        val itemNames = items.take(3).joinToString("、") { it.name }
        val displayText = if (items.size > 3) {
            "$itemNames 等${items.size}个物品"
        } else {
            itemNames
        }
        
        // 1. 批量本地删除和记录删除操作
        items.forEach { item ->
            itemDao.deleteItem(item)
            deletedRecordDao?.insertDeletedRecord(
                DeletedRecord(
                    entityType = "item",
                    entityUuid = item.uuid,
                    deletedAt = Date()
                )
            )
        }
        
        // 2. 记录一条合并的动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.ITEM_DELETED,
                    title = "批量删除物品",
                    description = displayText,
                    targetUuid = items.first().uuid,
                    targetName = displayText,
                    iconType = "batch_delete_item",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
                syncManager?.let { manager ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            manager.syncActivityEventToRemote(event)
                        } catch (e: Exception) {
                            android.util.Log.e("ItemRepository", "同步批量删除物品动态失败", e)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ItemRepository", "记录批量删除物品动态失败", e)
            }
        }
        
        // 3. 批量远端同步删除（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                items.forEach { item ->
                    try {
                        manager.deleteItemFromRemote(item.uuid)
                    } catch (e: Exception) {
                        android.util.Log.e("ItemRepository", "同步删除物品${item.name}到远端失败", e)
                    }
                }
            }
        }
    }
    
    /**
     * 批量移动物品到指定容器（合并为单条动态记录）
     */
    @androidx.room.Transaction
    suspend fun batchMoveItems(items: List<Item>, targetWarehouseUuid: String?, targetWarehouseName: String?) {
        if (items.isEmpty()) return
        
        val itemNames = items.take(3).joinToString("、") { it.name }
        val displayText = if (items.size > 3) {
            "$itemNames 等${items.size}个物品"
        } else {
            itemNames
        }
        
        val targetText = targetWarehouseName ?: "根目录"
        
        // 1. 批量本地更新
        val updatedItems = items.map { item ->
            item.copy(
                warehouseUuid = targetWarehouseUuid,
                updatedAt = Date()
            )
        }
        updatedItems.forEach { item ->
            itemDao.updateItem(item)
        }
        
        // 2. 记录一条合并的动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.ITEM_UPDATED,
                    title = "批量移动物品",
                    description = "将 $displayText 移动到 $targetText",
                    targetUuid = items.first().uuid,
                    targetName = displayText,
                    iconType = "batch_move_item",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
                syncManager?.let { manager ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            manager.syncActivityEventToRemote(event)
                        } catch (e: Exception) {
                            android.util.Log.e("ItemRepository", "同步批量移动物品动态失败", e)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ItemRepository", "记录批量移动物品动态失败", e)
            }
        }
        
        // 3. 批量远端同步（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                updatedItems.forEach { item ->
                    try {
                        manager.syncItemToRemote(item)
                    } catch (e: Exception) {
                        android.util.Log.e("ItemRepository", "同步物品${item.name}到远端失败", e)
                    }
                }
            }
        }
    }
}

