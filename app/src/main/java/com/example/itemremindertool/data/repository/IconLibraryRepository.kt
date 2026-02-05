package com.example.itemremindertool.data.repository

import android.content.Context
import android.util.Log
import com.example.itemremindertool.data.dao.IconLibraryDao
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.IconLibraryItem
import com.example.itemremindertool.sync.SyncManager
import com.example.itemremindertool.utils.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

class IconLibraryRepository(context: Context) {
    private val iconLibraryDao: IconLibraryDao = AppDatabase.getDatabase(context).iconLibraryDao()
    private val appContext = context.applicationContext
    private val syncManager: SyncManager? = try { SyncManager.getInstance(context) } catch (e: Exception) { null }
    
    /**
     * 获取所有图标
     */
    fun getAllIcons(): Flow<List<IconLibraryItem>> = iconLibraryDao.getAllIcons()
    
    /**
     * 获取所有图标列表
     */
    suspend fun getAllIconsList(): List<IconLibraryItem> = iconLibraryDao.getAllIconsList()
    
    /**
     * 根据UUID获取图标
     */
    suspend fun getIconByUuid(uuid: String): IconLibraryItem? = iconLibraryDao.getIconByUuid(uuid)
    
    /**
     * 插入图标
     */
    suspend fun insertIcon(icon: IconLibraryItem) {
        iconLibraryDao.insertIcon(icon)
        
        // 同步到远端
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncIconLibraryItemToRemote(icon)
                } catch (e: Exception) {
                    Log.e("IconLibraryRepository", "同步图标到远端失败", e)
                }
            }
        }
    }
    
    /**
     * 更新图标
     */
    suspend fun updateIcon(icon: IconLibraryItem) {
        val updatedIcon = icon.copy(updatedAt = System.currentTimeMillis())
        iconLibraryDao.updateIcon(updatedIcon)
        
        // 同步到远端
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncIconLibraryItemToRemote(updatedIcon)
                } catch (e: Exception) {
                    Log.e("IconLibraryRepository", "同步图标到远端失败", e)
                }
            }
        }
    }
    
    /**
     * 删除图标（同时删除文件）
     */
    suspend fun deleteIcon(icon: IconLibraryItem) {
        // 删除数据库记录
        iconLibraryDao.deleteIcon(icon)
        
        // 删除文件
        try {
            val file = File(icon.imagePath)
            if (file.exists()) {
                file.delete()
            }
            
            // 同时删除裁剪版本
            val croppedPath = ImageUtils.getCroppedImagePath(icon.imagePath)
            if (croppedPath != null) {
                val croppedFile = File(croppedPath)
                if (croppedFile.exists()) {
                    croppedFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 同步删除到远端
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.deleteIconLibraryItemFromRemote(icon.uuid)
                } catch (e: Exception) {
                    Log.e("IconLibraryRepository", "同步删除图标到远端失败", e)
                }
            }
        }
    }
    
    /**
     * 根据UUID删除图标
     */
    suspend fun deleteIconByUuid(uuid: String) {
        val icon = getIconByUuid(uuid)
        if (icon != null) {
            deleteIcon(icon)
        }
    }
    
    /**
     * 获取图标总数
     */
    suspend fun getIconCount(): Int = iconLibraryDao.getIconCount()
}
