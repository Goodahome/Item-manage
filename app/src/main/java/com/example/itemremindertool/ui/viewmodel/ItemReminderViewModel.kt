package com.example.itemremindertool.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemremindertool.data.dao.ItemReminderDao
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.utils.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch

class ItemReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val itemReminderDao: ItemReminderDao = AppDatabase.getDatabase(application).itemReminderDao()
    private val context = application.applicationContext
    
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
            itemReminderDao.insertReminder(reminder)
            // 如果插入成功并且启用了提醒，调度提醒
            if (reminder.isEnabled) {
                ReminderScheduler.scheduleReminder(context, reminder)
            }
        }
    }
    
    fun updateReminder(reminder: ItemReminder) {
        viewModelScope.launch {
            itemReminderDao.updateReminder(reminder)
            // 更新后重新调度提醒
            if (reminder.isEnabled) {
                ReminderScheduler.scheduleReminder(context, reminder)
            } else {
                // 如果禁用，取消提醒
                ReminderScheduler.cancelReminder(context, reminder.uuid)
            }
        }
    }
    
    fun deleteReminder(reminder: ItemReminder) {
        viewModelScope.launch {
            itemReminderDao.deleteReminder(reminder)
            // 删除后取消提醒
            ReminderScheduler.cancelReminder(context, reminder.uuid)
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
            }
            itemReminderDao.deleteRemindersByItemId(itemUuid)
        }
    }
}



