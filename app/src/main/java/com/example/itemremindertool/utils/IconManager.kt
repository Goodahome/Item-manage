package com.example.itemremindertool.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.itemremindertool.R

/**
 * 图标切换管理器
 * 使用 ActivityAlias 实现应用图标切换功能
 */
object IconManager {
    private const val PREF_KEY_ICON = "selected_app_icon"
    private const val DEFAULT_ICON = 0
    
    // ActivityAlias 名称列表
    private val ICON_ALIASES = arrayOf(
        "com.example.itemremindertool.MainActivity.Icon0",  // icon0 (默认)
        "com.example.itemremindertool.MainActivity.Icon1",  // icon1
        "com.example.itemremindertool.MainActivity.Icon2",  // icon2
        "com.example.itemremindertool.MainActivity.Icon3",  // icon3
        "com.example.itemremindertool.MainActivity.Icon4"   // icon4
    )
    
    /**
     * 获取当前选中的图标索引
     */
    fun getCurrentIcon(context: Context): Int {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getInt(PREF_KEY_ICON, DEFAULT_ICON)
    }
    
    /**
     * 切换应用图标
     * @param context 上下文
     * @param iconIndex 图标索引 (0-4)
     * @return 是否切换成功
     */
    fun switchIcon(context: Context, iconIndex: Int): Boolean {
        if (iconIndex < 0 || iconIndex >= ICON_ALIASES.size) {
            return false
        }
        
        val packageManager = context.packageManager
        val currentIcon = getCurrentIcon(context)
        
        // 禁用所有图标
        for (i in ICON_ALIASES.indices) {
            val componentName = ComponentName(context, ICON_ALIASES[i])
            val state = if (i == iconIndex) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            packageManager.setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP
            )
        }
        
        // 保存选中的图标索引
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putInt(PREF_KEY_ICON, iconIndex).apply()
        
        return true
    }
    
    /**
     * 初始化图标状态（在应用启动时调用）
     * 确保只有选中的图标是启用的
     */
    fun initializeIcon(context: Context) {
        val iconIndex = getCurrentIcon(context)
        switchIcon(context, iconIndex)
    }
    
    /**
     * 获取图标名称列表
     */
    fun getIconNames(context: Context): List<String> {
        return listOf(
            context.getString(R.string.icon_name_0),
            context.getString(R.string.icon_name_1),
            context.getString(R.string.icon_name_2),
            context.getString(R.string.icon_name_3),
            context.getString(R.string.icon_name_4)
        )
    }
}

