package com.example.itemremindertool.sync

import android.content.Context
import android.util.Log
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.SyncOperation
import com.example.itemremindertool.data.model.SyncQueueItem
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * 同步队列管理器
 * 管理失败的同步任务，提供重试机制
 */
class SyncQueue(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).syncQueueDao()
    private val gson = Gson()
    private val syncManager = SyncManager.getInstance(context)
    
    companion object {
        private const val TAG = "SyncQueue"
        
        @Volatile
        private var instance: SyncQueue? = null
        
        fun getInstance(context: Context): SyncQueue {
            return instance ?: synchronized(this) {
                instance ?: SyncQueue(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * 添加失败的同步任务到队列
     */
    suspend fun addToQueue(
        entityType: String,
        entityUuid: String,
        operation: SyncOperation,
        entityJson: String
    ) = withContext(Dispatchers.IO) {
        try {
            // 检查是否已存在相同的任务
            val existing = dao.findByUuidAndOperation(entityUuid, operation)
            if (existing != null) {
                // 更新重试次数
                dao.update(existing.copy(
                    retryCount = existing.retryCount + 1,
                    lastAttemptAt = Date()
                ))
                Log.d(TAG, "更新同步队列项：$entityType $entityUuid $operation，重试次数：${existing.retryCount + 1}")
            } else {
                // 添加新任务
                dao.insert(
                    SyncQueueItem(
                        entityType = entityType,
                        entityUuid = entityUuid,
                        operation = operation,
                        entityJson = entityJson,
                        retryCount = 1,
                        lastAttemptAt = Date()
                    )
                )
                Log.d(TAG, "添加到同步队列：$entityType $entityUuid $operation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "添加同步队列项失败", e)
        }
    }
    
    /**
     * 处理队列中的所有待同步项
     * @return 成功处理的数量
     */
    suspend fun processQueue(): Int = withContext(Dispatchers.IO) {
        val pendingItems = dao.getAllPendingItems()
        var successCount = 0
        
        Log.d(TAG, "开始处理同步队列，待处理项数：${pendingItems.size}")
        
        for (item in pendingItems) {
            val success = processQueueItem(item)
            if (success) {
                successCount++
                // 删除成功的项
                dao.delete(item)
                Log.d(TAG, "同步成功并移除队列：${item.entityType} ${item.entityUuid}")
            } else {
                // 更新重试次数
                val updated = item.copy(
                    retryCount = item.retryCount + 1,
                    lastAttemptAt = Date()
                )
                dao.update(updated)
                Log.d(TAG, "同步失败，更新重试次数：${item.entityType} ${item.entityUuid}，重试次数：${updated.retryCount}")
            }
        }
        
        // 清理超过最大重试次数的项
        dao.clearExceededRetries()
        
        Log.d(TAG, "同步队列处理完成，成功：$successCount/${pendingItems.size}")
        successCount
    }
    
    /**
     * 处理单个队列项
     */
    private suspend fun processQueueItem(item: SyncQueueItem): Boolean {
        return try {
            when (item.entityType) {
                "item" -> processItemSync(item)
                "category" -> processCategorySync(item)
                "warehouse" -> processWarehouseSync(item)
                "shopping_item" -> processShoppingItemSync(item)
                "reminder" -> processReminderSync(item)
                "activity_event" -> processActivityEventSync(item)
                else -> {
                    Log.w(TAG, "未知的实体类型：${item.entityType}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理队列项失败：${item.entityType} ${item.entityUuid}", e)
            false
        }
    }
    
    private suspend fun processItemSync(item: SyncQueueItem): Boolean {
        return when (item.operation) {
            SyncOperation.DELETE -> {
                val result = syncManager.deleteItemFromRemote(item.entityUuid)
                result.isSuccess
            }
            else -> {
                // CREATE 和 UPDATE 都使用 upsert
                val entity = gson.fromJson(item.entityJson, com.example.itemremindertool.data.model.Item::class.java)
                val result = syncManager.syncItemToRemote(entity)
                result.isSuccess
            }
        }
    }
    
    private suspend fun processCategorySync(item: SyncQueueItem): Boolean {
        return when (item.operation) {
            SyncOperation.DELETE -> {
                val result = syncManager.deleteCategoryFromRemote(item.entityUuid)
                result.isSuccess
            }
            else -> {
                val entity = gson.fromJson(item.entityJson, com.example.itemremindertool.data.model.Category::class.java)
                val result = syncManager.syncCategoryToRemote(entity)
                result.isSuccess
            }
        }
    }
    
    private suspend fun processWarehouseSync(item: SyncQueueItem): Boolean {
        return when (item.operation) {
            SyncOperation.DELETE -> {
                val result = syncManager.deleteWarehouseFromRemote(item.entityUuid)
                result.isSuccess
            }
            else -> {
                val entity = gson.fromJson(item.entityJson, com.example.itemremindertool.data.model.Warehouse::class.java)
                val result = syncManager.syncWarehouseToRemote(entity)
                result.isSuccess
            }
        }
    }
    
    private suspend fun processShoppingItemSync(item: SyncQueueItem): Boolean {
        return when (item.operation) {
            SyncOperation.DELETE -> {
                val result = syncManager.deleteShoppingItemFromRemote(item.entityUuid)
                result.isSuccess
            }
            else -> {
                val entity = gson.fromJson(item.entityJson, com.example.itemremindertool.data.model.ShoppingItem::class.java)
                val result = syncManager.syncShoppingItemToRemote(entity)
                result.isSuccess
            }
        }
    }

    private suspend fun processReminderSync(item: SyncQueueItem): Boolean {
        return when (item.operation) {
            SyncOperation.DELETE -> {
                val result = syncManager.deleteReminderFromRemote(item.entityUuid)
                result.isSuccess
            }
            else -> {
                val entity = gson.fromJson(item.entityJson, com.example.itemremindertool.data.model.ItemReminder::class.java)
                val result = syncManager.syncReminderToRemote(entity)
                result.isSuccess
            }
        }
    }

    private suspend fun processActivityEventSync(item: SyncQueueItem): Boolean {
        return when (item.operation) {
            SyncOperation.DELETE -> {
                Log.w(TAG, "动态不支持删除同步：${item.entityUuid}")
                false
            }
            else -> {
                val entity = gson.fromJson(item.entityJson, com.example.itemremindertool.data.model.ActivityEvent::class.java)
                val result = syncManager.syncActivityEventToRemote(entity)
                result.isSuccess
            }
        }
    }
    
    /**
     * 获取待同步项数量的 Flow
     */
    fun getPendingCountFlow() = dao.getPendingCount()
    
    /**
     * 清空队列
     */
    suspend fun clearQueue() {
        dao.clearAll()
        Log.d(TAG, "同步队列已清空")
    }
}
