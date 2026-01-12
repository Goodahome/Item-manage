package com.example.itemremindertool.config

/**
 * 应用功能开关配置
 * 统一管理应用中的功能开关，便于开启/关闭特定功能
 */
object FeatureFlags {
    /**
     * 是否启用付费购买功能
     * 
     * 设置为 false 时：
     * - 隐藏所有购买相关的 UI
     * - 不初始化 BillingManager
     * - 自动开放所有高级功能（用于开发测试）
     * 
     * 设置为 true 时：
     * - 显示购买按钮和购买对话框
     * - 正常初始化 BillingManager
     * - 需要购买或试用才能使用高级功能
     */
    const val ENABLE_PURCHASE_FEATURE = false
}
