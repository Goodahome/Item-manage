package com.example.itemremindertool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 同步队列项
 * 用于存储失败的同步任务，待网络恢复后重试
 */
@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val entityType: String, // "item", "category", "warehouse", "shopping_item"
    val entityUuid: String, // 实体的 UUID
    val operation: SyncOperation, // 操作类型：CREATE, UPDATE, DELETE
    val entityJson: String, // 实体的 JSON 表示（用于重试）
    val retryCount: Int = 0, // 重试次数
    val maxRetries: Int = 5, // 最大重试次数
    val lastAttemptAt: Date? = null, // 最后一次尝试时间
    val createdAt: Date = Date()
)

/**
 * 同步操作类型
 */
enum class SyncOperation {
    CREATE, // 创建/插入
    UPDATE, // 更新
    DELETE  // 删除
}
