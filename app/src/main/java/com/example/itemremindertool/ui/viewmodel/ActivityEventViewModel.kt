package com.example.itemremindertool.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.ActivityEvent
import com.example.itemremindertool.data.model.ActivityEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Date

class ActivityEventViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val activityEventDao = database.activityEventDao()
    
    // 获取最近的动态（默认50条）
    val recentEvents: Flow<List<ActivityEvent>> = activityEventDao.getRecentEvents(50)
    
    // 获取所有动态
    val allEvents: Flow<List<ActivityEvent>> = activityEventDao.getAllEvents()

    fun getEventsByType(type: ActivityEventType): Flow<List<ActivityEvent>> {
        return activityEventDao.getEventsByType(type)
    }
    
    /**
     * 记录物品添加动态
     */
    fun logItemAdded(itemUuid: String, itemName: String) {
        viewModelScope.launch {
            val item = database.itemDao().getItemByUuid(itemUuid)
            val event = ActivityEvent(
                type = ActivityEventType.ITEM_ADDED,
                title = getApplication<android.app.Application>().getString(com.example.itemremindertool.R.string.event_added_item),
                description = itemName,
                targetUuid = item?.uuid,
                targetName = itemName,
                iconType = "add_item",
                createdAt = Date()
            )
            activityEventDao.insert(event)
        }
    }
    
    /**
     * 记录物品删除动态
     */
    fun logItemDeleted(itemUuid: String, itemName: String) {
        viewModelScope.launch {
            val item = database.itemDao().getItemByUuid(itemUuid)
            val event = ActivityEvent(
                type = ActivityEventType.ITEM_DELETED,
                title = getApplication<android.app.Application>().getString(com.example.itemremindertool.R.string.event_deleted_item),
                description = itemName,
                targetUuid = item?.uuid,
                targetName = itemName,
                iconType = "delete_item",
                createdAt = Date()
            )
            activityEventDao.insert(event)
        }
    }
    
    /**
     * 记录物品更新动态
     */
    fun logItemUpdated(itemUuid: String, itemName: String, updateInfo: String = "") {
        viewModelScope.launch {
            val item = database.itemDao().getItemByUuid(itemUuid)
            val event = ActivityEvent(
                type = ActivityEventType.ITEM_UPDATED,
                title = getApplication<android.app.Application>().getString(com.example.itemremindertool.R.string.event_updated_item),
                description = if (updateInfo.isNotEmpty()) "$itemName - $updateInfo" else itemName,
                targetUuid = item?.uuid,
                targetName = itemName,
                iconType = "update_item",
                createdAt = Date()
            )
            activityEventDao.insert(event)
        }
    }
    
    /**
     * 记录使用物品动态
     */
    fun logItemUsed(itemUuid: String, itemName: String, usedQuantity: Int) {
        viewModelScope.launch {
            val item = database.itemDao().getItemByUuid(itemUuid)
            val event = ActivityEvent(
                type = ActivityEventType.ITEM_USED,
                title = getApplication<android.app.Application>().getString(com.example.itemremindertool.R.string.event_used_item),
                description = "$itemName × $usedQuantity",
                targetUuid = item?.uuid,
                targetName = itemName,
                iconType = "use_item",
                createdAt = Date()
            )
            activityEventDao.insert(event)
        }
    }

    /**
     * 记录查看物品详情动态
     */
    fun logItemViewed(itemUuid: String, itemName: String) {
        viewModelScope.launch {
            val item = database.itemDao().getItemByUuid(itemUuid)
            val event = ActivityEvent(
                type = ActivityEventType.ITEM_VIEWED,
                title = getApplication<android.app.Application>().getString(com.example.itemremindertool.R.string.event_viewed_item),
                description = itemName,
                targetUuid = item?.uuid,
                targetName = itemName,
                iconType = "view_item",
                createdAt = Date()
            )
            activityEventDao.insert(event)
        }
    }
    
    /**
     * 记录容器添加动态
     */
    fun logWarehouseAdded(warehouseUuid: String, warehouseName: String) {
        viewModelScope.launch {
            val warehouse = database.warehouseDao().getWarehouseByUuid(warehouseUuid)
            val event = ActivityEvent(
                type = ActivityEventType.WAREHOUSE_ADDED,
                title = getApplication<android.app.Application>().getString(com.example.itemremindertool.R.string.event_created_warehouse),
                description = warehouseName,
                targetUuid = warehouse?.uuid,
                targetName = warehouseName,
                iconType = "add_warehouse",
                createdAt = Date()
            )
            activityEventDao.insert(event)
        }
    }
    
    /**
     * 记录容器删除动态
     */
    fun logWarehouseDeleted(warehouseUuid: String, warehouseName: String) {
        viewModelScope.launch {
            val warehouse = database.warehouseDao().getWarehouseByUuid(warehouseUuid)
            val event = ActivityEvent(
                type = ActivityEventType.WAREHOUSE_DELETED,
                title = getApplication<android.app.Application>().getString(com.example.itemremindertool.R.string.event_deleted_warehouse),
                description = warehouseName,
                targetUuid = warehouse?.uuid,
                targetName = warehouseName,
                iconType = "delete_warehouse",
                createdAt = Date()
            )
            activityEventDao.insert(event)
        }
    }
    
    /**
     * 记录容器更新动态
     */
    fun logWarehouseUpdated(warehouseUuid: String, warehouseName: String) {
        viewModelScope.launch {
            val warehouse = database.warehouseDao().getWarehouseByUuid(warehouseUuid)
            val event = ActivityEvent(
                type = ActivityEventType.WAREHOUSE_UPDATED,
                title = getApplication<android.app.Application>().getString(com.example.itemremindertool.R.string.event_updated_warehouse),
                description = warehouseName,
                targetUuid = warehouse?.uuid,
                targetName = warehouseName,
                iconType = "update_warehouse",
                createdAt = Date()
            )
            activityEventDao.insert(event)
        }
    }
    
    /**
     * 记录提醒触发动态
     */
    fun logReminderTriggered(itemUuid: String, itemName: String, reminderReason: String) {
        viewModelScope.launch {
            val item = database.itemDao().getItemByUuid(itemUuid)
            val event = ActivityEvent(
                type = ActivityEventType.REMINDER_TRIGGERED,
                title = getApplication<android.app.Application>().getString(com.example.itemremindertool.R.string.event_reminder),
                description = "$itemName - $reminderReason",
                targetUuid = item?.uuid,
                targetName = itemName,
                iconType = "reminder",
                createdAt = Date()
            )
            activityEventDao.insert(event)
        }
    }
    
    /**
     * 记录物品即将过期动态
     */
    fun logItemExpiring(itemUuid: String, itemName: String, daysUntilExpiry: Int) {
        viewModelScope.launch {
            val item = database.itemDao().getItemByUuid(itemUuid)
            val event = ActivityEvent(
                type = ActivityEventType.ITEM_EXPIRING,
                title = getApplication<android.app.Application>().getString(com.example.itemremindertool.R.string.event_expiring_soon),
                description = getApplication<android.app.Application>().getString(
                    com.example.itemremindertool.R.string.item_days_until_expiry_description,
                    itemName,
                    daysUntilExpiry
                ),
                targetUuid = item?.uuid,
                targetName = itemName,
                iconType = "expiring",
                createdAt = Date()
            )
            activityEventDao.insert(event)
        }
    }
    
    /**
     * 记录物品库存不足动态
     */
    fun logItemLowStock(itemUuid: String, itemName: String, currentQuantity: Int) {
        viewModelScope.launch {
            val item = database.itemDao().getItemByUuid(itemUuid)
            val event = ActivityEvent(
                type = ActivityEventType.ITEM_LOW_STOCK,
                title = getApplication<android.app.Application>().getString(com.example.itemremindertool.R.string.event_low_stock),
                description = getApplication<android.app.Application>().getString(
                    com.example.itemremindertool.R.string.item_current_stock_description,
                    itemName,
                    currentQuantity
                ),
                targetUuid = item?.uuid,
                targetName = itemName,
                iconType = "low_stock",
                createdAt = Date()
            )
            activityEventDao.insert(event)
        }
    }
    
    /**
     * 删除指定动态
     */
    fun deleteEvent(event: ActivityEvent) {
        viewModelScope.launch {
            activityEventDao.delete(event)
        }
    }
    
    /**
     * 清空所有动态
     */
    fun clearAllEvents() {
        viewModelScope.launch {
            activityEventDao.deleteAll()
        }
    }
    
    /**
     * 删除指定天数之前的动态
     */
    fun deleteOldEvents(days: Int) {
        viewModelScope.launch {
            val cutoffDate = Date(System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L)
            activityEventDao.deleteOlderThan(cutoffDate.time)
        }
    }
}
