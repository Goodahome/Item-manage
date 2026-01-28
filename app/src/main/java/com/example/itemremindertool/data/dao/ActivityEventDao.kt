package com.example.itemremindertool.data.dao

import androidx.room.*
import com.example.itemremindertool.data.model.ActivityEvent
import com.example.itemremindertool.data.model.ActivityEventType
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEventDao {
    @Query("SELECT * FROM activity_events ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<ActivityEvent>>
    
    @Query("SELECT * FROM activity_events ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 50): Flow<List<ActivityEvent>>
    
    @Query("SELECT * FROM activity_events WHERE type = :type ORDER BY createdAt DESC")
    fun getEventsByType(type: ActivityEventType): Flow<List<ActivityEvent>>
    
    @Query("SELECT * FROM activity_events WHERE targetUuid = :targetUuid ORDER BY createdAt DESC")
    fun getEventsByTarget(targetUuid: String): Flow<List<ActivityEvent>>

    @Query("SELECT * FROM activity_events ORDER BY createdAt DESC")
    suspend fun getAllEventsSync(): List<ActivityEvent>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ActivityEvent): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<ActivityEvent>): Unit
    
    @Delete
    suspend fun delete(event: ActivityEvent): Int
    
    @Query("DELETE FROM activity_events WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String): Int
    
    @Query("DELETE FROM activity_events WHERE createdAt < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: Long): Int
    
    @Query("DELETE FROM activity_events")
    suspend fun deleteAll(): Int
}
