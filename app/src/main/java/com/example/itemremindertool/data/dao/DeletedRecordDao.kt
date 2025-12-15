package com.example.itemremindertool.data.dao

import androidx.room.*
import com.example.itemremindertool.data.model.DeletedRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletedRecordDao {
    @Query("SELECT * FROM deleted_records WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun getDeletedRecord(entityType: String, entityId: Long): DeletedRecord?
    
    @Query("SELECT * FROM deleted_records WHERE entityType = :entityType")
    suspend fun getDeletedRecordsByType(entityType: String): List<DeletedRecord>
    
    @Query("SELECT * FROM deleted_records")
    suspend fun getAllDeletedRecords(): List<DeletedRecord>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedRecord(record: DeletedRecord)
    
    @Query("DELETE FROM deleted_records WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun removeDeletedRecord(entityType: String, entityId: Long)
    
    @Query("DELETE FROM deleted_records WHERE deletedAt < :beforeDate")
    suspend fun cleanOldRecords(beforeDate: Long) // 清理30天前的删除记录
}

