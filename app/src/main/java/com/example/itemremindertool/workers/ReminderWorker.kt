package com.example.itemremindertool.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.data.model.ReminderType
import com.example.itemremindertool.sync.SyncManager
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
            val reminderId = inputData.getString("reminderId")
            if (reminderId.isNullOrEmpty()) {
                return@withContext Result.failure()
            }
            
            val reminderDao = AppDatabase.getDatabase(applicationContext).itemReminderDao()
            val itemDao = AppDatabase.getDatabase(applicationContext).itemDao()
            
            // 获取提醒信息
            val reminder = reminderDao.getReminderByUuid(reminderId) ?: return@withContext Result.failure()
            
            // 检查提醒是否仍然启用
            if (!reminder.isEnabled) {
                return@withContext Result.success()
            }
            
            // 获取关联的物品信息
            val item = itemDao.getItemByUuid(reminder.itemUuid)
            
            // 发送通知
            NotificationHelper.sendReminderNotification(
                applicationContext,
                reminder,
                item
            )
            
            if (reminder.reminderType == ReminderType.ONCE) {
                // 单次提醒触发后自动删除
                reminderDao.deleteReminder(reminder)
                ReminderScheduler.cancelReminder(applicationContext, reminder.uuid)
                try {
                    SyncManager.getInstance(applicationContext).deleteReminderFromRemote(reminder.uuid)
                } catch (e: Exception) {
                    android.util.Log.e("ReminderWorker", "同步删除单次提醒失败", e)
                }
            } else {
                // 循环提醒需要重新调度下一次提醒
                ReminderScheduler.scheduleReminder(applicationContext, reminder)
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

