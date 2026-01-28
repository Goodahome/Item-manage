package com.example.itemremindertool.data.dao

import androidx.room.*
import com.example.itemremindertool.data.model.ItemReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemReminderDao {
    @Query("SELECT * FROM item_reminders WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getAllActiveReminders(): Flow<List<ItemReminder>>
    
    @Query("SELECT * FROM item_reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<ItemReminder>>
    
    @Query("SELECT * FROM item_reminders ORDER BY createdAt DESC")
    suspend fun getAllRemindersSync(): List<ItemReminder>
    
    @Query("SELECT * FROM item_reminders WHERE itemUuid = :itemUuid ORDER BY createdAt DESC")
    fun getRemindersByItemId(itemUuid: String): Flow<List<ItemReminder>>

    @Query("SELECT * FROM item_reminders WHERE uuid = :uuid LIMIT 1")
    suspend fun getReminderByUuid(uuid: String): ItemReminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ItemReminder): Long
    
    @Update
    suspend fun updateReminder(reminder: ItemReminder): Int
    
    @Delete
    suspend fun deleteReminder(reminder: ItemReminder): Int
    
    @Query("DELETE FROM item_reminders WHERE itemUuid = :itemUuid")
    suspend fun deleteRemindersByItemId(itemUuid: String): Int
}



