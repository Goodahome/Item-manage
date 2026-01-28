package com.example.itemremindertool.network

import com.example.itemremindertool.network.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API 服务接口
 */
interface ApiService {
    
    // ==================== 认证相关 ====================
    
    /**
     * 注册
     */
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>
    
    /**
     * 登录
     */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>
    
    /**
     * 刷新 token
     */
    @POST("api/auth/refresh")
    suspend fun refreshToken(): Response<ApiResponse<Map<String, String>>>
    
    /**
     * 登出
     */
    @POST("api/auth/logout")
    suspend fun logout(): Response<ApiResponse<Map<String, String>>>

    // ==================== 媒体相关 ====================

    @POST("api/media/presign-upload")
    suspend fun presignUpload(@Body request: PresignUploadRequest): Response<ApiResponse<PresignUploadResponse>>

    @GET("api/media/presign-read")
    suspend fun presignRead(@Query("key") key: String): Response<ApiResponse<PresignReadResponse>>

    // ==================== 批量同步 ====================

    @POST("api/sync/bootstrap")
    suspend fun bootstrapSync(@Body request: SyncBootstrapRequest): Response<ApiResponse<SyncBootstrapResponse>>

    @POST("api/sync/bootstrap-ack")
    suspend fun bootstrapSyncAck(@Body request: SyncBootstrapAckRequest): Response<ApiResponse<SyncBootstrapAckResponse>>
    
    // ==================== 物品相关 ====================
    
    /**
     * 获取物品列表
     */
    @GET("api/items")
    suspend fun getItems(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
        @Query("search") search: String? = null,
        @Query("categoryUuid") categoryUuid: String? = null,
        @Query("warehouseUuid") warehouseUuid: String? = null
    ): Response<ApiResponse<ItemListResponse>>
    
    /**
     * 获取单个物品
     */
    @GET("api/items/{uuid}")
    suspend fun getItem(@Path("uuid") uuid: String): Response<ApiResponse<ItemDto>>
    
    /**
     * 创建或更新物品（upsert）
     */
    @POST("api/items")
    suspend fun upsertItem(@Body item: ItemDto): Response<ApiResponse<ItemDto>>
    
    /**
     * 删除物品
     */
    @DELETE("api/items/{uuid}")
    suspend fun deleteItem(@Path("uuid") uuid: String): Response<ApiResponse<Map<String, String>>>
    
    // ==================== 分类相关 ====================
    
    /**
     * 获取分类列表
     */
    @GET("api/categories")
    suspend fun getCategories(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<ApiResponse<CategoryListResponse>>
    
    /**
     * 获取单个分类
     */
    @GET("api/categories/{uuid}")
    suspend fun getCategory(@Path("uuid") uuid: String): Response<ApiResponse<CategoryDto>>
    
    /**
     * 创建或更新分类
     */
    @POST("api/categories")
    suspend fun upsertCategory(@Body category: CategoryDto): Response<ApiResponse<CategoryDto>>
    
    /**
     * 删除分类
     */
    @DELETE("api/categories/{uuid}")
    suspend fun deleteCategory(@Path("uuid") uuid: String): Response<ApiResponse<Map<String, String>>>
    
    // ==================== 容器相关 ====================
    
    /**
     * 获取容器列表
     */
    @GET("api/warehouses")
    suspend fun getWarehouses(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<ApiResponse<WarehouseListResponse>>
    
    /**
     * 获取单个容器
     */
    @GET("api/warehouses/{uuid}")
    suspend fun getWarehouse(@Path("uuid") uuid: String): Response<ApiResponse<WarehouseDto>>
    
    /**
     * 创建或更新容器
     */
    @POST("api/warehouses")
    suspend fun upsertWarehouse(@Body warehouse: WarehouseDto): Response<ApiResponse<WarehouseDto>>
    
    /**
     * 删除容器
     */
    @DELETE("api/warehouses/{uuid}")
    suspend fun deleteWarehouse(@Path("uuid") uuid: String): Response<ApiResponse<Map<String, String>>>
    
    // ==================== 购物清单相关 ====================
    
    /**
     * 获取购物清单列表
     */
    @GET("api/shopping-items")
    suspend fun getShoppingItems(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<ApiResponse<ShoppingItemListResponse>>
    
    /**
     * 获取单个购物项
     */
    @GET("api/shopping-items/{uuid}")
    suspend fun getShoppingItem(@Path("uuid") uuid: String): Response<ApiResponse<ShoppingItemDto>>
    
    /**
     * 创建或更新购物项
     */
    @POST("api/shopping-items")
    suspend fun upsertShoppingItem(@Body shoppingItem: ShoppingItemDto): Response<ApiResponse<ShoppingItemDto>>
    
    /**
     * 删除购物项
     */
    @DELETE("api/shopping-items/{uuid}")
    suspend fun deleteShoppingItem(@Path("uuid") uuid: String): Response<ApiResponse<Map<String, String>>>
}
