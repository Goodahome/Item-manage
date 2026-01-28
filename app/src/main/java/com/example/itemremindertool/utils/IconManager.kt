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
    
    // ActivityAlias 名称后缀列表（与包名拼接）
    private val ICON_ALIAS_SUFFIXES = arrayOf(
        ".MainActivity.Icon0",  // icon0 (默认)
        ".MainActivity.Icon1",  // icon1
        ".MainActivity.Icon2",  // icon2
        ".MainActivity.Icon3",  // icon3
        ".MainActivity.Icon4"   // icon4
    )

    private fun getAliasName(context: Context, index: Int): String {
        return context.packageName + ICON_ALIAS_SUFFIXES[index]
    }
    
    /**
     * 获取当前选中的图标索引
     */
    fun getCurrentIcon(context: Context): Int {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return try {
            // 尝试读取 Int 类型
            prefs.getInt(PREF_KEY_ICON, DEFAULT_ICON)
        } catch (e: ClassCastException) {
            // 如果存储的是 String 类型，尝试转换
            try {
                val stringValue = prefs.getString(PREF_KEY_ICON, DEFAULT_ICON.toString())
                val intValue = stringValue?.toIntOrNull() ?: DEFAULT_ICON
                // 清理错误的值，重新存储为 Int 类型
                prefs.edit().putInt(PREF_KEY_ICON, intValue).apply()
                intValue
            } catch (e2: Exception) {
                // 如果转换失败，使用默认值并清理
                prefs.edit().remove(PREF_KEY_ICON).putInt(PREF_KEY_ICON, DEFAULT_ICON).apply()
                DEFAULT_ICON
            }
        }
    }
    
    /**
     * 切换应用图标
     * @param context 上下文
     * @param iconIndex 图标索引 (0-4)
     * @return 是否切换成功
     */
    fun switchIcon(context: Context, iconIndex: Int, commit: Boolean = false): Boolean {
        if (iconIndex < 0 || iconIndex >= ICON_ALIAS_SUFFIXES.size) {
            return false
        }
        
        val packageManager = context.packageManager
        val currentIcon = getCurrentIcon(context)
        
        // 禁用所有图标
        for (i in ICON_ALIAS_SUFFIXES.indices) {
            val componentName = ComponentName(context, getAliasName(context, i))
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
        if (commit) {
            prefs.edit().putInt(PREF_KEY_ICON, iconIndex).commit()
        } else {
            prefs.edit().putInt(PREF_KEY_ICON, iconIndex).apply()
        }
        
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

    /**
     * 获取图标资源列表（用于预览）
     */
    fun getIconResIds(): List<Int> {
        return listOf(
            R.mipmap.ic_launcher,
            R.mipmap.ic_launcher_icon1,
            R.mipmap.ic_launcher_icon2,
            R.mipmap.ic_launcher_icon3,
            R.mipmap.ic_launcher_icon4
        )
    }
}

