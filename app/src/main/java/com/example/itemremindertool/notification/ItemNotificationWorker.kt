package com.example.itemremindertool.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.itemremindertool.MainActivity
import com.example.itemremindertool.data.database.AppDatabase
import kotlinx.coroutines.flow.first

class ItemNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val itemDao = database.itemDao()
        val alertSettingsManager = com.example.itemremindertool.data.AlertSettingsManager(applicationContext)

        // 检查是否启用了系统通知
        if (!alertSettingsManager.getSystemNotificationEnabled()) {
            return Result.success()
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道
        createNotificationChannel(notificationManager)

        val currentTime = System.currentTimeMillis()
        val expiryReminderDays = alertSettingsManager.getExpiryReminderDays()
        val lowStockThreshold = alertSettingsManager.getLowStockThreshold()
        
        // 计算提醒结束时间
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, expiryReminderDays)
        val reminderEndTime = calendar.timeInMillis

        // 获取所有物品
        val allItems = itemDao.getAllItems().first()
        
        // 检查即将过期的物品
        val expiringItems = allItems.filter { item ->
            item.expiryDate != null &&
            item.expiryDate.time >= currentTime &&
            item.expiryDate.time <= reminderEndTime
        }

        // 检查低库存的物品
        val lowStockItems = allItems.filter { item ->
            item.enableStockAlert && item.quantity <= lowStockThreshold
        }

        // 发送即将过期物品通知
        if (expiringItems.isNotEmpty()) {
            sendNotification(
                context = applicationContext,
                notificationManager = notificationManager,
                title = applicationContext.getString(com.example.itemremindertool.R.string.event_expiring_soon),
                message = "有 ${expiringItems.size} 个物品将在 $expiryReminderDays 天内过期",
                id = NOTIFICATION_ID_EXPIRING
            )
        }

        // 发送低库存物品通知
        if (lowStockItems.isNotEmpty()) {
            sendNotification(
                context = applicationContext,
                notificationManager = notificationManager,
                title = applicationContext.getString(com.example.itemremindertool.R.string.event_low_stock),
                message = "有 ${lowStockItems.size} 个物品库存低于阈值",
                id = NOTIFICATION_ID_LOW_STOCK
            )
        }

        return Result.success()
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "物品状态提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "物品状态变化提醒"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(
        context: Context,
        notificationManager: NotificationManager,
        title: String,
        message: String,
        id: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "item_reminder_channel"
        const val NOTIFICATION_ID_EXPIRING = 1
        const val NOTIFICATION_ID_LOW_STOCK = 2
    }
}

