package com.example.itemremindertool.data.dao

import androidx.room.*
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY updatedAt DESC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE uuid = :uuid LIMIT 1")
    suspend fun getItemByUuid(uuid: String): Item?

    @Query("SELECT * FROM items WHERE categoryUuid = :categoryUuid ORDER BY updatedAt DESC")
    fun getItemsByCategory(categoryUuid: String): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE warehouseUuid = :warehouseUuid ORDER BY updatedAt DESC")
    fun getItemsByWarehouse(warehouseUuid: String): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE warehouseUuid = :warehouseUuid ORDER BY updatedAt DESC")
    suspend fun getItemsByWarehouseSync(warehouseUuid: String): List<Item>

    @Query("SELECT * FROM items WHERE expiryDate IS NOT NULL AND expiryDate < :currentTime ORDER BY updatedAt DESC")
    fun getExpiredItems(currentTime: Long): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE expiryDate IS NOT NULL AND expiryDate >= :startTime AND expiryDate <= :endTime ORDER BY expiryDate ASC")
    fun getItemsExpiringBetween(startTime: Long, endTime: Long): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): Item?
    
    @Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%'")
    fun searchItemsByName(query: String): Flow<List<Item>>
    
    @Query("SELECT * FROM items ORDER BY updatedAt DESC")
    suspend fun getAllItemsList(): List<Item>

    @Query("SELECT COUNT(*) FROM items")
    fun getItemCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE expiryDate IS NULL OR expiryDate >= :currentTime")
    fun getNormalItemCount(currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM items WHERE expiryDate IS NOT NULL AND expiryDate < :currentTime")
    fun getExpiredItemCount(currentTime: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item): Int

    @Delete
    suspend fun deleteItem(item: Item): Int

    @Query("DELETE FROM items WHERE uuid = :uuid")
    suspend fun deleteItemByUuid(uuid: String): Int

    @Query("DELETE FROM items WHERE warehouseUuid = :warehouseUuid")
    suspend fun deleteItemsByWarehouse(warehouseUuid: String): Int
    
    @Query("SELECT COUNT(*) FROM items WHERE warehouseUuid = :warehouseUuid")
    suspend fun getItemCountByWarehouse(warehouseUuid: String): Int
}

