package com.example.itemremindertool.data.converters

import androidx.room.TypeConverter
import com.example.itemremindertool.data.model.ReminderType

class ReminderTypeConverters {
    @TypeConverter
    fun fromReminderType(type: ReminderType): String {
        return type.name
    }
    
    @TypeConverter
    fun toReminderType(value: String): ReminderType {
        return ReminderType.valueOf(value)
    }
}



