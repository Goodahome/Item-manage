package com.example.itemremindertool.sync

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import android.net.Uri
import android.os.Environment
import com.example.itemremindertool.auth.AuthManager
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.*
import com.example.itemremindertool.network.RetrofitClient
import com.example.itemremindertool.network.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 数据同步管理器 - 处理本地与远端的数据同步
 */
class SyncManager(private val context: Context) {
    
    private val authManager = AuthManager.getInstance(context)
    private val gson = com.google.gson.Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val uploadClient = OkHttpClient()
    private val syncPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    @Volatile
    private var isMergeRunning = false
    
    /**
     * 检查是否应该同步到远端
     */
    fun shouldSyncToRemote(): Boolean {
        return authManager.isLoggedIn()
    }

    /**
     * 登录后执行一次双向合并（云端 <-> 本地）
     * - Item/ShoppingItem/Warehouse 使用时间戳进行合并
     * - Category 无本地时间戳，若双方存在则保留本地不覆盖
     */
    suspend fun mergeRemoteAndLocalOnce(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!shouldSyncToRemote()) {
            return@withContext Result.success(Unit)
        }
        val now = System.currentTimeMillis()
        val nextRetryAt = syncPrefs.getLong(KEY_NEXT_RETRY_AT, 0L)
        if (nextRetryAt > now) {
            Log.d(TAG, "跳过合并：未到重试时间")
            return@withContext Result.success(Unit)
        }
        if (isMergeRunning) {
            Log.d(TAG, "跳过合并：已有合并进行中")
            return@withContext Result.success(Unit)
        }
        isMergeRunning = true

