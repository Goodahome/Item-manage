package com.example.itemremindertool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "warehouses")
data class Warehouse(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val location: String = "",
    val capacity: Int? = null, // 容量限制，null表示无限制
    val parentUuid: String? = null, // 父容器 UUID，null 表示顶层容器
    val level: Int = 1, // 层级，从1开始（顶层），最大5层
    val imageUri: String? = null, // 容器图片路径
    val imageKey: String? = null, // 远端图片对象存储 Key
    val createdAt: Date = Date(), // 创建时间戳
    val isSample: Boolean = false, // 示例数据标记（不参与云端同步）
    val itemsSuffix: String? = null, // 容器内物品后缀（空则使用默认）
    val hideUseButton: Boolean = false, // 隐藏“使用”按钮
    val hideDetailsButton: Boolean = false, // 隐藏“详情”按钮
    val hideQuantity: Boolean = false, // 隐藏数量显示
    val hideQuantitySlider: Boolean = false // 隐藏数量滑块与 +/- 控件
)

