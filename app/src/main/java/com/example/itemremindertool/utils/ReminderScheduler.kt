package com.example.itemremindertool.utils

import android.content.Context
import androidx.work.*
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.data.model.ReminderType
import com.example.itemremindertool.workers.ReminderWorker
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME_PREFIX = "reminder_"
    
    /**
     * 调度提醒
     */
    fun scheduleReminder(context: Context, reminder: ItemReminder) {
        if (!reminder.isEnabled) {
            cancelReminder(context, reminder.id)
            return
        }
        
        val workManager = WorkManager.getInstance(context)
        val workName = "${WORK_NAME_PREFIX}${reminder.id}"
        
        // 计算下次触发时间
        val delayMillis = calculateNextTriggerTime(reminder)
        if (delayMillis <= 0) {
            // 如果时间已过，取消提醒
            cancelReminder(context, reminder.id)
            return
        }
        
        // 创建输入数据
        val inputData = Data.Builder()
            .putLong("reminderId", reminder.id)
            .build()
        
        // 创建一次性工作请求
        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(workName)
            .build()
        
        // 调度工作
        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * 取消提醒
     */
    fun cancelReminder(context: Context, reminderId: Long) {
        val workManager = WorkManager.getInstance(context)
        val workName = "${WORK_NAME_PREFIX}${reminderId}"
        workManager.cancelUniqueWork(workName)
    }
    
    /**
     * 取消所有提醒
     */
    fun cancelAllReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(WORK_NAME_PREFIX)
    }
    
    /**
     * 重新调度所有活跃的提醒
     */
    suspend fun rescheduleAllReminders(context: Context) {
        val reminderDao = com.example.itemremindertool.data.database.AppDatabase
            .getDatabase(context)
            .itemReminderDao()
        
        val activeReminders = reminderDao.getAllActiveReminders()
        
        // 使用 Flow 获取第一个值（当前状态）
        activeReminders.first().let { reminders ->
            reminders.forEach { reminder ->
                scheduleReminder(context, reminder)
            }
        }
    }
    
    /**
     * 计算下次触发时间（毫秒）
     */
    private fun calculateNextTriggerTime(reminder: ItemReminder): Long {
        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance()
        
        when (reminder.reminderType) {
            ReminderType.ONCE -> {
                // 一次性提醒：使用 reminderTime
                reminder.reminderTime?.let {
                    targetTime.time = it
                    val diff = targetTime.timeInMillis - now.timeInMillis
                    return if (diff > 0) diff else 0
                } ?: return 0
            }
            
            ReminderType.DAILY -> {
                // 每日提醒：使用 dailyTime (格式: "HH:mm")
                reminder.dailyTime?.let { timeStr ->
                    val parts = timeStr.split(":")
                    if (parts.size == 2) {
                        val hour = parts[0].toIntOrNull() ?: 9
                        val minute = parts[1].toIntOrNull() ?: 0
                        
                        targetTime.set(Calendar.HOUR_OF_DAY, hour)
                        targetTime.set(Calendar.MINUTE, minute)
                        targetTime.set(Calendar.SECOND, 0)
                        targetTime.set(Calendar.MILLISECOND, 0)
                        
                        // 如果今天的时间已过，设置为明天
                        if (targetTime.before(now)) {
                            targetTime.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        
                        return targetTime.timeInMillis - now.timeInMillis
                    }
                }
                return 0
            }
            
            ReminderType.MONTHLY -> {
                // 每月提醒：使用 monthlyDay 和 monthlyTime
                reminder.monthlyDay?.let { day ->
                    reminder.monthlyTime?.let { timeStr ->
                        val parts = timeStr.split(":")
                        if (parts.size == 2) {
                            val hour = parts[0].toIntOrNull() ?: 9
                            val minute = parts[1].toIntOrNull() ?: 0
                            
                            targetTime.set(Calendar.DAY_OF_MONTH, day)
                            targetTime.set(Calendar.HOUR_OF_DAY, hour)
                            targetTime.set(Calendar.MINUTE, minute)
                            targetTime.set(Calendar.SECOND, 0)
                            targetTime.set(Calendar.MILLISECOND, 0)
                            
                            // 如果本月的时间已过，设置为下个月
                            if (targetTime.before(now)) {
                                targetTime.add(Calendar.MONTH, 1)
                                // 处理月份天数不一致的情况
                                val maxDay = targetTime.getActualMaximum(Calendar.DAY_OF_MONTH)
                                if (day > maxDay) {
                                    targetTime.set(Calendar.DAY_OF_MONTH, maxDay)
                                }
                            }
                            
                            return targetTime.timeInMillis - now.timeInMillis
                        }
                    }
                }
                return 0
            }
            
            ReminderType.YEARLY -> {
                // 每年提醒：使用 yearlyMonth, yearlyDay 和 yearlyTime
                reminder.yearlyMonth?.let { month ->
                    reminder.yearlyDay?.let { day ->
                        reminder.yearlyTime?.let { timeStr ->
                            val parts = timeStr.split(":")
                            if (parts.size == 2) {
                                val hour = parts[0].toIntOrNull() ?: 9
                                val minute = parts[1].toIntOrNull() ?: 0
                                
                                targetTime.set(Calendar.MONTH, month - 1) // Calendar.MONTH 是 0-based
                                targetTime.set(Calendar.DAY_OF_MONTH, day)
                                targetTime.set(Calendar.HOUR_OF_DAY, hour)
                                targetTime.set(Calendar.MINUTE, minute)
                                targetTime.set(Calendar.SECOND, 0)
                                targetTime.set(Calendar.MILLISECOND, 0)
                                
                                // 如果今年的时间已过，设置为明年
                                if (targetTime.before(now)) {
                                    targetTime.add(Calendar.YEAR, 1)
                                }
                                
                                return targetTime.timeInMillis - now.timeInMillis
                            }
                        }
                    }
                }
                return 0
            }
        }
    }
}

