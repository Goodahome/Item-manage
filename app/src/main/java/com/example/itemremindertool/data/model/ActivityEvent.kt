package com.example.itemremindertool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * 动态事件类型
 */
enum class ActivityEventType {
    ITEM_ADDED,          // 物品添加
    ITEM_DELETED,        // 物品删除
    ITEM_UPDATED,        // 物品更新
    ITEM_USED,           // 使用了物品
    ITEM_VIEWED,         // 查看了物品详情
    WAREHOUSE_ADDED,    // 容器添加
    WAREHOUSE_DELETED,   // 容器删除
    WAREHOUSE_UPDATED,   // 容器更新
    REMINDER_TRIGGERED,  // 提醒触发
    ITEM_EXPIRING,       // 物品即将过期
    ITEM_LOW_STOCK       // 物品库存不足
}

/**
 * 动态事件实体
 */
@Entity(tableName = "activity_events")
data class ActivityEvent(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val type: ActivityEventType,
    val title: String,                  // 事件标题
    val description: String = "",       // 事件描述
    val targetUuid: String? = null,     // 目标 UUID（物品或容器）
    val targetName: String = "",        // 目标名称
    val iconType: String = "",          // 图标类型（用于显示）
    val createdAt: Date = Date(),       // 创建时间
    val metadata: String = ""           // 额外元数据（JSON格式）
)
