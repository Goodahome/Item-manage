package com.example.itemremindertool.billing

import android.content.Context
import android.content.SharedPreferences
import com.example.itemremindertool.config.FeatureFlags

/**
 * 高级功能管理器
 * 统一管理高级功能的访问权限检查
 */
object PremiumFeatureManager {
    
    private const val PREFS_KEY_PREMIUM_FEATURES = "premium_features"
    private const val PREFS_KEY_TRIAL_START_TIME = "premium_trial_start_time"
    private const val PREFS_KEY_TRIAL_USED = "premium_trial_used"
    private const val TRIAL_DURATION_MS = 3 * 24 * 60 * 60 * 1000L // 2分钟试用期（测试用）
    
    /**
     * 检查是否已购买高级功能
     */
    fun isPremiumPurchased(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREFS_KEY_PREMIUM_FEATURES, false)
    }
    
    /**
     * 检查是否在试用期内
     */
    fun isTrialActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val trialUsed = prefs.getBoolean(PREFS_KEY_TRIAL_USED, false)
        if (!trialUsed) {
            return false // 未开始试用
        }
        
        val trialStartTime = prefs.getLong(PREFS_KEY_TRIAL_START_TIME, 0)
        if (trialStartTime == 0L) {
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - trialStartTime
        
        return elapsedTime < TRIAL_DURATION_MS
    }
    
    /**
     * 检查是否可以访问高级功能（已购买或在试用期内）
     * 如果付费购买功能已关闭，则自动开放所有高级功能（用于开发测试）
     */
    fun canAccessPremiumFeatures(context: Context): Boolean {
        // 如果付费购买功能已关闭，自动开放所有高级功能
        if (!FeatureFlags.ENABLE_PURCHASE_FEATURE) {
            return true
        }
        return isPremiumPurchased(context) || isTrialActive(context)
    }
    
    /**
     * 开始试用
     */
    fun startTrial(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val trialUsed = prefs.getBoolean(PREFS_KEY_TRIAL_USED, false)
        
        if (trialUsed) {
            return false // 试用已使用过
        }
        
        val currentTime = System.currentTimeMillis()
        prefs.edit()
            .putBoolean(PREFS_KEY_TRIAL_USED, true)
            .putLong(PREFS_KEY_TRIAL_START_TIME, currentTime)
            .apply()
        
        return true
    }
    
    /**
     * 获取剩余试用时间（毫秒）
     */
    fun getRemainingTrialTime(context: Context): Long {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val trialStartTime = prefs.getLong(PREFS_KEY_TRIAL_START_TIME, 0)
        
        if (trialStartTime == 0L) {
            return 0L
        }
        
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - trialStartTime
        val remainingTime = TRIAL_DURATION_MS - elapsedTime
        
        return remainingTime.coerceAtLeast(0L)
    }
    
    /**
     * 设置高级功能已购买
     */
    fun setPremiumPurchased(context: Context, purchased: Boolean) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(PREFS_KEY_PREMIUM_FEATURES, purchased)
            .apply()
    }
}

