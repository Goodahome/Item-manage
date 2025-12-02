package com.example.itemremindertool.data.repository

import com.example.itemremindertool.data.dao.ItemDao
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemStatus
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {
    fun getAllItems(): Flow<List<Item>> = itemDao.getAllItems()

    suspend fun getItemById(id: Long): Item? = itemDao.getItemById(id)

    fun getItemsByCategory(categoryId: Long): Flow<List<Item>> = itemDao.getItemsByCategory(categoryId)

    fun getItemsByWarehouse(warehouseId: Long): Flow<List<Item>> = itemDao.getItemsByWarehouse(warehouseId)

    fun getExpiredItems(currentTime: Long = System.currentTimeMillis()): Flow<List<Item>> = itemDao.getExpiredItems(currentTime)

    suspend fun getItemByBarcode(barcode: String): Item? = itemDao.getItemByBarcode(barcode)
    
    fun searchItemsByName(query: String): Flow<List<Item>> = itemDao.searchItemsByName(query)
    
    suspend fun getAllItemsList(): List<Item> = itemDao.getAllItemsList()

    fun getItemCount(): Flow<Int> = itemDao.getItemCount()

    fun getNormalItemCount(currentTime: Long = System.currentTimeMillis()): Flow<Int> = itemDao.getNormalItemCount(currentTime)

    fun getExpiredItemCount(currentTime: Long = System.currentTimeMillis()): Flow<Int> = itemDao.getExpiredItemCount(currentTime)

    suspend fun insertItem(item: Item): Long = itemDao.insertItem(item)

    suspend fun updateItem(item: Item) = itemDao.updateItem(item)

    suspend fun deleteItem(item: Item) = itemDao.deleteItem(item)

    suspend fun deleteItemById(id: Long) = itemDao.deleteItemById(id)
}

