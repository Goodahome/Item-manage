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

        // 检查过期物品
        val currentTime = System.currentTimeMillis()
        val expiredItems = itemDao.getExpiredItems(currentTime).first()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道
        createNotificationChannel(notificationManager)

        // 发送过期物品通知
        if (expiredItems.isNotEmpty()) {
            sendNotification(
                context = applicationContext,
                notificationManager = notificationManager,
                title = "物品过期提醒",
                message = "有 ${expiredItems.size} 个物品已过期",
                id = NOTIFICATION_ID_EXPIRED
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
        const val NOTIFICATION_ID_EXPIRED = 1
    }
}

