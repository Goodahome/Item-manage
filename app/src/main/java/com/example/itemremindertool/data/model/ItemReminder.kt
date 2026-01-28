package com.example.itemremindertool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * 物品提醒数据模型
 */
@Entity(tableName = "item_reminders")
data class ItemReminder(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val itemUuid: String,
    
    // 提醒类型
    val reminderType: ReminderType,
    
    // 提醒时间（一次性提醒）
    val reminderTime: Date? = null,
    
    // 循环提醒配置
    val dailyTime: String? = null,  // 每日提醒时间，格式 "HH:mm"
    val monthlyDay: Int? = null,     // 每月提醒日期（1-31）
    val monthlyTime: String? = null, // 每月提醒时间，格式 "HH:mm"
    val yearlyMonth: Int? = null,    // 每年提醒月份（1-12）
    val yearlyDay: Int? = null,      // 每年提醒日期（1-31）
    val yearlyTime: String? = null,  // 每年提醒时间，格式 "HH:mm"
    
    // 提醒原因/备注
    val reason: String = "",
    
    // 是否启用
    val isEnabled: Boolean = true,
    
    // 创建时间
    val createdAt: Date = Date(),
    
    // 更新时间
    val updatedAt: Date = Date()
)

/**
 * 提醒类型枚举
 */
enum class ReminderType {
    ONCE,       // 一次性提醒
    DAILY,      // 每日循环
    MONTHLY,    // 每月循环
    YEARLY      // 每年循环
}



