package com.example.itemremindertool.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.itemremindertool.MainActivity
import com.example.itemremindertool.R
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemReminder

object NotificationHelper {
    private const val CHANNEL_ID = "item_reminder_channel"
    private const val NOTIFICATION_ID_BASE = 1000
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.reminder_notification_channel_description)
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 发送提醒通知
     */
    fun sendReminderNotification(
        context: Context,
        reminder: ItemReminder,
        item: Item?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 创建点击通知后打开的 Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // 可以添加额外数据，比如跳转到物品详情页
            putExtra("itemUuid", reminder.itemUuid)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.uuid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 构建通知
        val itemName = item?.name ?: context.getString(R.string.unknown_item)
        val reminderReason = if (reminder.reason.isNotBlank()) {
            reminder.reason
        } else {
            when (reminder.reminderType) {
                com.example.itemremindertool.data.model.ReminderType.ONCE ->
                    context.getString(R.string.reminder_type_once_display)
                com.example.itemremindertool.data.model.ReminderType.DAILY ->
                    context.getString(R.string.reminder_type_daily_display)
                com.example.itemremindertool.data.model.ReminderType.MONTHLY ->
                    context.getString(R.string.reminder_type_monthly_display)
                com.example.itemremindertool.data.model.ReminderType.YEARLY ->
                    context.getString(R.string.reminder_type_yearly_display)
            }
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.reminder_notification_title, itemName))
            .setContentText(reminderReason)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(reminderReason))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // 发送通知
        notificationManager.notify(
            NOTIFICATION_ID_BASE + reminder.uuid.hashCode(),
            notification
        )
    }
    
    /**
     * 取消通知
     */
    fun cancelNotification(context: Context, reminderId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel((NOTIFICATION_ID_BASE + reminderId).toInt())
    }
}

