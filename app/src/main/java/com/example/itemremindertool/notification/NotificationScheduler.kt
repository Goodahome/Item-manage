package com.example.itemremindertool.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.itemremindertool.data.AlertSettingsManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val WORK_NAME = "item_notification_work"

    fun scheduleNotifications(context: Context) {
        val alertSettingsManager = AlertSettingsManager(context)
        
        // 取消现有的工作
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        
        // 如果系统通知未启用，不调度
        if (!alertSettingsManager.getSystemNotificationEnabled()) {
            return
        }
        
        // 获取用户设置的通知时间
        val notificationHour = alertSettingsManager.getNotificationHour()
        val notificationMinute = alertSettingsManager.getNotificationMinute()
        
        // 计算到下次通知时间的延迟
        val currentTime = Calendar.getInstance()
        val notificationTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, notificationHour)
            set(Calendar.MINUTE, notificationMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // 如果今天的时间已经过了，设置为明天
            if (before(currentTime)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        val initialDelay = notificationTime.timeInMillis - currentTime.timeInMillis
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ItemNotificationWorker>(
            1, TimeUnit.DAYS // 改为每天执行一次
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE, // 改为REPLACE以应用新的设置
            workRequest
        )
    }

    fun cancelNotifications(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

