package com.example.itemremindertool.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.utils.NotificationHelper
import com.example.itemremindertool.utils.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * WorkManager Worker 用于处理提醒触发
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val reminderId = inputData.getLong("reminderId", -1)
            if (reminderId == -1L) {
                return@withContext Result.failure()
            }
            
            val reminderDao = AppDatabase.getDatabase(applicationContext).itemReminderDao()
            val itemDao = AppDatabase.getDatabase(applicationContext).itemDao()
            
            // 获取提醒信息
            val reminder = reminderDao.getReminderById(reminderId) ?: return@withContext Result.failure()
            
            // 检查提醒是否仍然启用
            if (!reminder.isEnabled) {
                return@withContext Result.success()
            }
            
            // 获取关联的物品信息
            val item = itemDao.getItemById(reminder.itemId)
            
            // 发送通知
            NotificationHelper.sendReminderNotification(
                applicationContext,
                reminder,
                item
            )
            
            // 如果是循环提醒，需要重新调度下一次提醒
            if (reminder.reminderType != com.example.itemremindertool.data.model.ReminderType.ONCE) {
                ReminderScheduler.scheduleReminder(applicationContext, reminder)
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

