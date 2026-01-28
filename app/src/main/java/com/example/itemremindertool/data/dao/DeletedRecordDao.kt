package com.example.itemremindertool.data.dao

import androidx.room.*
import com.example.itemremindertool.data.model.DeletedRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletedRecordDao {
    @Query("SELECT * FROM deleted_records WHERE entityType = :entityType AND entityUuid = :entityUuid")
    suspend fun getDeletedRecord(entityType: String, entityUuid: String): DeletedRecord?
    
    @Query("SELECT * FROM deleted_records WHERE entityType = :entityType")
    suspend fun getDeletedRecordsByType(entityType: String): List<DeletedRecord>
    
    @Query("SELECT * FROM deleted_records")
    suspend fun getAllDeletedRecords(): List<DeletedRecord>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedRecord(record: DeletedRecord): Unit
    
    @Query("DELETE FROM deleted_records WHERE entityType = :entityType AND entityUuid = :entityUuid")
    suspend fun removeDeletedRecord(entityType: String, entityUuid: String): Int
    
    @Query("DELETE FROM deleted_records WHERE deletedAt < :beforeDate")
    suspend fun cleanOldRecords(beforeDate: Long): Int // 清理30天前的删除记录
}

