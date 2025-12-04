package com.example.itemremindertool.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WarehouseAccess(
    val warehouseId: Long,
    val timestamp: Long
)

class AccessHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("access_history", Context.MODE_PRIVATE)
    private val KEY_PREFIX = "warehouse_access_"
    private val MAX_HISTORY_SIZE = 10 // 最多保存10个最近访问的容器
    
    private val _accessHistory = MutableStateFlow<List<WarehouseAccess>>(loadHistory())
    val accessHistory: StateFlow<List<WarehouseAccess>> = _accessHistory.asStateFlow()
    
    private fun loadHistory(): List<WarehouseAccess> {
        val history = mutableListOf<WarehouseAccess>()
        val allEntries = prefs.all
        
        for ((key, value) in allEntries) {
            if (key.startsWith(KEY_PREFIX)) {
                val warehouseId = key.removePrefix(KEY_PREFIX).toLongOrNull()
                val timestamp = (value as? Long) ?: continue
                if (warehouseId != null) {
                    history.add(WarehouseAccess(warehouseId, timestamp))
                }
            }
        }
        
        // 按时间戳降序排序（最新的在前）
        return history.sortedByDescending { it.timestamp }
    }
    
    fun recordAccess(warehouseId: Long) {
        val currentTime = System.currentTimeMillis()
        val editor = prefs.edit()
        
        // 保存新的访问记录
        editor.putLong("$KEY_PREFIX$warehouseId", currentTime)
        
        // 如果历史记录超过最大数量，删除最旧的记录
        val currentHistory = loadHistory()
        if (currentHistory.size >= MAX_HISTORY_SIZE) {
            // 找到最旧的记录并删除
            val oldest = currentHistory.minByOrNull { it.timestamp }
            oldest?.let {
                editor.remove("$KEY_PREFIX${it.warehouseId}")
            }
        }
        
        editor.apply()
        
        // 更新状态流
        _accessHistory.value = loadHistory()
    }
    
    fun getRecentWarehouseIds(limit: Int = 5): List<Long> {
        return _accessHistory.value.take(limit).map { it.warehouseId }
    }
    
    fun clearHistory() {
        val editor = prefs.edit()
        val allEntries = prefs.all
        for ((key, _) in allEntries) {
            if (key.startsWith(KEY_PREFIX)) {
                editor.remove(key)
            }
        }
        editor.apply()
        _accessHistory.value = emptyList()
    }
}

