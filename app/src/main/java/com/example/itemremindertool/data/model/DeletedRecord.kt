package com.example.itemremindertool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 删除记录实体
 * 用于记录被删除的数据，以便在同步时不会恢复已删除的数据
 */
@Entity(tableName = "deleted_records")
data class DeletedRecord(
    @PrimaryKey
    val id: Long = 0,
    val entityType: String, // "item", "warehouse", "category", "shopping_item", "reminder"
    val entityId: Long, // 被删除实体的ID
    val deletedAt: Date = Date() // 删除时间
)