        try {
            val apiService = RetrofitClient.getApiService(context)
            val db = AppDatabase.getDatabase(context)
            val itemDao = db.itemDao()
            val categoryDao = db.categoryDao()
            val warehouseDao = db.warehouseDao()
            val shoppingItemDao = db.shoppingItemDao()
            val activityEventDao = db.activityEventDao()
            val syncQueueDao = db.syncQueueDao()

            if (!syncPrefs.getBoolean(KEY_BOOTSTRAP_COMPLETED, false)) {
                val bootstrapResult = runBootstrapSync(
                    apiService,
                    itemDao,
                    categoryDao,
                    warehouseDao,
                    shoppingItemDao,
                    activityEventDao,
                    syncQueueDao
                )
                if (bootstrapResult.isSuccess) {
                    syncPrefs.edit().putBoolean(KEY_BOOTSTRAP_COMPLETED, true).apply()
                    Log.d(TAG, "首次批量同步完成")
                    return@withContext Result.success(Unit)
                }
                Log.e(TAG, "批量同步失败，回退分页拉取")
            }

            val categoriesResult = fetchAllCategories(apiService)
            if (categoriesResult.rateLimited) {
                scheduleRetry(categoriesResult.retryAfterSec)
                return@withContext Result.failure(Exception("Rate limited"))
            }
            val warehousesResult = fetchAllWarehouses(apiService)
            if (warehousesResult.rateLimited) {
                scheduleRetry(warehousesResult.retryAfterSec)
                return@withContext Result.failure(Exception("Rate limited"))
            }
            val shoppingResult = fetchAllShoppingItems(apiService)
            if (shoppingResult.rateLimited) {
                scheduleRetry(shoppingResult.retryAfterSec)
                return@withContext Result.failure(Exception("Rate limited"))
            }
            val itemsResult = fetchAllItems(apiService)
            if (itemsResult.rateLimited) {
                scheduleRetry(itemsResult.retryAfterSec)
                return@withContext Result.failure(Exception("Rate limited"))
            }

            val remoteItems = itemsResult.items
            val remoteCategories = categoriesResult.items
            val remoteWarehouses = warehousesResult.items
            val remoteShoppingItems = shoppingResult.items

            val localItems = itemDao.getAllItemsList()
            val localCategories = categoryDao.getAllCategoriesSync()
            val localWarehouses = warehouseDao.getAllWarehousesSync()
            val localShoppingItems = shoppingItemDao.getAllShoppingItemsSync()

            val remoteItemsByUuid = remoteItems.associateBy { it.uuid }
            val remoteCategoriesByUuid = remoteCategories.associateBy { it.uuid }
            val remoteWarehousesByUuid = remoteWarehouses.associateBy { it.uuid }
            val remoteShoppingByUuid = remoteShoppingItems.associateBy { it.uuid }

            val localItemsByUuid = localItems.associateBy { it.uuid }
            val localCategoriesByUuid = localCategories.associateBy { it.uuid }
            val localWarehousesByUuid = localWarehouses.associateBy { it.uuid }
            val localShoppingByUuid = localShoppingItems.associateBy { it.uuid }

            // 先迁移本地旧 UUID（仅在云端为空时迁移，避免重复）
            if (remoteItems.isEmpty() && remoteWarehouses.isEmpty() && remoteShoppingItems.isEmpty()) {
                migrateLegacyUuidsIfNeeded(
                    itemDao = itemDao,
                    categoryDao = categoryDao,
                    warehouseDao = warehouseDao,
                    shoppingItemDao = shoppingItemDao,
                    syncQueueDao = syncQueueDao
                )
            }

            // Categories: 无本地时间戳，双方存在则保留本地
            for (remote in remoteCategories) {
                val local = localCategoriesByUuid[remote.uuid]
                if (local == null) {
                    categoryDao.insertCategory(categoryFromDto(remote, null))
                }
            }
            for (local in localCategories) {
                if (!remoteCategoriesByUuid.containsKey(local.uuid)) {
                    syncCategoryToRemote(local)
                }
            }

            // Warehouses: 仅有本地 createdAt，使用远端 updatedAt/createdAt 对比
            for (remote in remoteWarehouses) {
                val local = localWarehousesByUuid[remote.uuid]
                if (local == null) {
                    val entity = warehouseFromDto(remote, null)
                    val insertedId = warehouseDao.insertWarehouse(entity)
                    val inserted = warehouseDao.getWarehouseById(insertedId) ?: entity
                    if (remote.imageUri != null && inserted.imageUri.isNullOrBlank()) {
                        val localPath = downloadImage(apiService, remote.imageUri)
                        if (localPath != null) {
                            warehouseDao.updateWarehouse(inserted.copy(imageUri = localPath))
                        }
                    }
                } else {
                    val remoteTime = parseDateOrNull(remote.updatedAt) ?: parseDateOrNull(remote.createdAt)
                    if (isRemoteNewer(remoteTime, local.createdAt)) {
                        val entity = warehouseFromDto(remote, local)
                        warehouseDao.insertWarehouse(entity)
                        if (remote.imageUri != null && local.imageUri.isNullOrBlank()) {
                            val localPath = downloadImage(apiService, remote.imageUri)
                            if (localPath != null) {
                                warehouseDao.updateWarehouse(entity.copy(imageUri = localPath))
                            }
                        }
                    } else if (local.createdAt.after(remoteTime ?: Date(0))) {
                        syncWarehouseToRemote(local)
                    }
                }
            }
            for (local in localWarehouses) {
                if (!remoteWarehousesByUuid.containsKey(local.uuid) && !local.isSample) {
                    syncWarehouseToRemote(local)
                }
            }

            // Items: 双向合并（先尝试用内容匹配去重）
            val localItemsToSkipUpload = mutableSetOf<String>()
            if (remoteItems.isNotEmpty()) {
                val remoteIndex = remoteItems.groupBy { buildItemSignature(it) }
                for (local in localItems) {
                    if (remoteItemsByUuid.containsKey(local.uuid)) continue
                    val signature = buildItemSignature(local, warehouseDao, categoryDao)
                    val candidates = remoteIndex[signature] ?: emptyList()
                    var matched: ItemDto? = null
                    for (remote in candidates) {
                        if (isSameItem(remote, local, warehouseDao, categoryDao)) {
                            matched = remote
                            break
                        }
                    }
                    if (matched != null) {
                        itemDao.updateItem(local.copy(uuid = matched.uuid))
                        syncQueueDao.deleteByUuid(local.uuid)
                        localItemsToSkipUpload.add(matched.uuid)
                    }
                }
            }

            for (remote in remoteItems) {
                val local = localItemsByUuid[remote.uuid]
                if (local == null) {
                    val entity = itemFromDto(remote, null, warehouseDao, categoryDao)
                    val insertedId = itemDao.insertItem(entity)
                    val inserted = itemDao.getItemById(insertedId) ?: entity
                    downloadRemoteImagesIfNeeded(inserted, remote.imageUris ?: emptyList(), itemDao)
                } else {
                    val remoteTime = parseDateOrNull(remote.updatedAt) ?: parseDateOrNull(remote.createdAt)
                    if (isRemoteNewer(remoteTime, local.updatedAt)) {
                        val entity = itemFromDto(remote, local, warehouseDao, categoryDao)
                        itemDao.insertItem(entity)
                        downloadRemoteImagesIfNeeded(entity, remote.imageUris ?: emptyList(), itemDao)
                    } else if (local.updatedAt.after(remoteTime ?: Date(0))) {
                        syncItemToRemote(local)
                    }
                }
            }
            for (local in localItems) {
                if (!remoteItemsByUuid.containsKey(local.uuid) && !local.isSample && !localItemsToSkipUpload.contains(local.uuid)) {
                    syncItemToRemote(local)
                }
            }

            // ShoppingItems: 使用 completedAt / createdAt 作为对比时间
            for (remote in remoteShoppingItems) {
                val local = localShoppingByUuid[remote.uuid]
                if (local == null) {
                    val itemId = resolveItemIdByUuid(itemDao, remote.itemUuid)
                    val entity = shoppingItemFromDto(remote, null, itemId)
                    val insertedId = shoppingItemDao.insertShoppingItem(entity)
                    val inserted = shoppingItemDao.getShoppingItemById(insertedId) ?: entity
                    if (remote.imageUri != null && inserted.imageUri.isNullOrBlank()) {
                        val localPath = downloadImage(apiService, remote.imageUri)
                        if (localPath != null) {
                            shoppingItemDao.updateShoppingItem(inserted.copy(imageUri = localPath))
                        }
                    }
                } else {
                    val remoteTime = parseDateOrNull(remote.completedAt) ?: parseDateOrNull(remote.createdAt)
                    val localTime = local.completedAt ?: local.createdAt
                    if (isRemoteNewer(remoteTime, localTime)) {
                        val itemId = resolveItemIdByUuid(itemDao, remote.itemUuid) ?: local.itemId
                        val entity = shoppingItemFromDto(remote, local, itemId)
                        shoppingItemDao.insertShoppingItem(entity)
                        if (remote.imageUri != null && local.imageUri.isNullOrBlank()) {
                            val localPath = downloadImage(apiService, remote.imageUri)
                            if (localPath != null) {
                                shoppingItemDao.updateShoppingItem(entity.copy(imageUri = localPath))
                            }
                        }
                    } else if (localTime.after(remoteTime ?: Date(0))) {
                        syncShoppingItemToRemote(local)
                    }
                }
            }
            for (local in localShoppingItems) {
                if (!remoteShoppingByUuid.containsKey(local.uuid) && !local.isSample) {
                    syncShoppingItemToRemote(local)
                }
            }

            Log.d(TAG, "登录后双向合并完成")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "登录后双向合并失败", e)
            Result.failure(e)
        } finally {
            isMergeRunning = false
        }
    }

    private suspend fun migrateLegacyUuidsIfNeeded(
        itemDao: com.example.itemremindertool.data.dao.ItemDao,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao,
        shoppingItemDao: com.example.itemremindertool.data.dao.ShoppingItemDao,
        syncQueueDao: com.example.itemremindertool.data.dao.SyncQueueDao
    ) {
        val legacyItems = itemDao.getAllItemsList().filter { !isValidUuid(it.uuid) }
        val legacyCategories = categoryDao.getAllCategoriesSync().filter { !isValidUuid(it.uuid) }
        val legacyWarehouses = warehouseDao.getAllWarehousesSync().filter { !isValidUuid(it.uuid) }
        val legacyShoppingItems = shoppingItemDao.getAllShoppingItemsSync().filter { !isValidUuid(it.uuid) }

        if (legacyItems.isEmpty() &&
            legacyCategories.isEmpty() &&
            legacyWarehouses.isEmpty() &&
            legacyShoppingItems.isEmpty()
        ) {
            return
        }

        Log.d(TAG, "开始迁移旧 UUID 数据")

        legacyItems.forEach { item ->
            val oldUuid = item.uuid
            val newUuid = UUID.randomUUID().toString()
            val updated = item.copy(uuid = newUuid)
            itemDao.updateItem(updated)
            syncQueueDao.deleteByUuid(oldUuid)
            addToOfflineQueue("item", oldUuid, SyncOperation.DELETE, mapOf("uuid" to oldUuid))
            syncItemToRemote(updated)
        }

        legacyCategories.forEach { category ->
            val oldUuid = category.uuid
            val newUuid = UUID.randomUUID().toString()
            val updated = category.copy(uuid = newUuid)
            categoryDao.updateCategory(updated)
            syncQueueDao.deleteByUuid(oldUuid)
            addToOfflineQueue("category", oldUuid, SyncOperation.DELETE, mapOf("uuid" to oldUuid))
            syncCategoryToRemote(updated)
        }

        legacyWarehouses.forEach { warehouse ->
            val oldUuid = warehouse.uuid
            val newUuid = UUID.randomUUID().toString()
            val updated = warehouse.copy(uuid = newUuid)
            warehouseDao.updateWarehouse(updated)
            syncQueueDao.deleteByUuid(oldUuid)
            addToOfflineQueue("warehouse", oldUuid, SyncOperation.DELETE, mapOf("uuid" to oldUuid))
            syncWarehouseToRemote(updated)
        }

        legacyShoppingItems.forEach { shoppingItem ->
            val oldUuid = shoppingItem.uuid
            val newUuid = UUID.randomUUID().toString()
            val updated = shoppingItem.copy(uuid = newUuid)
            shoppingItemDao.updateShoppingItem(updated)
            syncQueueDao.deleteByUuid(oldUuid)
            addToOfflineQueue("shopping_item", oldUuid, SyncOperation.DELETE, mapOf("uuid" to oldUuid))
            syncShoppingItemToRemote(updated)
        }

        Log.d(TAG, "旧 UUID 迁移完成")
    }
    
    // ==================== Item 同步 ====================
    
    /**
     * 同步物品到远端
     */
    suspend fun syncItemToRemote(item: Item): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            if (item.isSample) {
                return@withContext Result.success(Unit)
            }
            val itemDao = AppDatabase.getDatabase(context).itemDao()
            val preparedItem = ensureRemoteImageKeys(item, itemDao)
            val apiService = RetrofitClient.getApiService(context)
        val warehouseDao = AppDatabase.getDatabase(context).warehouseDao()
        val categoryDao = AppDatabase.getDatabase(context).categoryDao()
        val dto = itemToDto(preparedItem, warehouseDao, categoryDao)
            val response = apiService.upsertItem(dto)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "物品同步成功: ${preparedItem.name}")
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "同步失败"
                Log.e(TAG, "物品同步失败: $error")
                // 添加到离线队列
                addToOfflineQueue("item", preparedItem.uuid, SyncOperation.UPDATE, preparedItem)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "物品同步异常", e)
            // 添加到离线队列
            addToOfflineQueue("item", item.uuid, SyncOperation.UPDATE, item)
            Result.failure(e)
        }
    }
    
    /**
     * 从远端删除物品
     */
    suspend fun deleteItemFromRemote(uuid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            
            val apiService = RetrofitClient.getApiService(context)
            val response = apiService.deleteItem(uuid)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "物品删除同步成功: $uuid")
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "删除同步失败"
                Log.e(TAG, "物品删除同步失败: $error")
                // 添加到离线队列
                addToOfflineQueue("item", uuid, SyncOperation.DELETE, mapOf("uuid" to uuid))
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "物品删除同步异常", e)
            // 添加到离线队列
            addToOfflineQueue("item", uuid, SyncOperation.DELETE, mapOf("uuid" to uuid))
            Result.failure(e)
        }
    }
    
    private suspend fun ensureRemoteImageKeys(
        item: Item,
        itemDao: com.example.itemremindertool.data.dao.ItemDao
    ): Item {
        if (item.imageKeys.isNotEmpty() || item.imageUris.isEmpty()) {
            return item
        }

        val apiService = RetrofitClient.getApiService(context)
        val uploadedKeys = mutableListOf<String>()

        for (path in item.imageUris) {
            val uploadResult = uploadImage(apiService, path, item.uuid)
            if (uploadResult != null) {
                uploadedKeys.add(uploadResult)
            }
        }

        if (uploadedKeys.isEmpty()) {
            return item
        }

        val updated = item.copy(imageKeys = uploadedKeys)
        itemDao.updateItem(updated)
        return updated
    }

    private suspend fun uploadImage(
        apiService: com.example.itemremindertool.network.ApiService,
        localPath: String,
        itemUuid: String
    ): String? {
        val bytes = readBytes(localPath) ?: return null
        val mimeType = resolveMimeType(localPath) ?: return null

        val presignResponse = apiService.presignUpload(
            PresignUploadRequest(
                mimeType = mimeType,
                fileSize = bytes.size.toLong(),
                itemUuid = itemUuid
            )
        )
        if (!presignResponse.isSuccessful || presignResponse.body()?.success != true) {
            Log.e(TAG, "获取上传签名失败: ${presignResponse.code()}")
            return null
        }

        val data = presignResponse.body()?.data ?: return null
        val requestBody = RequestBody.create(mimeType.toMediaTypeOrNull(), bytes)
        val requestBuilder = Request.Builder()
            .url(data.uploadUrl)
            .put(requestBody)
        data.requiredHeaders?.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }
        val request = requestBuilder.build()

        uploadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "图片上传失败: ${response.code}")
                return null
            }
        }

        return data.objectKey
    }

    private suspend fun downloadRemoteImagesIfNeeded(
        item: Item,
        imageKeys: List<String>,
        itemDao: com.example.itemremindertool.data.dao.ItemDao
    ) {
        if (imageKeys.isEmpty() || item.imageUris.isNotEmpty()) {
            return
        }

        val apiService = RetrofitClient.getApiService(context)
        val downloaded = mutableListOf<String>()
        val remoteKeys = imageKeys.filter { isRemoteObjectKey(it) }
        for (key in remoteKeys) {
            val localPath = downloadImage(apiService, key)
            if (localPath != null) {
                downloaded.add(localPath)
            }
        }

        if (downloaded.isEmpty()) {
            return
        }

        val updated = item.copy(
            imageUri = downloaded.firstOrNull(),
            imageUris = downloaded
        )
        itemDao.updateItem(updated)
    }

    private suspend fun ensureWarehouseImageKey(
        warehouse: Warehouse,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao
    ): Warehouse {
        if (!warehouse.imageKey.isNullOrBlank() || warehouse.imageUri.isNullOrBlank()) {
            return warehouse
        }
        val apiService = RetrofitClient.getApiService(context)
        val key = uploadImage(apiService, warehouse.imageUri, warehouse.uuid) ?: return warehouse
        val updated = warehouse.copy(imageKey = key)
        warehouseDao.updateWarehouse(updated)
        return updated
    }

    private suspend fun ensureShoppingItemImageKey(
        shoppingItem: ShoppingItem,
        shoppingItemDao: com.example.itemremindertool.data.dao.ShoppingItemDao
    ): ShoppingItem {
        if (!shoppingItem.imageKey.isNullOrBlank() || shoppingItem.imageUri.isNullOrBlank()) {
            return shoppingItem
        }
        val apiService = RetrofitClient.getApiService(context)
        val key = uploadImage(apiService, shoppingItem.imageUri, shoppingItem.uuid) ?: return shoppingItem
        val updated = shoppingItem.copy(imageKey = key)
        shoppingItemDao.updateShoppingItem(updated)
        return updated
    }

    private suspend fun downloadImage(
        apiService: com.example.itemremindertool.network.ApiService,
        objectKey: String
    ): String? {
        if (!isRemoteObjectKey(objectKey)) {
            return null
        }
        val response = apiService.presignRead(objectKey)
        if (!response.isSuccessful || response.body()?.success != true) {
            Log.e(TAG, "获取下载签名失败: ${response.code()}")
            return null
        }
        val data = response.body()?.data ?: return null
        val request = Request.Builder()
            .url(data.signedUrl)
            .get()
            .build()

        uploadClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.e(TAG, "下载图片失败: ${resp.code}")
                return null
            }
            val bytes = resp.body?.bytes() ?: return null
            return writeCacheFile(objectKey, bytes)
        }
    }

    private fun writeCacheFile(objectKey: String, bytes: ByteArray): String? {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (externalDir != null) {
            val dir = File(externalDir, "ItemReminderTool")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val extension = objectKey.substringAfterLast('.', "img")
            val fileName = sha256Hex(objectKey) + "." + extension
            val file = File(dir, fileName)
            return try {
                file.writeBytes(bytes)
                file.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "写入默认图片目录失败", e)
                null
            }
        }
        return try {
            val dir = File(context.cacheDir, "remote_images")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val extension = objectKey.substringAfterLast('.', "img")
            val fileName = sha256Hex(objectKey) + "." + extension
            val file = File(dir, fileName)
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "写入缓存图片失败", e)
            null
        }
    }

    private fun sha256Hex(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun resolveMimeType(path: String): String? {
        return if (path.startsWith("content://")) {
            context.contentResolver.getType(Uri.parse(path))
        } else {
            val extension = MimeTypeMap.getFileExtensionFromUrl(path).lowercase(Locale.US)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: when (extension) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> null
                }
        }
    }

    private fun readBytes(path: String): ByteArray? {
        return try {
            val inputStream: InputStream? = if (path.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(path))
            } else {
                val file = File(path)
                if (!file.exists()) return null
                file.inputStream()
            }
            inputStream?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "读取图片失败: $path", e)
            null
        }
    }

    // ==================== Category 同步 ====================
    
    /**
     * 同步分类到远端
     */
    suspend fun syncCategoryToRemote(category: Category): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            
            val apiService = RetrofitClient.getApiService(context)
            val dto = categoryToDto(category)
            val response = apiService.upsertCategory(dto)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "分类同步成功: ${category.name}")
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "同步失败"
                Log.e(TAG, "分类同步失败: $error")
                // 添加到离线队列
                addToOfflineQueue("category", category.uuid, SyncOperation.UPDATE, category)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "分类同步异常", e)
            // 添加到离线队列
            addToOfflineQueue("category", category.uuid, SyncOperation.UPDATE, category)
            Result.failure(e)
        }
    }
    
    /**
     * 从远端删除分类
     */
    suspend fun deleteCategoryFromRemote(uuid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            
            val apiService = RetrofitClient.getApiService(context)
            val response = apiService.deleteCategory(uuid)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "分类删除同步成功: $uuid")
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "删除同步失败"
                Log.e(TAG, "分类删除同步失败: $error")
                // 添加到离线队列（删除操作，entity 传递 uuid）
                addToOfflineQueue("category", uuid, SyncOperation.DELETE, mapOf("uuid" to uuid))
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "分类删除同步异常", e)
            // 添加到离线队列
            addToOfflineQueue("category", uuid, SyncOperation.DELETE, mapOf("uuid" to uuid))
            Result.failure(e)
        }
    }
    
    // ==================== Warehouse 同步 ====================
    
    /**
     * 同步容器到远端
     */
    suspend fun syncWarehouseToRemote(warehouse: Warehouse): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            if (warehouse.isSample) {
                return@withContext Result.success(Unit)
            }
            val warehouseDao = AppDatabase.getDatabase(context).warehouseDao()
            val preparedWarehouse = ensureWarehouseImageKey(warehouse, warehouseDao)
            
            val apiService = RetrofitClient.getApiService(context)
            val dto = warehouseToDto(preparedWarehouse)
            val response = apiService.upsertWarehouse(dto)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "容器同步成功: ${preparedWarehouse.name}")
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "同步失败"
                Log.e(TAG, "容器同步失败: $error")
                // 添加到离线队列
                addToOfflineQueue("warehouse", preparedWarehouse.uuid, SyncOperation.UPDATE, preparedWarehouse)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "容器同步异常", e)
            // 添加到离线队列
            addToOfflineQueue("warehouse", warehouse.uuid, SyncOperation.UPDATE, warehouse)
            Result.failure(e)
        }
    }
    
    /**
     * 从远端删除容器
     */
    suspend fun deleteWarehouseFromRemote(uuid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            
            val apiService = RetrofitClient.getApiService(context)
            val response = apiService.deleteWarehouse(uuid)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "容器删除同步成功: $uuid")
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "删除同步失败"
                Log.e(TAG, "容器删除同步失败: $error")
                // 添加到离线队列
                addToOfflineQueue("warehouse", uuid, SyncOperation.DELETE, mapOf("uuid" to uuid))
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "容器删除同步异常", e)
            // 添加到离线队列
            addToOfflineQueue("warehouse", uuid, SyncOperation.DELETE, mapOf("uuid" to uuid))
            Result.failure(e)
        }
    }
    
    // ==================== 数据转换方法 ====================
    
    private suspend fun itemToDto(
        item: Item,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao
    ): ItemDto {
        val warehouseUuid = item.warehouseId?.let { id ->
            warehouseDao.getWarehouseById(id)?.uuid
        }
        val categoryUuid = item.categoryId?.let { id ->
            categoryDao.getCategoryById(id)?.uuid
        }
        return ItemDto(
            uuid = item.uuid,
            name = item.name,
            description = item.description,
            categoryId = null,
            categoryUuid = categoryUuid,
            warehouseId = null,
            warehouseUuid = warehouseUuid,
            tags = item.tags,
            purchaseDate = item.purchaseDate?.let { dateFormat.format(it) },
            expiryDate = item.expiryDate?.let { dateFormat.format(it) },
            price = item.price,
            quantity = item.quantity,
            barcode = item.barcode,
            imageUri = item.imageUri,
            imageUris = item.imageKeys,
            primaryImageIndex = item.primaryImageIndex,
            featureCode = item.featureCode,
            enableStockAlert = item.enableStockAlert,
            createdAt = dateFormat.format(item.createdAt),
            updatedAt = dateFormat.format(item.updatedAt)
        )
    }
    
    private fun categoryToDto(category: Category): CategoryDto {
        return CategoryDto(
            uuid = category.uuid,
            name = category.name,
            description = category.description,
            color = category.color,
            icon = category.icon
        )
    }
    
    private fun warehouseToDto(warehouse: Warehouse): WarehouseDto {
        return WarehouseDto(
            uuid = warehouse.uuid,
            name = warehouse.name,
            description = warehouse.description,
            location = warehouse.location,
            capacity = warehouse.capacity,
            parentId = warehouse.parentId?.toInt(),
            level = warehouse.level,
            imageUri = warehouse.imageKey,
            createdAt = dateFormat.format(warehouse.createdAt)
        )
    }

    private suspend fun itemFromDto(
        dto: ItemDto,
        existing: Item?,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao
    ): Item {
        val createdAt = parseDateOrNull(dto.createdAt) ?: existing?.createdAt ?: Date()
        val updatedAt = parseDateOrNull(dto.updatedAt) ?: existing?.updatedAt ?: createdAt
        val resolvedWarehouseId = resolveWarehouseId(dto, warehouseDao)
        val resolvedCategoryId = resolveCategoryId(dto, categoryDao)
        return Item(
            id = existing?.id ?: 0,
            uuid = dto.uuid,
            name = dto.name,
            description = dto.description ?: existing?.description ?: "",
            categoryId = resolvedCategoryId ?: existing?.categoryId,
            warehouseId = resolvedWarehouseId ?: existing?.warehouseId,
            tags = dto.tags ?: existing?.tags ?: emptyList(),
            purchaseDate = parseDateOrNull(dto.purchaseDate),
            expiryDate = parseDateOrNull(dto.expiryDate),
            price = dto.price,
            quantity = dto.quantity ?: existing?.quantity ?: 1,
            barcode = dto.barcode ?: existing?.barcode,
            imageUri = dto.imageUri ?: existing?.imageUri,
            imageUris = existing?.imageUris ?: emptyList(),
            imageKeys = filterRemoteKeys(dto.imageUris) ?: existing?.imageKeys ?: emptyList(),
            isSample = existing?.isSample ?: false,
            primaryImageIndex = dto.primaryImageIndex ?: existing?.primaryImageIndex ?: 0,
            featureCode = dto.featureCode ?: existing?.featureCode,
            enableStockAlert = dto.enableStockAlert ?: existing?.enableStockAlert ?: true,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun categoryFromDto(dto: CategoryDto, existing: Category?): Category {
        return Category(
            id = existing?.id ?: 0,
            uuid = dto.uuid,
            name = dto.name,
            description = dto.description ?: existing?.description ?: "",
            color = dto.color ?: existing?.color ?: "#6200EE",
            icon = dto.icon ?: existing?.icon ?: "category"
        )
    }

    private fun warehouseFromDto(dto: WarehouseDto, existing: Warehouse?): Warehouse {
        val createdAt = parseDateOrNull(dto.createdAt) ?: existing?.createdAt ?: Date()
        return Warehouse(
            id = existing?.id ?: 0,
            uuid = dto.uuid,
            name = dto.name,
            description = dto.description ?: existing?.description ?: "",
            location = dto.location ?: existing?.location ?: "",
            capacity = dto.capacity ?: existing?.capacity,
            parentId = dto.parentId?.toLong() ?: existing?.parentId,
            level = dto.level ?: existing?.level ?: 1,
            imageUri = existing?.imageUri,
            imageKey = dto.imageUri ?: existing?.imageKey,
            createdAt = createdAt,
            isSample = existing?.isSample ?: false
        )
    }

    private fun shoppingItemFromDto(
        dto: ShoppingItemDto,
        existing: ShoppingItem?,
        resolvedItemId: Long?
    ): ShoppingItem {
        val createdAt = parseDateOrNull(dto.createdAt) ?: existing?.createdAt ?: Date()
        val completedAt = parseDateOrNull(dto.completedAt) ?: existing?.completedAt
        val priority = runCatching {
            Priority.valueOf(dto.priority ?: existing?.priority?.name ?: "MEDIUM")
        }.getOrElse { Priority.MEDIUM }
        return ShoppingItem(
            id = existing?.id ?: 0,
            uuid = dto.uuid,
            name = dto.name,
            description = dto.description ?: existing?.description ?: "",
            quantity = dto.quantity ?: existing?.quantity ?: 1,
            isCompleted = dto.isCompleted ?: existing?.isCompleted ?: false,
            priority = priority,
            createdAt = createdAt,
            completedAt = completedAt,
            imageUri = existing?.imageUri,
            imageKey = dto.imageUri ?: existing?.imageKey,
            itemId = resolvedItemId ?: existing?.itemId,
            isSample = existing?.isSample ?: false
        )
    }

    private fun parseDateOrNull(value: String?): Date? {
        if (value.isNullOrBlank()) return null
        return runCatching { dateFormat.parse(value) }.getOrNull()
    }

    private fun isValidUuid(value: String): Boolean {
        return UUID_REGEX.matches(value)
    }

    private fun isRemoteObjectKey(value: String): Boolean {
        return value.startsWith("users/")
    }

    private fun filterRemoteKeys(keys: List<String>?): List<String>? {
        if (keys.isNullOrEmpty()) return keys
        val filtered = keys.filter { isRemoteObjectKey(it) }
        return filtered.ifEmpty { null }
    }

    private fun isRemoteNewer(remote: Date?, local: Date?): Boolean {
        if (remote == null) return false
        if (local == null) return true
        return remote.after(local)
    }

    private suspend fun resolveItemIdByUuid(itemDao: com.example.itemremindertool.data.dao.ItemDao, uuid: String?): Long? {
        if (uuid.isNullOrBlank()) return null
        return itemDao.getItemByUuid(uuid)?.id
    }

    private suspend fun resolveWarehouseId(
        dto: ItemDto,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao
    ): Long? {
        dto.warehouseUuid?.let { uuid ->
            return warehouseDao.getWarehouseByUuid(uuid)?.id
        }
        return dto.warehouseId?.toLong()
    }

    private suspend fun resolveCategoryId(
        dto: ItemDto,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao
    ): Long? {
        dto.categoryUuid?.let { uuid ->
            return categoryDao.getCategoryByUuid(uuid)?.id
        }
        return dto.categoryId?.toLong()
    }

    private fun buildItemSignature(dto: ItemDto): String {
        val createdAt = dto.createdAt ?: ""
        return listOf(
            dto.name.trim(),
            createdAt,
            dto.warehouseUuid ?: "",
            dto.categoryUuid ?: ""
        ).joinToString("|")
    }

    private suspend fun buildItemSignature(
        item: Item,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao
    ): String {
        val warehouseUuid = item.warehouseId?.let { warehouseDao.getWarehouseById(it)?.uuid } ?: ""
        val categoryUuid = item.categoryId?.let { categoryDao.getCategoryById(it)?.uuid } ?: ""
        return listOf(
            item.name.trim(),
            dateFormat.format(item.createdAt),
            warehouseUuid,
            categoryUuid
        ).joinToString("|")
    }

    private suspend fun isSameItem(
        remote: ItemDto,
        local: Item,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao
    ): Boolean {
        if (remote.name.trim() != local.name.trim()) return false
        val remoteTime = parseDateOrNull(remote.createdAt) ?: return false
        val diff = kotlin.math.abs(remoteTime.time - local.createdAt.time)
        if (diff > 60_000) return false
        val remoteWarehouseUuid = remote.warehouseUuid
        val localWarehouseUuid = local.warehouseId?.let { warehouseDao.getWarehouseById(it)?.uuid }
        if (remoteWarehouseUuid != null && localWarehouseUuid != null && remoteWarehouseUuid != localWarehouseUuid) {
            return false
        }
        val remoteCategoryUuid = remote.categoryUuid
        val localCategoryUuid = local.categoryId?.let { categoryDao.getCategoryById(it)?.uuid }
        if (remoteCategoryUuid != null && localCategoryUuid != null && remoteCategoryUuid != localCategoryUuid) {
            return false
        }
        return true
    }

    companion object {
        private const val TAG = "SyncManager"
        private const val KEY_NEXT_RETRY_AT = "next_retry_at"
        private const val KEY_RESUME_ITEMS_PAGE = "resume_items_page"
        private const val KEY_RESUME_CATEGORIES_PAGE = "resume_categories_page"
        private const val KEY_RESUME_WAREHOUSES_PAGE = "resume_warehouses_page"
        private const val KEY_RESUME_SHOPPING_PAGE = "resume_shopping_page"
        private const val KEY_BOOTSTRAP_COMPLETED = "bootstrap_completed"
        private const val KEY_SETTINGS_UPDATED_AT = "settings_updated_at"
        private val UUID_REGEX =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")

        @Volatile
        private var instance: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return instance ?: synchronized(this) {
                instance ?: SyncManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private suspend fun fetchAllItems(apiService: com.example.itemremindertool.network.ApiService): FetchResult<ItemDto> {
        val items = mutableListOf<ItemDto>()
        var page = syncPrefs.getInt(KEY_RESUME_ITEMS_PAGE, 1).coerceAtLeast(1)
        val pageSize = 200
        while (true) {
            val response = apiService.getItems(page = page, pageSize = pageSize)
            if (response.code() == 429) {
                syncPrefs.edit().putInt(KEY_RESUME_ITEMS_PAGE, page).apply()
                return FetchResult(items, true, response.headers()["Retry-After"]?.toLongOrNull() ?: 60L)
            }
            if (!response.isSuccessful || response.body()?.success != true) {
                Log.e(TAG, "拉取物品失败：${response.code()}")
                break
            }
            val data = response.body()?.data ?: break
            items.addAll(data.items)
            if (data.page * data.pageSize >= data.total) {
                syncPrefs.edit().remove(KEY_RESUME_ITEMS_PAGE).apply()
                break
            }
            page += 1
            syncPrefs.edit().putInt(KEY_RESUME_ITEMS_PAGE, page).apply()
        }
        return FetchResult(items, false, null)
    }

    private suspend fun fetchAllCategories(apiService: com.example.itemremindertool.network.ApiService): FetchResult<CategoryDto> {
        val categories = mutableListOf<CategoryDto>()
        var page = syncPrefs.getInt(KEY_RESUME_CATEGORIES_PAGE, 1).coerceAtLeast(1)
        val pageSize = 200
        while (true) {
            val response = apiService.getCategories(page = page, pageSize = pageSize)
            if (response.code() == 429) {
                syncPrefs.edit().putInt(KEY_RESUME_CATEGORIES_PAGE, page).apply()
                return FetchResult(categories, true, response.headers()["Retry-After"]?.toLongOrNull() ?: 60L)
            }
            if (!response.isSuccessful || response.body()?.success != true) {
                Log.e(TAG, "拉取分类失败：${response.code()}")
                break
            }
            val data = response.body()?.data ?: break
            categories.addAll(data.categories)
            if (data.page * data.pageSize >= data.total) {
                syncPrefs.edit().remove(KEY_RESUME_CATEGORIES_PAGE).apply()
                break
            }
            page += 1
            syncPrefs.edit().putInt(KEY_RESUME_CATEGORIES_PAGE, page).apply()
        }
        return FetchResult(categories, false, null)
    }

    private suspend fun fetchAllWarehouses(apiService: com.example.itemremindertool.network.ApiService): FetchResult<WarehouseDto> {
        val warehouses = mutableListOf<WarehouseDto>()
        var page = syncPrefs.getInt(KEY_RESUME_WAREHOUSES_PAGE, 1).coerceAtLeast(1)
        val pageSize = 200
        while (true) {
            val response = apiService.getWarehouses(page = page, pageSize = pageSize)
            if (response.code() == 429) {
                syncPrefs.edit().putInt(KEY_RESUME_WAREHOUSES_PAGE, page).apply()
                return FetchResult(warehouses, true, response.headers()["Retry-After"]?.toLongOrNull() ?: 60L)
            }
            if (!response.isSuccessful || response.body()?.success != true) {
                Log.e(TAG, "拉取容器失败：${response.code()}")
                break
            }
            val data = response.body()?.data ?: break
            warehouses.addAll(data.warehouses)
            if (data.page * data.pageSize >= data.total) {
                syncPrefs.edit().remove(KEY_RESUME_WAREHOUSES_PAGE).apply()
                break
            }
            page += 1
            syncPrefs.edit().putInt(KEY_RESUME_WAREHOUSES_PAGE, page).apply()
        }
        return FetchResult(warehouses, false, null)
    }

    private suspend fun fetchAllShoppingItems(apiService: com.example.itemremindertool.network.ApiService): FetchResult<ShoppingItemDto> {
        val shoppingItems = mutableListOf<ShoppingItemDto>()
        var page = syncPrefs.getInt(KEY_RESUME_SHOPPING_PAGE, 1).coerceAtLeast(1)
        val pageSize = 200
        while (true) {
            val response = apiService.getShoppingItems(page = page, pageSize = pageSize)
            if (response.code() == 429) {
                syncPrefs.edit().putInt(KEY_RESUME_SHOPPING_PAGE, page).apply()
                return FetchResult(shoppingItems, true, response.headers()["Retry-After"]?.toLongOrNull() ?: 60L)
            }
            if (!response.isSuccessful || response.body()?.success != true) {
                Log.e(TAG, "拉取购物清单失败：${response.code()}")
                break
            }
            val data = response.body()?.data ?: break
            shoppingItems.addAll(data.shoppingItems)
            if (data.page * data.pageSize >= data.total) {
                syncPrefs.edit().remove(KEY_RESUME_SHOPPING_PAGE).apply()
                break
            }
            page += 1
            syncPrefs.edit().putInt(KEY_RESUME_SHOPPING_PAGE, page).apply()
        }
        return FetchResult(shoppingItems, false, null)
    }

    private fun scheduleRetry(retryAfterSec: Long?) {
        val delaySeconds = retryAfterSec ?: 60L
        syncPrefs.edit()
            .putLong(KEY_NEXT_RETRY_AT, System.currentTimeMillis() + delaySeconds * 1000)
            .apply()
    }

    private data class FetchResult<T>(
        val items: List<T>,
        val rateLimited: Boolean,
        val retryAfterSec: Long?
    )

    private suspend fun runBootstrapSync(
        apiService: com.example.itemremindertool.network.ApiService,
        itemDao: com.example.itemremindertool.data.dao.ItemDao,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao,
        shoppingItemDao: com.example.itemremindertool.data.dao.ShoppingItemDao,
        activityEventDao: com.example.itemremindertool.data.dao.ActivityEventDao,
        syncQueueDao: com.example.itemremindertool.data.dao.SyncQueueDao
    ): Result<Unit> {
        val request = buildBootstrapRequest(
            itemDao,
            categoryDao,
            warehouseDao,
            shoppingItemDao,
            activityEventDao
        )
        val response = apiService.bootstrapSync(request)
        if (!response.isSuccessful || response.body()?.success != true) {
            return Result.failure(Exception("bootstrap failed"))
        }
        val payload = response.body()?.data ?: return Result.failure(Exception("bootstrap empty"))

        applyBootstrapPayload(
            payload.toApply,
            itemDao,
            categoryDao,
            warehouseDao,
            shoppingItemDao,
            activityEventDao
        )

        val ack = buildBootstrapAck(
            payload.toUpload,
            itemDao,
            categoryDao,
            warehouseDao,
            shoppingItemDao,
            activityEventDao
        )
        if (ack != null) {
            val ackResponse = apiService.bootstrapSyncAck(ack)
            if (!ackResponse.isSuccessful || ackResponse.body()?.success != true) {
                return Result.failure(Exception("bootstrap ack failed"))
            }
        }

        return Result.success(Unit)
    }

    private suspend fun buildBootstrapRequest(
        itemDao: com.example.itemremindertool.data.dao.ItemDao,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao,
        shoppingItemDao: com.example.itemremindertool.data.dao.ShoppingItemDao,
        activityEventDao: com.example.itemremindertool.data.dao.ActivityEventDao
    ): SyncBootstrapRequest {
        val items = itemDao.getAllItemsList().map {
            SyncSnapshotEntry(it.uuid, dateFormat.format(it.updatedAt))
        }
        val categories = categoryDao.getAllCategoriesSync().map {
            SyncSnapshotEntry(it.uuid, null)
        }
        val warehouses = warehouseDao.getAllWarehousesSync().map {
            SyncSnapshotEntry(it.uuid, dateFormat.format(it.createdAt))
        }
        val shoppingItems = shoppingItemDao.getAllShoppingItemsSync().map {
            val updatedAt = it.completedAt ?: it.createdAt
            SyncSnapshotEntry(it.uuid, dateFormat.format(updatedAt))
        }
        val events = activityEventDao.getAllEventsSync().map {
            SyncSnapshotEntry(it.uuid, dateFormat.format(it.createdAt))
        }
        val settings = readSettingsSnapshot()

        return SyncBootstrapRequest(
            items = items,
            categories = categories,
            warehouses = warehouses,
            shoppingItems = shoppingItems,
            activityEvents = events,
            settings = settings
        )
    }

    private suspend fun applyBootstrapPayload(
        payload: SyncPayload,
        itemDao: com.example.itemremindertool.data.dao.ItemDao,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao,
        shoppingItemDao: com.example.itemremindertool.data.dao.ShoppingItemDao,
        activityEventDao: com.example.itemremindertool.data.dao.ActivityEventDao
    ) {
        payload.categories.forEach { dto ->
            categoryDao.insertCategory(categoryFromDto(dto, categoryDao.getCategoryByUuid(dto.uuid)))
        }
        payload.warehouses.forEach { dto ->
            val entity = warehouseFromDto(dto, warehouseDao.getWarehouseByUuid(dto.uuid))
            val id = warehouseDao.insertWarehouse(entity)
            val inserted = warehouseDao.getWarehouseById(id) ?: entity
            if (dto.imageUri != null && inserted.imageUri.isNullOrBlank()) {
                val localPath = downloadImage(RetrofitClient.getApiService(context), dto.imageUri)
                if (localPath != null) {
                    warehouseDao.updateWarehouse(inserted.copy(imageUri = localPath))
                }
            }
        }
        payload.shoppingItems.forEach { dto ->
            val entity = shoppingItemFromDto(dto, shoppingItemDao.getShoppingItemByUuid(dto.uuid), null)
            val id = shoppingItemDao.insertShoppingItem(entity)
            val inserted = shoppingItemDao.getShoppingItemById(id) ?: entity
            if (dto.imageUri != null && inserted.imageUri.isNullOrBlank()) {
                val localPath = downloadImage(RetrofitClient.getApiService(context), dto.imageUri)
                if (localPath != null) {
                    shoppingItemDao.updateShoppingItem(inserted.copy(imageUri = localPath))
                }
            }
        }
        payload.items.forEach { dto ->
            val entity = itemFromDto(dto, itemDao.getItemByUuid(dto.uuid), warehouseDao, categoryDao)
            val id = itemDao.insertItem(entity)
            val inserted = itemDao.getItemById(id) ?: entity
            downloadRemoteImagesIfNeeded(inserted, dto.imageUris ?: emptyList(), itemDao)
        }
        payload.activityEvents.forEach { dto ->
            val type = runCatching {
                com.example.itemremindertool.data.model.ActivityEventType.valueOf(dto.type)
            }.getOrElse { com.example.itemremindertool.data.model.ActivityEventType.ITEM_VIEWED }
            val event = com.example.itemremindertool.data.model.ActivityEvent(
                uuid = dto.uuid,
                type = type,
                title = dto.title,
                description = dto.description ?: "",
                targetId = null,
                targetName = dto.targetName ?: "",
                iconType = dto.iconType ?: "",
                createdAt = parseDateOrNull(dto.createdAt) ?: Date(),
                metadata = dto.metadata ?: ""
            )
            activityEventDao.insert(event)
        }
        applySettingsSnapshot(payload.settings)
    }

    private suspend fun buildBootstrapAck(
        plan: SyncUploadPlan,
        itemDao: com.example.itemremindertool.data.dao.ItemDao,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao,
        shoppingItemDao: com.example.itemremindertool.data.dao.ShoppingItemDao,
        activityEventDao: com.example.itemremindertool.data.dao.ActivityEventDao
    ): SyncBootstrapAckRequest? {
        val items = mutableListOf<ItemDto>()
        for (uuid in plan.items) {
            val item = itemDao.getItemByUuid(uuid) ?: continue
            items.add(itemToDto(item, warehouseDao, categoryDao))
        }

        val categories = mutableListOf<CategoryDto>()
        for (uuid in plan.categories) {
            val category = categoryDao.getCategoryByUuid(uuid) ?: continue
            categories.add(categoryToDto(category))
        }

        val warehouses = mutableListOf<WarehouseDto>()
        for (uuid in plan.warehouses) {
            val warehouse = warehouseDao.getWarehouseByUuid(uuid) ?: continue
            warehouses.add(warehouseToDto(warehouse))
        }

        val shoppingItems = mutableListOf<ShoppingItemDto>()
        for (uuid in plan.shoppingItems) {
            val shoppingItem = shoppingItemDao.getShoppingItemByUuid(uuid) ?: continue
            shoppingItems.add(shoppingItemToDto(shoppingItem))
        }

        val eventsMap = activityEventDao.getAllEventsSync().associateBy { it.uuid }
        val activityEvents = mutableListOf<ActivityEventDto>()
        for (uuid in plan.activityEvents) {
            val event = eventsMap[uuid] ?: continue
            activityEvents.add(
                ActivityEventDto(
                    uuid = event.uuid,
                    type = event.type.name,
                    title = event.title,
                    description = event.description,
                    targetUuid = null,
                    targetName = event.targetName,
                    iconType = event.iconType,
                    createdAt = dateFormat.format(event.createdAt),
                    metadata = event.metadata
                )
            )
        }
        val settings = if (plan.settings) readSettingsSnapshot() else null

        if (
            items.isEmpty() &&
            categories.isEmpty() &&
            warehouses.isEmpty() &&
            shoppingItems.isEmpty() &&
            activityEvents.isEmpty() &&
            settings == null
        ) {
            return null
        }

        return SyncBootstrapAckRequest(
            items = items,
            categories = categories,
            warehouses = warehouses,
            shoppingItems = shoppingItems,
            activityEvents = activityEvents,
            settings = settings
        )
    }

    private fun readSettingsSnapshot(): SettingsSnapshot? {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val data = prefs.all.mapValues { it.value?.toString() ?: "" }
        val updatedAt = syncPrefs.getString(KEY_SETTINGS_UPDATED_AT, null)
        return SettingsSnapshot(data = data, updatedAt = updatedAt)
    }

    private fun applySettingsSnapshot(snapshot: SettingsSnapshot?) {
        if (snapshot == null) return
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            clear()
            snapshot.data.forEach { (key, value) ->
                putString(key, value)
            }
            apply()
        }
        syncPrefs.edit().putString(KEY_SETTINGS_UPDATED_AT, snapshot.updatedAt).apply()
    }
    
    // ==================== 离线队列支持 ====================
    
    /**
     * 添加失败的同步任务到离线队列
     */
    private suspend fun addToOfflineQueue(
        entityType: String,
        entityUuid: String,
        operation: SyncOperation,
        entity: Any
    ) {
        try {
            val entityJson = gson.toJson(entity)
            val syncQueue = SyncQueue.getInstance(context)
            syncQueue.addToQueue(entityType, entityUuid, operation, entityJson)
        } catch (e: Exception) {
            Log.e(TAG, "添加到离线队列失败", e)
        }
    }
    
    // ==================== ShoppingItem 同步 ====================
    
    /**
     * 同步购物项到远端
     */
    suspend fun syncShoppingItemToRemote(shoppingItem: ShoppingItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            if (shoppingItem.isSample) {
                return@withContext Result.success(Unit)
            }
            val shoppingItemDao = AppDatabase.getDatabase(context).shoppingItemDao()
            val preparedItem = ensureShoppingItemImageKey(shoppingItem, shoppingItemDao)
            
            val apiService = RetrofitClient.getApiService(context)
            val dto = shoppingItemToDto(preparedItem)
            val response = apiService.upsertShoppingItem(dto)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "购物项同步成功: ${preparedItem.name}")
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "同步失败"
                Log.e(TAG, "购物项同步失败: $error")
                // 添加到离线队列
                addToOfflineQueue("shopping_item", preparedItem.uuid, SyncOperation.UPDATE, preparedItem)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "购物项同步异常", e)
            // 添加到离线队列
            addToOfflineQueue("shopping_item", shoppingItem.uuid, SyncOperation.UPDATE, shoppingItem)
            Result.failure(e)
        }
    }
    
    /**
     * 从远端删除购物项
     */
    suspend fun deleteShoppingItemFromRemote(uuid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            
            val apiService = RetrofitClient.getApiService(context)
            val response = apiService.deleteShoppingItem(uuid)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "购物项删除同步成功: $uuid")
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "删除同步失败"
                Log.e(TAG, "购物项删除同步失败: $error")
                // 添加到离线队列
                addToOfflineQueue("shopping_item", uuid, SyncOperation.DELETE, mapOf("uuid" to uuid))
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "购物项删除同步异常", e)
            // 添加到离线队列
            addToOfflineQueue("shopping_item", uuid, SyncOperation.DELETE, mapOf("uuid" to uuid))
            Result.failure(e)
        }
    }
    
    private fun shoppingItemToDto(shoppingItem: ShoppingItem): ShoppingItemDto {
        return ShoppingItemDto(
            uuid = shoppingItem.uuid,
            name = shoppingItem.name,
            description = shoppingItem.description,
            quantity = shoppingItem.quantity,
            isCompleted = shoppingItem.isCompleted,
            priority = shoppingItem.priority.name,
            createdAt = dateFormat.format(shoppingItem.createdAt),
            completedAt = shoppingItem.completedAt?.let { dateFormat.format(it) },
            imageUri = shoppingItem.imageKey,
            itemUuid = null // 暂时不同步 itemId，需要转换为 itemUuid
        )
    }
}
