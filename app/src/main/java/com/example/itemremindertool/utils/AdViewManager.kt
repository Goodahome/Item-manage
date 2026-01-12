package com.example.itemremindertool.utils

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.BaseAdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView

/**
 * 全局广告视图管理器
 * 用于保持 AdView 实例，防止页面切换时重新创建
 */
object AdViewManager {
    private const val TAG = "AdViewManager"
    
    // 存储 AdView 实例，key 为 adUnitId
    private val adViewMap = mutableMapOf<String, AdView>()
    private val adManagerViewMap = mutableMapOf<String, AdManagerAdView>()
    
    // 存储广告加载状态，key 为 adUnitId
    private val adLoadedMap = mutableMapOf<String, Boolean>()
    
    /**
     * 获取或创建 AdView
     */
    fun getOrCreateAdView(
        context: Context,
        adUnitId: String,
        isAdManager: Boolean = false
    ): BaseAdView? {
        return if (isAdManager) {
            getOrCreateAdManagerView(context, adUnitId)
        } else {
            synchronized(adViewMap) {
                adViewMap.getOrPut(adUnitId) {
                    Log.d(TAG, "创建新的 AdView，广告ID: $adUnitId")
                    AdView(context).apply {
                        this.adUnitId = adUnitId
                        setAdSize(AdSize.LARGE_BANNER)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT // 使用 WRAP_CONTENT 让 AdView 根据 AdSize 自动确定高度
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 获取或创建 AdManagerAdView
     */
    private fun getOrCreateAdManagerView(
        context: Context,
        adUnitId: String
    ): AdManagerAdView {
        return synchronized(adManagerViewMap) {
            adManagerViewMap.getOrPut(adUnitId) {
                Log.d(TAG, "创建新的 AdManagerAdView，广告ID: $adUnitId")
                AdManagerAdView(context).apply {
                    setAdUnitId(adUnitId)
                    setAdSizes(AdSize.LARGE_BANNER)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT // 使用 WRAP_CONTENT 让 AdView 根据 AdSize 自动确定高度
                    )
                }
            }
        }
    }
    
    /**
     * 检查广告是否已加载
     */
    fun isAdLoaded(adUnitId: String): Boolean {
        return synchronized(adLoadedMap) {
            adLoadedMap[adUnitId] ?: false
        }
    }
    
    /**
     * 设置广告加载状态
     */
    fun setAdLoaded(adUnitId: String, loaded: Boolean) {
        synchronized(adLoadedMap) {
            adLoadedMap[adUnitId] = loaded
            Log.d(TAG, "广告加载状态更新: $adUnitId = $loaded")
        }
    }
    
    /**
     * 加载广告（如果尚未加载）
     */
    fun loadAdIfNeeded(
        adView: BaseAdView,
        adUnitId: String,
        isAdManager: Boolean = false,
        onAdLoaded: () -> Unit = {},
        onAdFailedToLoad: (LoadAdError) -> Unit = {}
    ) {
        // 检查是否已加载
        if (isAdLoaded(adUnitId)) {
            Log.d(TAG, "广告已加载，跳过重新加载: $adUnitId")
            return
        }
        
        Log.d(TAG, "开始加载广告: $adUnitId")
        
        // 设置监听器
        adView.adListener = object : com.google.android.gms.ads.AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "广告加载成功: $adUnitId")
                setAdLoaded(adUnitId, true)
                onAdLoaded()
            }
            
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val errorCode = loadAdError.code
                val errorMessage = loadAdError.message
                val errorDomain = loadAdError.domain
                
                // 错误代码说明：
                // 0: ERROR_CODE_INTERNAL_ERROR - 内部错误
                // 1: ERROR_CODE_INVALID_REQUEST - 无效请求
                // 2: ERROR_CODE_NETWORK_ERROR - 网络错误
                // 3: ERROR_CODE_NO_FILL - 没有可用广告（正常情况）
                
                when (errorCode) {
                    3 -> {
                        // No fill 是正常情况，使用 DEBUG 级别
                        Log.d(TAG, "广告暂无填充: $adUnitId (错误代码: $errorCode, 域: $errorDomain)")
                    }
                    else -> {
                        // 其他错误使用 WARNING 级别
                        Log.w(TAG, "广告加载失败: $adUnitId, 错误代码: $errorCode, 域: $errorDomain, 消息: $errorMessage")
                    }
                }
                
                setAdLoaded(adUnitId, false)
                onAdFailedToLoad(loadAdError)
            }
        }
        
        // 加载广告
        val adRequest = if (isAdManager) {
            AdManagerAdRequest.Builder().build()
        } else {
            AdRequest.Builder().build()
        }
        adView.loadAd(adRequest)
    }
    
    /**
     * 清理指定广告ID的 AdView
     */
    fun removeAdView(adUnitId: String, isAdManager: Boolean = false) {
        if (isAdManager) {
            synchronized(adManagerViewMap) {
                adManagerViewMap[adUnitId]?.destroy()
                adManagerViewMap.remove(adUnitId)
            }
        } else {
            synchronized(adViewMap) {
                adViewMap[adUnitId]?.destroy()
                adViewMap.remove(adUnitId)
            }
        }
        synchronized(adLoadedMap) {
            adLoadedMap.remove(adUnitId)
        }
        Log.d(TAG, "已清理 AdView: $adUnitId")
    }
    
    /**
     * 清理所有 AdView
     */
    fun clearAll() {
        synchronized(adViewMap) {
            adViewMap.values.forEach { it.destroy() }
            adViewMap.clear()
        }
        synchronized(adManagerViewMap) {
            adManagerViewMap.values.forEach { it.destroy() }
            adManagerViewMap.clear()
        }
        synchronized(adLoadedMap) {
            adLoadedMap.clear()
        }
        Log.d(TAG, "已清理所有 AdView")
    }
}

