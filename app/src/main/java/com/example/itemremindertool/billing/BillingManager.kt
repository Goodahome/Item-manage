package com.example.itemremindertool.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.example.itemremindertool.billing.PremiumFeatureManager

/**
 * 内购管理器
 * 管理 Google Play Billing 的购买流程
 * 支持多个产品：remove_ads（移除广告）和 premium_features（高级功能）
 */
class BillingManager(
    private val context: Context,
    private val productIds: List<String> = listOf("remove_ads", "premium_features") // 支持多个产品
) : DefaultLifecycleObserver {
    
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    private var billingClient: BillingClient? = null
    
    // 各个产品的购买状态流
    private val _purchaseStates = MutableStateFlow<Map<String, PurchaseState>>(emptyMap())
    val purchaseStates: StateFlow<Map<String, PurchaseState>> = _purchaseStates.asStateFlow()
    
    // 各个产品的详情
    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()
    
    // 是否已初始化
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    companion object {
        private const val TAG = "BillingManager"
        private const val PREFS_KEY_ADS_REMOVED = "ads_removed"
        private const val PREFS_KEY_PREMIUM_FEATURES = "premium_features"
        private const val PREFS_KEY_PREMIUM_LIFETIME = "premium_lifetime"
        
        // 产品ID常量
        const val PRODUCT_REMOVE_ADS = "remove_ads"
        const val PRODUCT_PREMIUM_FEATURES = "premium_features"
        const val PRODUCT_PREMIUM_LIFETIME = "premium_lifetime"
    }

    private val productTypeMap = mapOf(
        PRODUCT_REMOVE_ADS to BillingClient.ProductType.INAPP,
        PRODUCT_PREMIUM_FEATURES to BillingClient.ProductType.SUBS,
        PRODUCT_PREMIUM_LIFETIME to BillingClient.ProductType.INAPP
    )
    
    // 向后兼容：单个产品的状态流（移除广告）
    val purchaseState: StateFlow<PurchaseState> = 
        _purchaseStates.map { states ->
            states[PRODUCT_REMOVE_ADS] ?: PurchaseState.NotPurchased
        }.stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.Eagerly,
            initialValue = PurchaseState.NotPurchased
        )
    
    // 向后兼容：单个产品的详情（移除广告）
    val productDetails: StateFlow<ProductDetails?> = 
        _productDetailsMap.map { detailsMap ->
            detailsMap[PRODUCT_REMOVE_ADS]
        }.stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    
    /**
     * 初始化 BillingClient
     */
    fun initialize() {
        if (billingClient != null) {
            Log.d(TAG, "BillingClient 已初始化")
            return
        }
        
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    Log.d(TAG, "用户取消了购买")
                } else {
                    Log.e(TAG, "购买处理错误: ${billingResult.debugMessage}")
                }
            }
            .enablePendingPurchases()
            .build()
        
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient 连接成功")
                    _isReady.value = true
                    queryPurchases()
                    queryProductDetails()
                } else {
                    Log.e(TAG, "BillingClient 连接失败: ${billingResult.debugMessage}")
                    _isReady.value = false
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "BillingClient 连接断开")
                _isReady.value = false
            }
        })
    }
    
    /**
     * 查询已购买的产品
     */
    private fun queryPurchases() {
        queryPurchasesInternal(null)
    }

    private fun queryPurchasesInternal(onComplete: ((Boolean) -> Unit)?) {
        val billingClient = billingClient ?: return
        val states = mutableMapOf<String, PurchaseState>()
        val collectedPurchases = mutableListOf<Purchase>()

        fun applyStates() {
            productIds.forEach { productId ->
                val hasPurchase = collectedPurchases.any { it.products.contains(productId) }
                if (hasPurchase) {
                    val purchase = collectedPurchases.first { it.products.contains(productId) }
                    handlePurchase(purchase)
                    states[productId] = PurchaseState.Purchased
                } else {
                    val isPurchased = when (productId) {
                        PRODUCT_REMOVE_ADS -> sharedPrefs.getBoolean(PREFS_KEY_ADS_REMOVED, false)
                        PRODUCT_PREMIUM_FEATURES -> sharedPrefs.getBoolean(PREFS_KEY_PREMIUM_FEATURES, false)
                        PRODUCT_PREMIUM_LIFETIME -> sharedPrefs.getBoolean(PREFS_KEY_PREMIUM_LIFETIME, false)
                        else -> false
                    }
                    states[productId] = if (isPurchased) {
                        PurchaseState.Purchased
                    } else {
                        PurchaseState.NotPurchased
                    }
                    if (productId == PRODUCT_PREMIUM_FEATURES && isPurchased) {
                        PremiumFeatureManager.setPremiumPurchased(context, true)
                    }
                    if (productId == PRODUCT_PREMIUM_LIFETIME && isPurchased) {
                        PremiumFeatureManager.setLifetimePurchased(context, true)
                    }
                }
            }
            _purchaseStates.value = states
            onComplete?.invoke(states.values.any { it is PurchaseState.Purchased })
        }

        val typesToQuery = listOf(
            BillingClient.ProductType.INAPP,
            BillingClient.ProductType.SUBS
        )

        var completedQueries = 0
        typesToQuery.forEach { type ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(type)
                    .build()
            ) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    collectedPurchases.addAll(purchases)
                } else {
                    Log.e(TAG, "查询购买失败($type): ${billingResult.debugMessage}")
                }
                completedQueries += 1
                if (completedQueries == typesToQuery.size) {
                    applyStates()
                }
            }
        }
    }
    
    /**
     * 查询产品详情
     */
    private fun queryProductDetails() {
        val billingClient = billingClient ?: return
        val detailsMap = _productDetailsMap.value.toMutableMap()

        val productsByType = productIds.groupBy { productTypeMap[it] ?: BillingClient.ProductType.INAPP }
        var completedQueries = 0
        val totalQueries = productsByType.size

        productsByType.forEach { (type, ids) ->
            val productList = ids.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(type)
                    .build()
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    productDetailsList.forEach { detailsMap[it.productId] = it }
                    Log.d(TAG, "产品详情查询成功($type): ${productDetailsList.map { it.productId }}")
                } else {
                    Log.e(TAG, "产品详情查询失败($type): ${billingResult.debugMessage}")
                }
                completedQueries += 1
                if (completedQueries == totalQueries) {
                    _productDetailsMap.value = detailsMap
                }
            }
        }
    }
    
    /**
     * 手动恢复购买/订阅
     */
    fun restorePurchases(onComplete: (Boolean) -> Unit) {
        if (billingClient == null || !_isReady.value) {
            onComplete(false)
            return
        }
        queryPurchasesInternal { hasPurchase ->
            onComplete(hasPurchase)
        }
    }

    /**
     * 打开订阅管理页
     */
    fun openSubscriptionManagement(activity: Activity, productId: String = PRODUCT_PREMIUM_FEATURES): Boolean {
        return try {
            val uri = Uri.parse(
                "https://play.google.com/store/account/subscriptions?sku=$productId&package=${context.packageName}"
            )
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (e: Exception) {
            Log.e(TAG, "打开订阅管理失败: ${e.message}")
            false
        }
    }

    /**
     * 启动购买流程
     * @param activity Activity 实例
     * @param productId 要购买的产品ID，默认为 remove_ads
     */
    fun launchPurchaseFlow(
        activity: Activity,
        productId: String = PRODUCT_REMOVE_ADS,
        offerToken: String? = null
    ): Boolean {
        val billingClient = billingClient
        val productDetails = _productDetailsMap.value[productId]
        
        if (billingClient == null || !_isReady.value) {
            Log.e(TAG, "BillingClient 未就绪")
            return false
        }
        
        if (productDetails == null) {
            Log.e(TAG, "产品详情为空，请先查询产品详情: $productId")
            // 尝试重新查询产品详情
            queryProductDetails()
            return false
        }
        
        val productType = productTypeMap[productId] ?: BillingClient.ProductType.INAPP
        val detailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        if (productType == BillingClient.ProductType.SUBS) {
            val tokenToUse = offerToken
                ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (tokenToUse == null) {
                Log.e(TAG, "订阅 offerToken 获取失败: $productId")
                return false
            }
            detailsParamsBuilder.setOfferToken(tokenToUse)
        }
        val productDetailsParamsList = listOf(detailsParamsBuilder.build())
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (responseCode.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "启动购买流程失败: ${responseCode.debugMessage}")
            return false
        }
        return true
    }
    
    /**
     * 处理购买结果
     */
    private fun handlePurchase(purchase: Purchase) {
        purchase.products.forEach { productId ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    // 确认购买
                    acknowledgePurchase(purchase, productId)
                } else {
                    // 已确认，保存状态
                    savePurchaseState(productId, true)
                    val currentStates = _purchaseStates.value.toMutableMap()
                    currentStates[productId] = PurchaseState.Purchased
                    _purchaseStates.value = currentStates
                    
                    // 如果是高级功能产品，更新 PremiumFeatureManager
                    if (productId == PRODUCT_PREMIUM_FEATURES) {
                        PremiumFeatureManager.setPremiumPurchased(context, true)
                    }
                    if (productId == PRODUCT_PREMIUM_LIFETIME) {
                        PremiumFeatureManager.setLifetimePurchased(context, true)
                    }
                    
                    Log.d(TAG, "购买已确认: $productId")
                }
            }
        }
    }
    
    /**
     * 确认购买
     */
    private fun acknowledgePurchase(purchase: Purchase, productId: String) {
        val billingClient = billingClient ?: return
        
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                savePurchaseState(productId, true)
                val currentStates = _purchaseStates.value.toMutableMap()
                currentStates[productId] = PurchaseState.Purchased
                _purchaseStates.value = currentStates
                
                // 如果是高级功能产品，更新 PremiumFeatureManager
                if (productId == PRODUCT_PREMIUM_FEATURES) {
                    PremiumFeatureManager.setPremiumPurchased(context, true)
                }
                if (productId == PRODUCT_PREMIUM_LIFETIME) {
                    PremiumFeatureManager.setLifetimePurchased(context, true)
                }
                
                Log.d(TAG, "购买确认成功: $productId")
            } else {
                Log.e(TAG, "购买确认失败: ${billingResult.debugMessage}")
            }
        }
    }
    
    /**
     * 保存购买状态
     */
    private fun savePurchaseState(productId: String, isPurchased: Boolean) {
        val prefsKey = when (productId) {
            PRODUCT_REMOVE_ADS -> PREFS_KEY_ADS_REMOVED
            PRODUCT_PREMIUM_FEATURES -> PREFS_KEY_PREMIUM_FEATURES
            PRODUCT_PREMIUM_LIFETIME -> PREFS_KEY_PREMIUM_LIFETIME
            else -> return
        }
        sharedPrefs.edit()
            .putBoolean(prefsKey, isPurchased)
            .apply()
    }
    
    /**
     * 检查是否已购买移除广告（从本地读取）
     */
    fun isAdsRemoved(): Boolean {
        return sharedPrefs.getBoolean(PREFS_KEY_ADS_REMOVED, false) || 
               (_purchaseStates.value[PRODUCT_REMOVE_ADS] == PurchaseState.Purchased)
    }
    
    /**
     * 检查是否已购买高级功能（从本地读取）
     */
    fun isPremiumFeaturesPurchased(): Boolean {
        return sharedPrefs.getBoolean(PREFS_KEY_PREMIUM_FEATURES, false) || 
               (_purchaseStates.value[PRODUCT_PREMIUM_FEATURES] == PurchaseState.Purchased)
    }
    
    /**
     * 获取产品详情
     */
    fun getProductDetails(productId: String): ProductDetails? {
        return _productDetailsMap.value[productId]
    }
    
    /**
     * 获取产品购买状态
     */
    fun getPurchaseState(productId: String): PurchaseState {
        return _purchaseStates.value[productId] ?: PurchaseState.NotPurchased
    }
    
    /**
     * 生命周期：当 Activity 恢复时重新查询购买状态
     */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (_isReady.value) {
            queryPurchases()
        }
    }
    
    /**
     * 清理资源
     */
    fun release() {
        billingClient?.endConnection()
        billingClient = null
        _isReady.value = false
    }
}

/**
 * 购买状态枚举
 */
sealed class PurchaseState {
    object NotPurchased : PurchaseState()
    object Purchased : PurchaseState()
    object Purchasing : PurchaseState()
}

