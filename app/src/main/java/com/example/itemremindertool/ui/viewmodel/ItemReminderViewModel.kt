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
import kotlinx.coroutines.launch

class ItemReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val itemReminderDao: ItemReminderDao = AppDatabase.getDatabase(application).itemReminderDao()
    private val context = application.applicationContext
    
    val allActiveReminders: Flow<List<ItemReminder>> = itemReminderDao.getAllActiveReminders()
    
    fun getRemindersByItemId(itemId: Long): Flow<List<ItemReminder>> {
        return itemReminderDao.getRemindersByItemId(itemId)
    }
    
    fun insertReminder(reminder: ItemReminder) {
        viewModelScope.launch {
            val reminderId = itemReminderDao.insertReminder(reminder)
            // 如果插入成功，调度提醒
            if (reminderId > 0 && reminder.isEnabled) {
                val insertedReminder = reminder.copy(id = reminderId)
                ReminderScheduler.scheduleReminder(context, insertedReminder)
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
                ReminderScheduler.cancelReminder(context, reminder.id)
            }
        }
    }
    
    fun deleteReminder(reminder: ItemReminder) {
        viewModelScope.launch {
            itemReminderDao.deleteReminder(reminder)
            // 删除后取消提醒
            ReminderScheduler.cancelReminder(context, reminder.id)
        }
    }
    
    fun deleteRemindersByItemId(itemId: Long) {
        viewModelScope.launch {
            // 先获取所有提醒，以便取消调度
            val reminders = itemReminderDao.getRemindersByItemId(itemId)
            val reminderList = reminders.first()
            reminderList.forEach { reminder ->
                ReminderScheduler.cancelReminder(context, reminder.id)
            }
            itemReminderDao.deleteRemindersByItemId(itemId)
        }
    }
}



