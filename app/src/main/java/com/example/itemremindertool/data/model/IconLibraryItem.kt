package com.example.itemremindertool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 图标库项目实体类
 */
@Entity(tableName = "icon_library")
data class IconLibraryItem(
    @PrimaryKey
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val name: String, // 图标名称
    val imagePath: String, // 图标文件路径
    val fileSize: Long, // 文件大小（字节）
    val createdAt: Long = System.currentTimeMillis() // 创建时间
)
