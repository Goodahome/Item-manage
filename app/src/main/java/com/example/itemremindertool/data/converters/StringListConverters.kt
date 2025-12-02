package com.example.itemremindertool.data.converters

import androidx.room.TypeConverter

class StringListConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return if (value.isNullOrEmpty()) {
            emptyList()
        } else {
            value.split(",").filter { it.isNotEmpty() }
        }
    }
}
