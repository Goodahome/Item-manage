package com.example.itemremindertool.data.converters

import androidx.room.TypeConverter
import com.example.itemremindertool.data.model.SyncOperation

class SyncOperationConverters {
    @TypeConverter
    fun fromSyncOperation(operation: SyncOperation): String {
        return operation.name
    }
    
    @TypeConverter
    fun toSyncOperation(value: String): SyncOperation {
        return SyncOperation.valueOf(value)
    }
}
