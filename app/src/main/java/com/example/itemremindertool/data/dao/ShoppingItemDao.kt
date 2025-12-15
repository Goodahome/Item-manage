package com.example.itemremindertool.data.dao

import androidx.room.*
import com.example.itemremindertool.data.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items ORDER BY priority DESC, createdAt DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingItem>>
    
    @Query("SELECT * FROM shopping_items ORDER BY priority DESC, createdAt DESC")
    suspend fun getAllShoppingItemsSync(): List<ShoppingItem>

    @Query("SELECT * FROM shopping_items WHERE isCompleted = 0 ORDER BY priority DESC, createdAt DESC")
    fun getActiveShoppingItems(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE id = :id")
    suspend fun getShoppingItemById(id: Long): ShoppingItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem): Long

    @Update
    suspend fun updateShoppingItem(item: ShoppingItem)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteShoppingItemById(id: Long)
}

