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
import com.example.itemremindertool.R
import com.example.itemremindertool.data.AlertSettingsManager
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.ActivityEventType
import kotlinx.coroutines.flow.first

class ItemNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val itemDao = database.itemDao()
        val activityEventDao = database.activityEventDao()
        val alertSettingsManager = AlertSettingsManager(applicationContext)

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
        val zone = java.time.ZoneId.systemDefault()
        val today = java.time.Instant.ofEpochMilli(currentTime).atZone(zone).toLocalDate()
        val endDate = today.plusDays(expiryReminderDays.toLong())

        // 获取所有物品
        val allItems = itemDao.getAllItems().first()
        
        // 检查即将过期的物品
        val expiringItems = allItems.filter { item ->
            val expiryDate = item.expiryDate ?: return@filter false
            val expiryLocalDate = java.time.Instant.ofEpochMilli(expiryDate.time)
                .atZone(zone)
                .toLocalDate()
            !expiryLocalDate.isBefore(today) && !expiryLocalDate.isAfter(endDate)
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
                title = applicationContext.getString(R.string.event_expiring_soon),
                message = "有 ${expiringItems.size} 个物品将在 $expiryReminderDays 天内过期",
                id = NOTIFICATION_ID_EXPIRING
            )
        }

        // 发送低库存物品通知
        if (lowStockItems.isNotEmpty()) {
            sendNotification(
                context = applicationContext,
                notificationManager = notificationManager,
                title = applicationContext.getString(R.string.event_low_stock),
                message = "有 ${lowStockItems.size} 个物品库存低于阈值",
                id = NOTIFICATION_ID_LOW_STOCK
            )
        }

        // 防遗忘提醒（汇总通知 + 轮转）
        if (alertSettingsManager.isForgetProtectionEnabled()) {
            val inactiveDays = alertSettingsManager.getForgetProtectionInactiveDays()
            val cutoffInactive = today.minusDays(inactiveDays.toLong())
            val usedEvents = activityEventDao.getEventsByType(ActivityEventType.ITEM_USED).first()
            val viewedEvents = activityEventDao.getEventsByType(ActivityEventType.ITEM_VIEWED).first()

            val lastUsedByItem = mutableMapOf<String, java.util.Date>()
            usedEvents.forEach { event ->
                val targetUuid = event.targetUuid ?: return@forEach
                val current = lastUsedByItem[targetUuid]
                if (current == null || event.createdAt.after(current)) {
                    lastUsedByItem[targetUuid] = event.createdAt
                }
            }

            val lastViewedByItem = mutableMapOf<String, java.util.Date>()
            viewedEvents.forEach { event ->
                val targetUuid = event.targetUuid ?: return@forEach
                val current = lastViewedByItem[targetUuid]
                if (current == null || event.createdAt.after(current)) {
                    lastViewedByItem[targetUuid] = event.createdAt
                }
            }

            val forgetCandidates = allItems.filter { item ->
                val lastUsedAt = lastUsedByItem[item.uuid] ?: item.updatedAt
                val lastViewedAt = lastViewedByItem[item.uuid] ?: item.createdAt

                val lastUsedDate = java.time.Instant.ofEpochMilli(lastUsedAt.time)
                    .atZone(zone)
                    .toLocalDate()
                val lastViewedDate = java.time.Instant.ofEpochMilli(lastViewedAt.time)
                    .atZone(zone)
                    .toLocalDate()

                !lastUsedDate.isAfter(cutoffInactive) && !lastViewedDate.isAfter(cutoffInactive)
            }.sortedBy { it.updatedAt.time }

            if (forgetCandidates.isNotEmpty()) {
                val statePrefs = applicationContext.getSharedPreferences("forget_reminder_state", Context.MODE_PRIVATE)
                val todayKey = today.toString()
                val lastRunDate = statePrefs.getString(KEY_FORGET_LAST_RUN_DATE, null)

                // 同一天只发一次
                if (lastRunDate != todayKey) {
                    val dailyLimit = alertSettingsManager.getForgetProtectionDailyLimit()
                        .coerceAtLeast(1)
                        .coerceAtMost(50)
                    val startIndex = (statePrefs.getInt(KEY_FORGET_ROTATION_INDEX, 0))
                        .mod(forgetCandidates.size)

                    val selected = mutableListOf<com.example.itemremindertool.data.model.Item>()
                    var index = startIndex
                    repeat(minOf(dailyLimit, forgetCandidates.size)) {
                        selected.add(forgetCandidates[index])
                        index = (index + 1) % forgetCandidates.size
                    }

                    sendForgetSummaryNotification(
                        context = applicationContext,
                        notificationManager = notificationManager,
                        totalCount = forgetCandidates.size,
                        items = selected
                    )

                    statePrefs.edit()
                        .putString(KEY_FORGET_LAST_RUN_DATE, todayKey)
                        .putInt(KEY_FORGET_ROTATION_INDEX, index)
                        .apply()
                }
            }
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

    private fun sendForgetSummaryNotification(
        context: Context,
        notificationManager: NotificationManager,
        totalCount: Int,
        items: List<com.example.itemremindertool.data.model.Item>
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_forget_reminder", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.forget_reminder_title)
        val message = context.getString(R.string.forget_reminder_summary, totalCount)

        val inbox = NotificationCompat.InboxStyle()
            .setSummaryText(context.getString(R.string.forget_reminder_summary_hint))
        items.forEach { item ->
            inbox.addLine(item.name)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(inbox)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_FORGET, notification)
    }

    companion object {
        const val CHANNEL_ID = "item_reminder_channel"
        const val NOTIFICATION_ID_EXPIRING = 1
        const val NOTIFICATION_ID_LOW_STOCK = 2
        const val NOTIFICATION_ID_FORGET = 3
        private const val KEY_FORGET_ROTATION_INDEX = "forget_rotation_index"
        private const val KEY_FORGET_LAST_RUN_DATE = "forget_last_run_date"
    }
}

