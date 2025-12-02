package com.example.itemremindertool.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.itemremindertool.data.converters.StringListConverters
import java.util.Date

@Entity(tableName = "items")
@TypeConverters(StringListConverters::class)
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val categoryId: Long? = null,
    val warehouseId: Long? = null,
    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),
    val purchaseDate: Date? = null,
    val expiryDate: Date? = null,
    val price: Double? = null,
    val quantity: Int = 1,
    val barcode: String? = null,
    val imageUri: String? = null,
    val featureCode: String? = null, // 特征码（特征向量的字符串表示）
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val status: ItemStatus
        get() {
            return if (expiryDate != null && expiryDate.before(Date())) {
                ItemStatus.EXPIRED
            } else {
                ItemStatus.NORMAL
            }
        }
}

enum class ItemStatus {
    NORMAL,      // 正常
    DAMAGED,     // 损坏
    LOST,        // 遗失
    EXPIRED      // 过期
}

