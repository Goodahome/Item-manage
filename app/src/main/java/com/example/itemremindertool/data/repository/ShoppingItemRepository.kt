package com.example.itemremindertool.data.repository

import com.example.itemremindertool.data.dao.ShoppingItemDao
import com.example.itemremindertool.data.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

class ShoppingItemRepository(private val shoppingItemDao: ShoppingItemDao) {
    fun getAllShoppingItems(): Flow<List<ShoppingItem>> = shoppingItemDao.getAllShoppingItems()

    fun getActiveShoppingItems(): Flow<List<ShoppingItem>> = shoppingItemDao.getActiveShoppingItems()

    suspend fun getAllShoppingItemsSync(): List<ShoppingItem> = shoppingItemDao.getAllShoppingItemsSync()

    suspend fun getShoppingItemById(id: Long): ShoppingItem? = shoppingItemDao.getShoppingItemById(id)

    suspend fun insertShoppingItem(item: ShoppingItem): Long = shoppingItemDao.insertShoppingItem(item)

    suspend fun updateShoppingItem(item: ShoppingItem) = shoppingItemDao.updateShoppingItem(item)

    suspend fun deleteShoppingItem(item: ShoppingItem) = shoppingItemDao.deleteShoppingItem(item)

    suspend fun deleteShoppingItemById(id: Long) = shoppingItemDao.deleteShoppingItemById(id)
}

