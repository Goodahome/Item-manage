package com.example.itemremindertool.data.repository

import android.content.Context
import com.example.itemremindertool.data.dao.CategoryDao
import com.example.itemremindertool.data.model.Category
import com.example.itemremindertool.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val context: Context? = null
) {
    private val syncManager: SyncManager? by lazy {
        context?.let { SyncManager.getInstance(it) }
    }
    
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getCategoryByUuid(uuid: String): Category? = categoryDao.getCategoryByUuid(uuid)

    suspend fun insertCategory(category: Category) {
        // 1. 本地写入
        categoryDao.insertCategory(category)
        val savedCategory = categoryDao.getCategoryByUuid(category.uuid) ?: category
        
        // 2. 远端同步（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncCategoryToRemote(savedCategory)
                } catch (e: Exception) {
                    android.util.Log.e("CategoryRepository", "同步分类到远端失败", e)
                }
            }
        }
    }

    suspend fun updateCategory(category: Category) {
        // 1. 本地更新
        categoryDao.updateCategory(category)
        
        // 2. 远端同步（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncCategoryToRemote(category)
                } catch (e: Exception) {
                    android.util.Log.e("CategoryRepository", "同步分类到远端失败", e)
                }
            }
        }
    }

    suspend fun deleteCategory(category: Category) {
        val uuid = category.uuid
        
        // 1. 本地删除
        categoryDao.deleteCategory(category)
        
        // 2. 远端同步删除（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.deleteCategoryFromRemote(uuid)
                } catch (e: Exception) {
                    android.util.Log.e("CategoryRepository", "同步删除分类到远端失败", e)
                }
            }
        }
    }

    suspend fun deleteCategoryByUuid(uuid: String) {
        val category = categoryDao.getCategoryByUuid(uuid)
        if (category != null) {
            deleteCategory(category)
        } else {
            categoryDao.deleteCategoryByUuid(uuid)
        }
    }
}

