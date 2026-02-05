package com.example.itemremindertool.data.dao

import androidx.room.*
import com.example.itemremindertool.data.model.IconLibraryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface IconLibraryDao {
    /**
     * 获取所有图标（按创建时间倒序）
     */
    @Query("SELECT * FROM icon_library ORDER BY createdAt DESC")
    fun getAllIcons(): Flow<List<IconLibraryItem>>
    
    /**
     * 获取所有图标列表（非Flow）
     */
    @Query("SELECT * FROM icon_library ORDER BY createdAt DESC")
    suspend fun getAllIconsList(): List<IconLibraryItem>
    
    /**
     * 根据UUID获取图标
     */
    @Query("SELECT * FROM icon_library WHERE uuid = :uuid")
    suspend fun getIconByUuid(uuid: String): IconLibraryItem?
    
    /**
     * 插入图标
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIcon(icon: IconLibraryItem)
    
    /**
     * 插入多个图标
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIcons(icons: List<IconLibraryItem>)
    
    /**
     * 更新图标
     */
    @Update
    suspend fun updateIcon(icon: IconLibraryItem)
    
    /**
     * 删除图标
     */
    @Delete
    suspend fun deleteIcon(icon: IconLibraryItem)
    
    /**
     * 根据UUID删除图标
     */
    @Query("DELETE FROM icon_library WHERE uuid = :uuid")
    suspend fun deleteIconByUuid(uuid: String)
    
    /**
     * 删除所有图标
     */
    @Query("DELETE FROM icon_library")
    suspend fun deleteAllIcons()
    
    /**
     * 获取图标总数
     */
    @Query("SELECT COUNT(*) FROM icon_library")
    suspend fun getIconCount(): Int
}
