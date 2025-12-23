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
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
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
    isAdManager: Boolean = false
) {
    val context = LocalContext.current
    
    // 如果没有提供广告单元ID，不显示广告
    if (adUnitId.isNullOrBlank()) {
        return
    }
    
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
            
            // 设置布局参数
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
                    // 广告加载失败，可以在这里记录日志或处理错误
                    android.util.Log.d("AdBanner", "广告加载失败: ${loadAdError.message}")
                }
                
                override fun onAdLoaded() {
                    android.util.Log.d("AdBanner", "广告加载成功")
                }
            }
            
            adView
        },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp) // 标准横幅广告高度
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
 */
@Composable
fun DynamicBannerAd(
    modifier: Modifier = Modifier,
    isAdManager: Boolean = false,
    testAdUnitId: String = "ca-app-pub-3940256099942544/6300978111", // Google 官方测试横幅 ID
    productionAdUnitId: String? = null,
    maxRetries: Int = 3,
    retryDelayMs: Long = 5000,
    hideOnFailure: Boolean = false
) {
    android.util.Log.d("DynamicBannerAd", "=== DynamicBannerAd 组件开始组合 ===")
    val context = LocalContext.current
    val sharedPrefs = remember { 
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) 
    }
    val scope = rememberCoroutineScope()
    
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
            if (key == "ad_banner_unit_id") {
                savedAdUnitId = sharedPrefs.getString("ad_banner_unit_id", null)
                android.util.Log.d("DynamicBannerAd", "广告ID已更新: $savedAdUnitId")
                // 重置重试计数
                retryCount = 0
                adLoadFailed = false
                shouldShowAd = true
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    // 获取最终要用的广告 ID（优先用保存的，没保存就用测试 ID 或正式 ID）
    val adUnitId = savedAdUnitId ?: (productionAdUnitId ?: testAdUnitId)
    
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
    
    // 如果失败且需要隐藏，则不显示广告
    if (hideOnFailure && adLoadFailed && retryCount >= maxRetries) {
        android.util.Log.d("DynamicBannerAd", "广告加载失败次数过多，隐藏广告视图")
        return
    }
    
    // 记录广告视图创建
    LaunchedEffect(adUnitId) {
        android.util.Log.d("DynamicBannerAd", "准备创建广告视图，广告ID: $adUnitId")
    }
    
    // 使用 AndroidView 嵌入原生 AdView
    // 使用 key() 确保当 adUnitId 变化时重新创建 AdView（因为 AdView 的 adUnitId 只能设置一次）
    key(adUnitId) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(90.dp) // 与物品卡片相同的高度
                .background(Color.White, RoundedCornerShape(12.dp)), // 给广告一点背景，避免透明问题，并添加圆角
            factory = { ctx ->
            android.util.Log.d("DynamicBannerAd", "Factory: 创建 AdView，广告ID: $adUnitId")
            
            // 获取屏幕宽度（dp）以确定合适的广告尺寸
            val displayMetrics = ctx.resources.displayMetrics
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
            
            // 根据屏幕宽度选择合适的广告尺寸
            // 如果屏幕宽度 >= 728dp，使用 LARGE_BANNER (320x100)，否则使用 BANNER (320x50)
            // 但为了占满 90dp 高度，我们优先使用 LARGE_BANNER
            val adSize = if (screenWidthDp >= 320) {
                AdSize.LARGE_BANNER // 320x100，更适合 90dp 高度
            } else {
                AdSize.BANNER // 320x50，标准横幅
            }
            
            val adView = if (isAdManager) {
                AdManagerAdView(ctx).apply {
                    setAdUnitId(adUnitId)
                    setAdSizes(adSize)
                    android.util.Log.d("DynamicBannerAd", "创建 AdManagerAdView，广告ID: $adUnitId，尺寸: $adSize")
                }
            } else {
                AdView(ctx).apply {
                    this.adUnitId = adUnitId
                    setAdSize(adSize)
                    android.util.Log.d("DynamicBannerAd", "创建 AdView，广告ID: $adUnitId，尺寸: $adSize")
                }
            }
            
            // 设置布局参数，使用 MATCH_PARENT 让广告占满整个容器
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT // 改为 MATCH_PARENT 以占满整个高度
            )
            adView.layoutParams = layoutParams
            
            // 加载广告的函数
            fun loadAdWithRetry() {
                // 使用 AdView 的当前 adUnitId，而不是闭包中的值（避免使用过期的值）
                val currentAdUnitId = if (isAdManager) {
                    (adView as? AdManagerAdView)?.adUnitId
                } else {
                    (adView as? AdView)?.adUnitId
                } ?: adUnitId
                
                android.util.Log.d("DynamicBannerAd", "开始加载广告，广告ID: $currentAdUnitId (AdView当前ID: $currentAdUnitId)")
                val adRequest = if (isAdManager) {
                    AdManagerAdRequest.Builder().build()
                } else {
                    AdRequest.Builder().build()
                }
                adView.loadAd(adRequest)
                android.util.Log.d("DynamicBannerAd", "已调用 loadAd()")
            }
            
            // 设置错误监听，包含重试逻辑
            adView.adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    val errorCode = loadAdError.code
                    val errorMessage = loadAdError.message
                    val errorDomain = loadAdError.domain
                    val errorCause = loadAdError.cause
                    
                    android.util.Log.w(
                        "DynamicBannerAd", 
                        "广告加载失败 [Code: $errorCode, Domain: $errorDomain]: $errorMessage"
                    )
                    
                    // 记录失败原因
                    if (errorCause != null) {
                        android.util.Log.w("DynamicBannerAd", "失败原因: ${errorCause.message}")
                    }
                    
                    adLoadFailed = true
                    
                    // 错误代码说明：
                    // ERROR_CODE_INTERNAL_ERROR = 0: 内部错误
                    // ERROR_CODE_INVALID_REQUEST = 1: 无效请求
                    // ERROR_CODE_NETWORK_ERROR = 2: 网络错误
                    // ERROR_CODE_NO_FILL = 3: 广告服务器没有可用的广告（No fill）
                    // ERROR_CODE_APP_ID_MISSING = 8: App ID 缺失
                    // ERROR_CODE_INVALID_AD_SIZE = 9: 无效的广告尺寸
                    
                    // 对于 "No fill" (ERROR_CODE_NO_FILL = 3) 或其他可重试的错误，进行重试
                    // ERROR_CODE_NO_FILL = 3 表示广告服务器没有可用的广告（这是正常的，特别是在测试环境中）
                    // ERROR_CODE_NETWORK_ERROR = 2 表示网络错误
                    // ERROR_CODE_INTERNAL_ERROR = 0 表示内部错误
                    if (retryCount < maxRetries && 
                        (errorCode == 3 || errorCode == 2 || errorCode == 0)) {
                        retryCount++
                        android.util.Log.d(
                            "DynamicBannerAd", 
                            "准备重试 ($retryCount/$maxRetries)，延迟 ${retryDelayMs}ms..."
                        )
                        
                        // 延迟后重试
                        scope.launch {
                            delay(retryDelayMs)
                            if (shouldShowAd) {
                                android.util.Log.d("DynamicBannerAd", "开始重试加载广告...")
                                loadAdWithRetry()
                            }
                        }
                    } else {
                        android.util.Log.w(
                            "DynamicBannerAd", 
                            "已达到最大重试次数或错误不可重试，停止重试"
                        )
                    }
                }
                
                override fun onAdLoaded() {
                    android.util.Log.d("DynamicBannerAd", "广告加载成功")
                    adLoadFailed = false
                    retryCount = 0 // 重置重试计数
                }
                
                override fun onAdOpened() {
                    android.util.Log.d("DynamicBannerAd", "广告被打开")
                }
                
                override fun onAdClosed() {
                    android.util.Log.d("DynamicBannerAd", "广告被关闭")
                }
                
                override fun onAdClicked() {
                    android.util.Log.d("DynamicBannerAd", "广告被点击")
                }
            }
            
            // 初始加载
            android.util.Log.d("DynamicBannerAd", "准备初始加载广告")
            loadAdWithRetry()
            
            android.util.Log.d("DynamicBannerAd", "AdView 创建完成")
            adView
        },
        update = { adView ->
            // 由于使用了 key(adUnitId)，当 adUnitId 变化时 AndroidView 会自动重新创建 AdView
            // 所以 update 块中不需要做任何操作
            // 这里可以用于其他更新逻辑（如果需要的话）
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

