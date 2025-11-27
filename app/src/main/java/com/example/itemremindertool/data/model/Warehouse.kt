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
    val capacity: Int? = null // 容量限制，null表示无限制
)

