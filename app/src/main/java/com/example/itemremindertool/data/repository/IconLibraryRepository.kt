package com.example.itemremindertool.data.repository

import android.content.Context
import com.example.itemremindertool.data.dao.IconLibraryDao
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.IconLibraryItem
import com.example.itemremindertool.utils.ImageUtils
import kotlinx.coroutines.flow.Flow
import java.io.File

class IconLibraryRepository(context: Context) {
    private val iconLibraryDao: IconLibraryDao = AppDatabase.getDatabase(context).iconLibraryDao()
    private val appContext = context.applicationContext
    
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
    suspend fun insertIcon(icon: IconLibraryItem) = iconLibraryDao.insertIcon(icon)
    
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
