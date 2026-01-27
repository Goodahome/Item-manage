package com.example.itemremindertool.data.dao

import androidx.room.*
import com.example.itemremindertool.data.model.SyncQueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    /**
     * 获取所有待同步的项
     */
    @Query("SELECT * FROM sync_queue WHERE retryCount < maxRetries ORDER BY createdAt ASC")
    suspend fun getAllPendingItems(): List<SyncQueueItem>
    
    /**
     * 获取待同步项数量
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE retryCount < maxRetries")
    fun getPendingCount(): Flow<Int>
    
    /**
     * 插入同步项
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueItem): Long
    
    /**
     * 更新同步项
     */
    @Update
    suspend fun update(item: SyncQueueItem)
    
    /**
     * 删除同步项
     */
    @Delete
    suspend fun delete(item: SyncQueueItem)
    
    /**
     * 根据实体 UUID 和操作类型查找
     */
    @Query("SELECT * FROM sync_queue WHERE entityUuid = :uuid AND operation = :operation LIMIT 1")
    suspend fun findByUuidAndOperation(uuid: String, operation: com.example.itemremindertool.data.model.SyncOperation): SyncQueueItem?

    /**
     * 删除指定实体 UUID 的所有队列项
     */
    @Query("DELETE FROM sync_queue WHERE entityUuid = :uuid")
    suspend fun deleteByUuid(uuid: String)
    
    /**
     * 清除已超过最大重试次数的项
     */
    @Query("DELETE FROM sync_queue WHERE retryCount >= maxRetries")
    suspend fun clearExceededRetries()
    
    /**
     * 清空队列
     */
    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
