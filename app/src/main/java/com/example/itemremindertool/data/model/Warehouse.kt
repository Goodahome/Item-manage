package com.example.itemremindertool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warehouses")
data class Warehouse(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val location: String = "",
    val capacity: Int? = null, // 容量限制，null表示无限制
    val parentId: Long? = null, // 父容器ID，null表示顶层容器
    val level: Int = 1 // 层级，从1开始（顶层），最大5层
)

