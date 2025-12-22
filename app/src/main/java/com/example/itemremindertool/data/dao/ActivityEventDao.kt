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
    
    @Query("SELECT * FROM activity_events WHERE targetId = :targetId ORDER BY createdAt DESC")
    fun getEventsByTarget(targetId: Long): Flow<List<ActivityEvent>>
    
    @Insert
    suspend fun insert(event: ActivityEvent): Long
    
    @Insert
    suspend fun insertAll(events: List<ActivityEvent>)
    
    @Delete
    suspend fun delete(event: ActivityEvent)
    
    @Query("DELETE FROM activity_events WHERE id = :eventId")
    suspend fun deleteById(eventId: Long)
    
    @Query("DELETE FROM activity_events WHERE createdAt < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: Long)
    
    @Query("DELETE FROM activity_events")
    suspend fun deleteAll()
}
