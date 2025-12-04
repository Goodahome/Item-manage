package com.example.itemremindertool.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 提醒设置管理器
 */
class AlertSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("alert_settings", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_EXPIRY_REMINDER_DAYS = "expiry_reminder_days"
        private const val KEY_LOW_STOCK_THRESHOLD = "low_stock_threshold"
        private const val KEY_FORGET_PROTECTION_ENABLED = "forget_protection_enabled"
        private const val KEY_SYSTEM_NOTIFICATION_ENABLED = "system_notification_enabled"
        private const val KEY_NOTIFICATION_HOUR = "notification_hour"
        private const val KEY_NOTIFICATION_MINUTE = "notification_minute"
        
        // 默认值
        private const val DEFAULT_EXPIRY_REMINDER_DAYS = 7
        private const val DEFAULT_LOW_STOCK_THRESHOLD = 1
        private const val DEFAULT_FORGET_PROTECTION_ENABLED = true
        private const val DEFAULT_SYSTEM_NOTIFICATION_ENABLED = true
        private const val DEFAULT_NOTIFICATION_HOUR = 9 // 默认上午9点
        private const val DEFAULT_NOTIFICATION_MINUTE = 0
    }
    
    /**
     * 获取到期提醒期限（天数）
     */
    fun getExpiryReminderDays(): Int {
        return prefs.getInt(KEY_EXPIRY_REMINDER_DAYS, DEFAULT_EXPIRY_REMINDER_DAYS)
    }
    
    /**
     * 设置到期提醒期限（天数）
     */
    fun setExpiryReminderDays(days: Int) {
        prefs.edit().putInt(KEY_EXPIRY_REMINDER_DAYS, days).apply()
    }
    
    /**
     * 获取库存提醒阈值
     */
    fun getLowStockThreshold(): Int {
        return prefs.getInt(KEY_LOW_STOCK_THRESHOLD, DEFAULT_LOW_STOCK_THRESHOLD)
    }
    
    /**
     * 设置库存提醒阈值
     */
    fun setLowStockThreshold(threshold: Int) {
        prefs.edit().putInt(KEY_LOW_STOCK_THRESHOLD, threshold).apply()
    }
    
    /**
     * 获取防遗忘提醒是否启用
     */
    fun isForgetProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_FORGET_PROTECTION_ENABLED, DEFAULT_FORGET_PROTECTION_ENABLED)
    }
    
    /**
     * 设置防遗忘提醒是否启用
     */
    fun setForgetProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FORGET_PROTECTION_ENABLED, enabled).apply()
    }
    
    /**
     * 获取系统通知是否启用
     */
    fun isSystemNotificationEnabled(): Boolean {
        return prefs.getBoolean(KEY_SYSTEM_NOTIFICATION_ENABLED, DEFAULT_SYSTEM_NOTIFICATION_ENABLED)
    }
    
    /**
     * 获取系统通知是否启用（别名方法）
     */
    fun getSystemNotificationEnabled(): Boolean {
        return isSystemNotificationEnabled()
    }
    
    /**
     * 设置系统通知是否启用
     */
    fun setSystemNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SYSTEM_NOTIFICATION_ENABLED, enabled).apply()
    }
    
    /**
     * 获取系统通知提醒时间（小时）
     */
    fun getNotificationHour(): Int {
        return prefs.getInt(KEY_NOTIFICATION_HOUR, DEFAULT_NOTIFICATION_HOUR)
    }
    
    /**
     * 设置系统通知提醒时间（小时）
     */
    fun setNotificationHour(hour: Int) {
        prefs.edit().putInt(KEY_NOTIFICATION_HOUR, hour).apply()
    }
    
    /**
     * 获取系统通知提醒时间（分钟）
     */
    fun getNotificationMinute(): Int {
        return prefs.getInt(KEY_NOTIFICATION_MINUTE, DEFAULT_NOTIFICATION_MINUTE)
    }
    
    /**
     * 设置系统通知提醒时间（分钟）
     */
    fun setNotificationMinute(minute: Int) {
        prefs.edit().putInt(KEY_NOTIFICATION_MINUTE, minute).apply()
    }
}

