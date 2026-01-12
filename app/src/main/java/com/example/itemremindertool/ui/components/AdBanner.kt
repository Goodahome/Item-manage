package com.example.itemremindertool.ui.components

import android.content.Context
import android.content.SharedPreferences
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.example.itemremindertool.utils.AdViewManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 广告横幅组件
 * 支持 Google AdMob 和 Ad Manager
 * 
 * @param adUnitId 广告单元ID，如果为空则不显示广告
 * @param modifier 修饰符
 * @param isAdManager 是否为 Ad Manager（默认为 false，使用 AdMob）
 */
@Composable
fun AdBanner(
    adUnitId: String? = null,
    modifier: Modifier = Modifier,
    isAdManager: Boolean = false,
    height: Dp = 60.dp // 广告高度，默认60dp
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // 如果没有提供广告单元ID，不显示广告
    if (adUnitId.isNullOrBlank()) {
        return
    }
    
    // 将高度转换为像素
    val heightPx = with(density) { height.toPx().toInt() }
    
    // 使用 AndroidView 嵌入原生 AdView
    AndroidView(
        factory = { ctx ->
            val adView = if (isAdManager) {
                AdManagerAdView(ctx).apply {
                    setAdUnitId(adUnitId)
                    setAdSizes(AdSize.BANNER)
                }
            } else {
                AdView(ctx).apply {
                    this.adUnitId = adUnitId
                    setAdSize(AdSize.BANNER)
                }
            }
            
            // 设置布局参数，使用 WRAP_CONTENT 让 AdView 根据 AdSize 自动确定高度
            // Compose 的 height 修饰符会控制容器大小，但 AdView 内部需要根据 AdSize 渲染
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            adView.layoutParams = layoutParams
            
            // 加载广告
            val adRequest = if (isAdManager) {
                AdManagerAdRequest.Builder().build()
            } else {
                AdRequest.Builder().build()
            }
            
            adView.loadAd(adRequest)
            
            // 设置错误监听（可选，用于调试）
            adView.adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    val errorCode = loadAdError.code
                    // 错误代码 3 (NO_FILL) 是正常情况，使用 DEBUG 级别
                    if (errorCode == 3) {
                        android.util.Log.d("AdBanner", "广告暂无填充 (错误代码: $errorCode)")
                    } else {
                        android.util.Log.w("AdBanner", "广告加载失败: 错误代码=$errorCode, 消息=${loadAdError.message}")
                    }
                }
                
                override fun onAdLoaded() {
                    android.util.Log.d("AdBanner", "广告加载成功")
                }
            }
            
            adView
        },
        modifier = modifier
            .fillMaxWidth()
            .height(height) // 使用参数化的高度
    )
}

/**
 * 动态广告横幅组件
 * 从 SharedPreferences 读取广告单元 ID，支持运行时更新
 * 包含自动重试机制和错误处理
 * 
 * @param modifier 修饰符
 * @param isAdManager 是否为 Ad Manager（默认为 false，使用 AdMob）
 * @param testAdUnitId 测试广告 ID（用于开发阶段，如果 SharedPreferences 中没有保存 ID 则使用此 ID）
 * @param productionAdUnitId 正式广告 ID（可选，如果设置了且 SharedPreferences 中没有保存 ID，则使用此 ID）
 * @param maxRetries 最大重试次数（默认 3 次）
 * @param retryDelayMs 重试延迟时间（毫秒，默认 5000ms = 5秒）
 * @param hideOnFailure 加载失败时是否隐藏广告视图（默认 false，显示空白区域）
 * @param height 广告高度（默认 90.dp，与物品卡片相同的高度）
 */
