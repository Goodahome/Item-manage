package com.example.itemremindertool.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemremindertool.data.dao.ItemReminderDao
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.DeletedRecord
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.sync.SyncManager
import com.example.itemremindertool.utils.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class ItemReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val itemReminderDao: ItemReminderDao = AppDatabase.getDatabase(application).itemReminderDao()
    private val deletedRecordDao = AppDatabase.getDatabase(application).deletedRecordDao()
    private val context = application.applicationContext
    private val syncManager = SyncManager.getInstance(context)
    
    val allActiveReminders: Flow<List<ItemReminder>> = itemReminderDao.getAllActiveReminders()
    val allReminders: Flow<List<ItemReminder>> = itemReminderDao.getAllReminders()
    
    fun getRemindersByItemId(itemId: String): Flow<List<ItemReminder>> {
        // 已废弃：为保持兼容性保留，但实际调用UUID版本
        return getRemindersByItemUuid(itemId)
    }
    
    fun getRemindersByItemUuid(itemUuid: String): Flow<List<ItemReminder>> {
        return itemReminderDao.getRemindersByItemId(itemUuid)
    }
    
    fun insertReminder(reminder: ItemReminder) {
        viewModelScope.launch {
            val now = Date()
            val toInsert = reminder.copy(
                createdAt = reminder.createdAt.takeIf { it.time > 0 } ?: now,
                updatedAt = now
            )
            itemReminderDao.insertReminder(toInsert)
            // 如果插入成功并且启用了提醒，调度提醒
            if (toInsert.isEnabled) {
                ReminderScheduler.scheduleReminder(context, toInsert)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = Date()
                syncManager.syncReminderToRemote(
                    reminder.copy(updatedAt = now, createdAt = reminder.createdAt.takeIf { it.time > 0 } ?: now)
                )
            } catch (e: Exception) {
                android.util.Log.e("ItemReminderViewModel", "同步提醒到远端失败", e)
            }
        }
    }
    
    fun updateReminder(reminder: ItemReminder) {
        viewModelScope.launch {
            val updated = reminder.copy(updatedAt = Date())
            itemReminderDao.updateReminder(updated)
            // 更新后重新调度提醒
            if (updated.isEnabled) {
                ReminderScheduler.scheduleReminder(context, updated)
            } else {
                // 如果禁用，取消提醒
                ReminderScheduler.cancelReminder(context, updated.uuid)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                syncManager.syncReminderToRemote(
                    reminder.copy(updatedAt = Date())
                )
            } catch (e: Exception) {
                android.util.Log.e("ItemReminderViewModel", "同步提醒到远端失败", e)
            }
        }
    }
    
    fun deleteReminder(reminder: ItemReminder) {
        viewModelScope.launch {
            itemReminderDao.deleteReminder(reminder)
            // 删除后取消提醒
            ReminderScheduler.cancelReminder(context, reminder.uuid)
            deletedRecordDao.insertDeletedRecord(
                DeletedRecord(
                    entityType = "reminder",
                    entityUuid = reminder.uuid,
                    deletedAt = Date()
                )
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                syncManager.deleteReminderFromRemote(reminder.uuid)
            } catch (e: Exception) {
                android.util.Log.e("ItemReminderViewModel", "同步删除提醒到远端失败", e)
            }
        }
    }
    
    fun deleteRemindersByItemId(itemId: String) {
        // 已废弃：为保持兼容性保留，但实际调用UUID版本
        deleteRemindersByItemUuid(itemId)
    }
    
    fun deleteRemindersByItemUuid(itemUuid: String) {
        viewModelScope.launch {
            // 先获取所有提醒，以便取消调度
            val reminders = itemReminderDao.getRemindersByItemId(itemUuid)
            val reminderList = reminders.first()
            reminderList.forEach { reminder ->
                ReminderScheduler.cancelReminder(context, reminder.uuid)
                deletedRecordDao.insertDeletedRecord(
                    DeletedRecord(
                        entityType = "reminder",
                        entityUuid = reminder.uuid,
                        deletedAt = Date()
                    )
                )
            }
            itemReminderDao.deleteRemindersByItemId(itemUuid)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    reminderList.forEach { reminder ->
                        syncManager.deleteReminderFromRemote(reminder.uuid)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ItemReminderViewModel", "同步批量删除提醒到远端失败", e)
                }
            }
        }
    }
}



