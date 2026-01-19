package com.example.itemremindertool.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 物品展示模式
 */
enum class ItemDisplayMode {
    LIST,  // 列表模式（长方形整行显示）
    GRID   // 网格模式（正方形网格显示，类似游戏背包）
}

/**
 * 物品展示模式管理器
 */
class ItemDisplayModeManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "item_display_settings",
        Context.MODE_PRIVATE
    )
    
    private val _displayMode = MutableStateFlow(getDisplayMode())
    val displayMode: StateFlow<ItemDisplayMode> = _displayMode.asStateFlow()
    
    // 监听SharedPreferences变化，确保配置变更后能正确恢复
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_DISPLAY_MODE) {
            _displayMode.value = getDisplayMode()
        }
    }
    
    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }
    
    companion object {
        private const val KEY_DISPLAY_MODE = "item_display_mode"
        
        @Volatile
        private var instance: ItemDisplayModeManager? = null
        
        fun getInstance(context: Context): ItemDisplayModeManager {
            return instance ?: synchronized(this) {
                instance ?: ItemDisplayModeManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    /**
     * 获取当前显示模式
     */
    fun getDisplayMode(): ItemDisplayMode {
        val modeName = prefs.getString(KEY_DISPLAY_MODE, ItemDisplayMode.LIST.name)
        return try {
            ItemDisplayMode.valueOf(modeName ?: ItemDisplayMode.LIST.name)
        } catch (e: IllegalArgumentException) {
            ItemDisplayMode.LIST
        }
    }
    
    /**
     * 设置显示模式
     */
    fun setDisplayMode(mode: ItemDisplayMode) {
        prefs.edit().putString(KEY_DISPLAY_MODE, mode.name).apply()
        _displayMode.value = mode
    }
    
    /**
     * 切换显示模式
     */
    fun toggleDisplayMode() {
        val newMode = when (_displayMode.value) {
            ItemDisplayMode.LIST -> ItemDisplayMode.GRID
            ItemDisplayMode.GRID -> ItemDisplayMode.LIST
        }
        setDisplayMode(newMode)
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
}
