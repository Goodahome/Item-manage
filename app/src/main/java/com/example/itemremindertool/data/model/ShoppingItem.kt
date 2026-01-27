package com.example.itemremindertool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val quantity: Int = 1,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val createdAt: Date = Date(),
    val completedAt: Date? = null,
    val imageUri: String? = null, // 从物品添加时的图片URI
    val imageKey: String? = null, // 远端图片对象存储 Key
    val itemId: Long? = null, // 关联的物品ID（可选，用于完成购买时补充库存）
    val isSample: Boolean = false // 示例数据标记（不参与云端同步）
)

enum class Priority {
    LOW,      // 低
    MEDIUM,   // 中
    HIGH      // 高
}

