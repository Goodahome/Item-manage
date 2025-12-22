package com.example.itemremindertool.data.repository

import android.content.Context
import com.example.itemremindertool.data.dao.ShoppingItemDao
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.data.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import java.util.Date

class ShoppingItemRepository(
    private val shoppingItemDao: ShoppingItemDao,
    private val context: Context? = null
) {
    fun getAllShoppingItems(): Flow<List<ShoppingItem>> = shoppingItemDao.getAllShoppingItems()

    fun getActiveShoppingItems(): Flow<List<ShoppingItem>> = shoppingItemDao.getActiveShoppingItems()

    suspend fun getAllShoppingItemsSync(): List<ShoppingItem> = shoppingItemDao.getAllShoppingItemsSync()

    suspend fun getShoppingItemById(id: Long): ShoppingItem? = shoppingItemDao.getShoppingItemById(id)

    suspend fun insertShoppingItem(item: ShoppingItem): Long = shoppingItemDao.insertShoppingItem(item)

    suspend fun updateShoppingItem(item: ShoppingItem) = shoppingItemDao.updateShoppingItem(item)

    suspend fun deleteShoppingItem(item: ShoppingItem) {
        shoppingItemDao.deleteShoppingItem(item)
        // 如果是已完成的购物项，记录购买动态
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
    }

    suspend fun deleteShoppingItemById(id: Long) = shoppingItemDao.deleteShoppingItemById(id)
}

