package com.example.itemremindertool.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WarehouseAccess(
    val warehouseUuid: String,
    val timestamp: Long
)

class AccessHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("access_history", Context.MODE_PRIVATE)
    private val KEY_PREFIX = "warehouse_access_"
    private val MAX_HISTORY_SIZE = 10

    private val _accessHistory = MutableStateFlow<List<WarehouseAccess>>(loadHistory())
    val accessHistory: StateFlow<List<WarehouseAccess>> = _accessHistory.asStateFlow()

    private fun loadHistory(): List<WarehouseAccess> {
        val history = mutableListOf<WarehouseAccess>()
        for ((key, value) in prefs.all) {
            if (key.startsWith(KEY_PREFIX)) {
                val uuid = key.removePrefix(KEY_PREFIX).takeIf { it.isNotBlank() } ?: continue
                val timestamp = (value as? Long) ?: continue
                history.add(WarehouseAccess(uuid, timestamp))
            }
        }
        return history.sortedByDescending { it.timestamp }
    }

    fun recordAccess(warehouseUuid: String) {
        val currentTime = System.currentTimeMillis()
        val editor = prefs.edit()
        editor.putLong("$KEY_PREFIX$warehouseUuid", currentTime)
        val currentHistory = loadHistory()
        if (currentHistory.size >= MAX_HISTORY_SIZE) {
            val oldest = currentHistory.minByOrNull { it.timestamp }
            oldest?.let { editor.remove("$KEY_PREFIX${it.warehouseUuid}") }
        }
        editor.apply()
        _accessHistory.value = loadHistory()
    }

    fun getRecentWarehouseUuids(limit: Int = 5): List<String> {
        return _accessHistory.value.take(limit).map { it.warehouseUuid }
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