@Composable
fun DynamicBannerAd(
    modifier: Modifier = Modifier,
    isAdManager: Boolean = false,
    testAdUnitId: String = "ca-app-pub-3940256099942544/6300978111", // Google 官方测试横幅 ID
    productionAdUnitId: String = "ca-app-pub-9384252615968132/7853152781",
    maxRetries: Int = 3,
    retryDelayMs: Long = 5000,
    hideOnFailure: Boolean = false,
    height: Dp = 90.dp // 广告高度，默认90dp
) {
    android.util.Log.d("DynamicBannerAd", "=== DynamicBannerAd 组件开始组合 ===")
    val context = LocalContext.current
    val sharedPrefs = remember { 
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) 
    }
    val scope = rememberCoroutineScope()
    
    // 检查是否已购买移除广告（监听变化）
    var isAdsRemoved by remember { mutableStateOf(sharedPrefs.getBoolean("ads_removed", false)) }
    
    // 使用 DisposableEffect 监听 SharedPreferences 变化
    var savedAdUnitId by remember { mutableStateOf<String?>(null) }
    
    // 广告加载状态
    var adLoadFailed by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }
    var shouldShowAd by remember { mutableStateOf(true) }
    
    DisposableEffect(Unit) {
        // 初始读取
        savedAdUnitId = sharedPrefs.getString("ad_banner_unit_id", null)
        android.util.Log.d("DynamicBannerAd", "初始读取广告ID: $savedAdUnitId")
        
        // 监听 SharedPreferences 变化
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "ad_banner_unit_id" -> {
                    savedAdUnitId = sharedPrefs.getString("ad_banner_unit_id", null)
                    android.util.Log.d("DynamicBannerAd", "广告ID已更新: $savedAdUnitId")
                    // 重置重试计数
                    retryCount = 0
                    adLoadFailed = false
                    shouldShowAd = true
                }
                "ads_removed" -> {
                    isAdsRemoved = sharedPrefs.getBoolean("ads_removed", false)
                    android.util.Log.d("DynamicBannerAd", "广告移除状态已更新: $isAdsRemoved")
                }
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    // 获取最终要用的广告 ID（优先用保存的，没保存就用测试 ID 或正式 ID）
    val adUnitId = savedAdUnitId ?: (productionAdUnitId ?: testAdUnitId)
    
    // 使用全局管理器检查广告是否已加载
    var adLoaded by remember(adUnitId) { mutableStateOf(AdViewManager.isAdLoaded(adUnitId)) }
    
    // 记录最终使用的广告ID
    LaunchedEffect(adUnitId) {
        android.util.Log.d("DynamicBannerAd", "使用广告ID: $adUnitId")
    }
    
    // 检查 Google Play Services 是否可用
    val googleApiAvailability = GoogleApiAvailability.getInstance()
    val googlePlayServicesAvailable = remember {
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        resultCode == ConnectionResult.SUCCESS
    }
    
    // 如果 Google Play Services 不可用，记录警告但不阻止显示（让 AdMob SDK 自己处理）
    if (!googlePlayServicesAvailable) {
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        android.util.Log.w(
            "DynamicBannerAd",
            "Google Play Services 不可用 (错误代码: $resultCode): ${googleApiAvailability.getErrorString(resultCode)}"
        )
    }
    
    // 如果已购买移除广告，不显示广告
    if (isAdsRemoved) {
        android.util.Log.d("DynamicBannerAd", "已购买移除广告，不显示广告")
        return
    }
    
    // 如果失败且需要隐藏，则不显示广告
    if (hideOnFailure && adLoadFailed && retryCount >= maxRetries) {
        android.util.Log.d("DynamicBannerAd", "广告加载失败次数过多，隐藏广告视图")
        return
    }
    
    // 记录广告视图创建
    LaunchedEffect(adUnitId) {
        android.util.Log.d("DynamicBannerAd", "准备创建广告视图，广告ID: $adUnitId")
    }
    
    // 使用全局管理器获取或创建 AdView
    val adView = remember(adUnitId) {
        AdViewManager.getOrCreateAdView(context, adUnitId, isAdManager)
    }
    
    // 只在广告加载成功后才显示广告视图，避免显示白屏
    if (!adLoaded || adView == null) {
        // 广告未加载成功或 AdView 为空，不显示任何内容（不显示白屏）
        // 但如果 AdView 存在，尝试加载广告
        LaunchedEffect(adUnitId, adView) {
            if (adView != null && !adLoaded) {
                AdViewManager.loadAdIfNeeded(
                    adView = adView,
                    adUnitId = adUnitId,
                    isAdManager = isAdManager,
                    onAdLoaded = {
                        // 广告加载成功，更新状态以触发重组显示广告
                        adLoaded = true
                    },
                    onAdFailedToLoad = { loadAdError ->
                        adLoadFailed = true
                        // 重试逻辑
                        if (retryCount < maxRetries && 
                            (loadAdError.code == 3 || loadAdError.code == 2 || loadAdError.code == 0)) {
                            retryCount++
                            scope.launch {
                                delay(retryDelayMs)
                                if (shouldShowAd && adView != null) {
                                    AdViewManager.loadAdIfNeeded(
                                        adView = adView,
                                        adUnitId = adUnitId,
                                        isAdManager = isAdManager,
                                        onAdLoaded = {
                                            adLoaded = true
                                        },
                                        onAdFailedToLoad = {}
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
        return
    }
    
    // 使用 AndroidView 嵌入原生 AdView
    // 使用 key() 确保当 adUnitId 变化时重新创建 AndroidView（但 AdView 实例由全局管理器保持）
    key(adUnitId) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(height) // 使用参数化的高度
                .background(Color.White, RoundedCornerShape(12.dp)), // 给广告一点背景，避免透明问题，并添加圆角
            factory = { ctx ->
                android.util.Log.d("DynamicBannerAd", "Factory: 使用已存在的 AdView，广告ID: $adUnitId")
                adView!!
            },
            update = { view ->
                // 不需要做任何操作，AdView 实例由全局管理器保持
                // 页面切换时不会重新创建 AdView，因此不会触发重新加载
            }
        )
    }
}

/**
 * 广告横幅占位符（用于开发测试，不显示实际广告）
 */
@Composable
fun AdBannerPlaceholder(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "广告区域",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

