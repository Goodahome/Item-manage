package com.example.itemremindertool.utils

import android.content.Context
import com.example.itemremindertool.R

/**
 * 应用配置管理器
 * 注意：Android系统级别的应用名称和图标修改需要重新安装应用
 * 这里我们只能修改应用内部显示的名称，系统级别的修改需要用户重新安装应用
 */
object AppConfigManager {
    /**
     * 检查是否有待应用的系统级别更改
     */
    fun hasPendingSystemChanges(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("pending_name_change", false)
    }
    
    /**
     * 应用待处理的更改（在应用启动时调用）
     * 注意：系统级别的名称修改需要重新安装应用，这里只能提示用户
     */
    fun applyPendingChanges(context: Context) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        
        // 检查是否有待应用的名称更改
        if (prefs.getBoolean("pending_name_change", false)) {
            // 系统级别的名称修改需要重新安装应用
            // 这里我们只能清除标志，因为实际修改需要重新编译和安装
            // 在实际应用中，可以提示用户需要重新安装应用
            prefs.edit().putBoolean("pending_name_change", false).apply()
        }
    }
    
    /**
     * 获取自定义的应用名称
     */
    fun getCustomAppName(context: Context): String {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val defaultAppName = context.getString(R.string.app_name)
        return prefs.getString("app_name", defaultAppName) ?: defaultAppName
    }
}

