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

    fun getItemsByStatus(status: ItemStatus): Flow<List<Item>> = itemDao.getItemsByStatus(status)

    suspend fun getItemByBarcode(barcode: String): Item? = itemDao.getItemByBarcode(barcode)

    fun getItemCount(): Flow<Int> = itemDao.getItemCount()

    fun getItemCountByStatus(status: ItemStatus): Flow<Int> = itemDao.getItemCountByStatus(status)

    suspend fun insertItem(item: Item): Long = itemDao.insertItem(item)

    suspend fun updateItem(item: Item) = itemDao.updateItem(item)

    suspend fun deleteItem(item: Item) = itemDao.deleteItem(item)

    suspend fun deleteItemById(id: Long) = itemDao.deleteItemById(id)
}

