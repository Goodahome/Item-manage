package com.example.itemremindertool.data.dao

import androidx.room.*
import com.example.itemremindertool.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>
    
    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesSync(): List<Category>

    @Query("SELECT * FROM categories WHERE uuid = :uuid LIMIT 1")
    suspend fun getCategoryByUuid(uuid: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category): Int

    @Delete
    suspend fun deleteCategory(category: Category): Int

    @Query("DELETE FROM categories WHERE uuid = :uuid")
    suspend fun deleteCategoryByUuid(uuid: String): Int
}

