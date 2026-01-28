package com.example.itemremindertool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val color: String = "#6200EE", // 默认颜色
    val icon: String = "category" // 图标名称
)

