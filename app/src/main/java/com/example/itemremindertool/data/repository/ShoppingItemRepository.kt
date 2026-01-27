package com.example.itemremindertool.data.repository

import android.content.Context
import com.example.itemremindertool.data.dao.ShoppingItemDao
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class ShoppingItemRepository(
    private val shoppingItemDao: ShoppingItemDao,
    private val context: Context? = null
) {
    private val syncManager: SyncManager? by lazy {
        context?.let { SyncManager.getInstance(it) }
    }
    fun getAllShoppingItems(): Flow<List<ShoppingItem>> = shoppingItemDao.getAllShoppingItems()

    fun getActiveShoppingItems(): Flow<List<ShoppingItem>> = shoppingItemDao.getActiveShoppingItems()

    suspend fun getAllShoppingItemsSync(): List<ShoppingItem> = shoppingItemDao.getAllShoppingItemsSync()

    suspend fun getShoppingItemById(id: Long): ShoppingItem? = shoppingItemDao.getShoppingItemById(id)

    suspend fun insertShoppingItem(item: ShoppingItem): Long {
        // 1. 本地写入
        val itemId = shoppingItemDao.insertShoppingItem(item)
        val savedItem = shoppingItemDao.getShoppingItemById(itemId) ?: item
        
        // 2. 远端同步（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncShoppingItemToRemote(savedItem)
                } catch (e: Exception) {
                    android.util.Log.e("ShoppingItemRepository", "同步购物项到远端失败", e)
                }
            }
        }
        
        return itemId
    }

    suspend fun updateShoppingItem(item: ShoppingItem) {
        // 1. 本地更新
        shoppingItemDao.updateShoppingItem(item)
        
        // 2. 远端同步（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncShoppingItemToRemote(item)
                } catch (e: Exception) {
                    android.util.Log.e("ShoppingItemRepository", "同步购物项到远端失败", e)
                }
            }
        }
    }

    suspend fun deleteShoppingItem(item: ShoppingItem) {
        val uuid = item.uuid
        
        // 1. 本地删除
        shoppingItemDao.deleteShoppingItem(item)
        
        // 2. 如果是已完成的购物项，记录购买动态
        if (item.isCompleted) {
            context?.let {
                try {
                    val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                    val event = com.example.itemremindertool.data.model.ActivityEvent(
                        type = com.example.itemremindertool.data.model.ActivityEventType.ITEM_ADDED,
                        title = it.getString(com.example.itemremindertool.R.string.event_purchased_item),
                        description = "${item.name}${if (item.quantity > 1) " × ${item.quantity}" else ""}",
                        targetId = item.itemId,
                        targetName = item.name,
                        iconType = "purchase",
                        createdAt = Date()
                    )
                    activityEventDao.insert(event)
                } catch (e: Exception) {
                    android.util.Log.e("ShoppingItemRepository", "记录购买动态失败", e)
                }
            }
        }
        
        // 3. 远端同步删除（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.deleteShoppingItemFromRemote(uuid)
                } catch (e: Exception) {
                    android.util.Log.e("ShoppingItemRepository", "同步删除购物项到远端失败", e)
                }
            }
        }
    }

    suspend fun deleteShoppingItemById(id: Long) {
        // 先获取购物项的 uuid
        val item = shoppingItemDao.getShoppingItemById(id)
        if (item != null) {
            deleteShoppingItem(item)
        } else {
            shoppingItemDao.deleteShoppingItemById(id)
        }
    }
}

