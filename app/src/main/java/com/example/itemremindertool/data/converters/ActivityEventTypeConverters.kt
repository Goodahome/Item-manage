package com.example.itemremindertool.data.converters

import androidx.room.TypeConverter
import com.example.itemremindertool.data.model.ActivityEventType

class ActivityEventTypeConverters {
    @TypeConverter
    fun fromActivityEventType(type: ActivityEventType): String {
        return type.name
    }

    @TypeConverter
    fun toActivityEventType(value: String): ActivityEventType {
        return ActivityEventType.valueOf(value)
    }
}
