package com.example.itemremindertool.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val categoryId: Long? = null,
    val warehouseId: Long? = null,
    val status: ItemStatus = ItemStatus.NORMAL,
    val purchaseDate: Date? = null,
    val expiryDate: Date? = null,
    val price: Double? = null,
    val quantity: Int = 1,
    val barcode: String? = null,
    val imageUri: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class ItemStatus {
    NORMAL,      // 正常
    DAMAGED,     // 损坏
    LOST,        // 遗失
    EXPIRED      // 过期
}

