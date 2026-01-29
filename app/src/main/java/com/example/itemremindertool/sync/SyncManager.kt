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
    suspend fun mergeRemoteAndLocalOnce(force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        if (!shouldSyncToRemote()) {
            return@withContext Result.success(Unit)
        }
        if (force) {
            syncPrefs.edit()
                .remove(KEY_NEXT_RETRY_AT)
                .remove(KEY_RESUME_ITEMS_PAGE)
                .remove(KEY_RESUME_CATEGORIES_PAGE)
                .remove(KEY_RESUME_WAREHOUSES_PAGE)
                .remove(KEY_RESUME_SHOPPING_PAGE)
                .remove(KEY_RESUME_DELETED_PAGE)
                .remove(KEY_RESUME_REMINDERS_PAGE)
                .apply()
            Log.d(TAG, "手动同步：已清理重试与分页断点")
        }
        val now = System.currentTimeMillis()
        val nextRetryAt = syncPrefs.getLong(KEY_NEXT_RETRY_AT, 0L)
        if (!force && nextRetryAt > now) {
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
            val deletedRecordDao = db.deletedRecordDao()
            val itemReminderDao = db.itemReminderDao()
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

            val deletedResult = fetchAllDeletedRecords(apiService)
            if (deletedResult.rateLimited) {
                scheduleRetry(deletedResult.retryAfterSec)
                return@withContext Result.failure(Exception("Rate limited"))
            }

            applyRemoteDeletedRecords(
                deletedResult.items,
                deletedRecordDao,
                itemDao,
                categoryDao,
                warehouseDao,
                shoppingItemDao,
                activityEventDao,
                itemReminderDao,
                syncQueueDao
            )

            val localDeletedRecords = deletedRecordDao.getAllDeletedRecords()
            val remoteDeletedKeys = deletedResult.items.associateBy { "${it.entityType}:${it.entityUuid}" }
            for (record in localDeletedRecords) {
                val key = "${record.entityType}:${record.entityUuid}"
                if (!remoteDeletedKeys.containsKey(key)) {
                    syncDeletedRecordToRemote(record)
                }
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
            val remindersResult = fetchAllReminders(apiService)
            if (remindersResult.rateLimited) {
                scheduleRetry(remindersResult.retryAfterSec)
                return@withContext Result.failure(Exception("Rate limited"))
            }

            val deletedItemUuids = deletedResult.items
                .filter { it.entityType == "item" }
                .map { it.entityUuid }
                .toSet()
            val deletedCategoryUuids = deletedResult.items
                .filter { it.entityType == "category" }
                .map { it.entityUuid }
                .toSet()
            val deletedWarehouseUuids = deletedResult.items
                .filter { it.entityType == "warehouse" }
                .map { it.entityUuid }
                .toSet()
            val deletedShoppingUuids = deletedResult.items
                .filter { it.entityType == "shopping_item" }
                .map { it.entityUuid }
                .toSet()
            val deletedReminderUuids = deletedResult.items
                .filter { it.entityType == "reminder" }
                .map { it.entityUuid }
                .toSet()

            val remoteItems = itemsResult.items.filterNot { deletedItemUuids.contains(it.uuid) }
            val remoteCategories = categoriesResult.items.filterNot { deletedCategoryUuids.contains(it.uuid) }
            val remoteWarehouses = warehousesResult.items.filterNot { deletedWarehouseUuids.contains(it.uuid) }
            val remoteShoppingItems = shoppingResult.items.filterNot { deletedShoppingUuids.contains(it.uuid) }
            val remoteReminders = remindersResult.items.filterNot { deletedReminderUuids.contains(it.uuid) }

            val localItems = itemDao.getAllItemsList()
            val localCategories = categoryDao.getAllCategoriesSync()
            val localWarehouses = warehouseDao.getAllWarehousesSync()
            val localShoppingItems = shoppingItemDao.getAllShoppingItemsSync()
            val localReminders = itemReminderDao.getAllRemindersSync()

            val remoteItemsByUuid = remoteItems.associateBy { it.uuid }
            val remoteCategoriesByUuid = remoteCategories.associateBy { it.uuid }
            val remoteWarehousesByUuid = remoteWarehouses.associateBy { it.uuid }
            val remoteShoppingByUuid = remoteShoppingItems.associateBy { it.uuid }
            val remoteRemindersByUuid = remoteReminders.associateBy { it.uuid }

            val localItemsByUuid = localItems.associateBy { it.uuid }
            val localCategoriesByUuid = localCategories.associateBy { it.uuid }
            val localWarehousesByUuid = localWarehouses.associateBy { it.uuid }
            val localShoppingByUuid = localShoppingItems.associateBy { it.uuid }
            val localRemindersByUuid = localReminders.associateBy { it.uuid }

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
                    val entity = warehouseFromDto(remote, null, warehouseDao)
                    val insertedId = warehouseDao.insertWarehouse(entity)
                    val inserted = warehouseDao.getWarehouseByUuid(entity.uuid) ?: entity
                    if (remote.imageUri != null && inserted.imageUri.isNullOrBlank()) {
                        val localPath = downloadImage(apiService, remote.imageUri)
                        if (localPath != null) {
                            warehouseDao.updateWarehouse(inserted.copy(imageUri = localPath))
                        }
                    }
                } else {
                    val remoteTime = parseDateOrNull(remote.updatedAt) ?: parseDateOrNull(remote.createdAt)
                    if (isRemoteNewer(remoteTime, local.createdAt)) {
                        val entity = warehouseFromDto(remote, local, warehouseDao)
                        warehouseDao.insertWarehouse(entity)
                        if (remote.imageUri != null && local.imageUri.isNullOrBlank()) {
                            val localPath = downloadImage(apiService, remote.imageUri)
                            if (localPath != null) {
                                warehouseDao.updateWarehouse(entity.copy(imageUri = localPath))
                            }
                        }
                    } else if (local.createdAt.after(remoteTime ?: Date(0)) || !isSameWarehouse(local, remote)) {
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
                    val inserted = itemDao.getItemByUuid(entity.uuid) ?: entity
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
                    val itemUuid = remote.itemUuid
                    val entity = shoppingItemFromDto(remote, null, itemUuid)
                    val insertedId = shoppingItemDao.insertShoppingItem(entity)
                    val inserted = shoppingItemDao.getShoppingItemByUuid(entity.uuid) ?: entity
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
                        val itemUuid = remote.itemUuid ?: local.itemUuid
                        val entity = shoppingItemFromDto(remote, local, itemUuid)
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

            // Reminders: 使用 updatedAt 对比，支持双向同步
            for (remote in remoteReminders) {
                val local = localRemindersByUuid[remote.uuid]
                if (local == null) {
                    val entity = reminderFromDto(remote, null)
                    itemReminderDao.insertReminder(entity)
                } else {
                    val remoteTime = parseDateOrNull(remote.updatedAt) ?: parseDateOrNull(remote.createdAt)
                    if (isRemoteNewer(remoteTime, local.updatedAt)) {
                        val entity = reminderFromDto(remote, local)
                        itemReminderDao.updateReminder(entity)
                    } else if (local.updatedAt.after(remoteTime ?: Date(0))) {
                        syncReminderToRemote(local)
                    }
                }
            }
            for (local in localReminders) {
                if (!remoteRemindersByUuid.containsKey(local.uuid)) {
                    syncReminderToRemote(local)
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

    private suspend fun applyRemoteDeletedRecords(
        records: List<DeletedRecordDto>,
        deletedRecordDao: com.example.itemremindertool.data.dao.DeletedRecordDao,
        itemDao: com.example.itemremindertool.data.dao.ItemDao,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao,
        shoppingItemDao: com.example.itemremindertool.data.dao.ShoppingItemDao,
        activityEventDao: com.example.itemremindertool.data.dao.ActivityEventDao,
        itemReminderDao: com.example.itemremindertool.data.dao.ItemReminderDao,
        syncQueueDao: com.example.itemremindertool.data.dao.SyncQueueDao
    ) {
        for (record in records) {
            val deletedAt = parseDateOrNull(record.deletedAt) ?: Date()
            val existing = deletedRecordDao.getDeletedRecord(record.entityType, record.entityUuid)
            if (existing != null && existing.deletedAt.after(deletedAt)) {
                continue
            }
            deletedRecordDao.insertDeletedRecord(
                DeletedRecord(
                    uuid = record.uuid,
                    entityType = record.entityType,
                    entityUuid = record.entityUuid,
                    deletedAt = deletedAt
                )
            )
            syncQueueDao.deleteByUuid(record.entityUuid)
            when (record.entityType) {
                "item" -> {
                    itemDao.deleteItemByUuid(record.entityUuid)
                    itemReminderDao.deleteRemindersByItemId(record.entityUuid)
                }
                "category" -> categoryDao.deleteCategoryByUuid(record.entityUuid)
                "warehouse" -> warehouseDao.deleteWarehouseByUuid(record.entityUuid)
                "shopping_item" -> shoppingItemDao.deleteShoppingItemByUuid(record.entityUuid)
                "reminder" -> {
                    val reminder = itemReminderDao.getReminderByUuid(record.entityUuid)
                    if (reminder != null) {
                        itemReminderDao.deleteReminder(reminder)
                    }
                }
                "activity_event" -> activityEventDao.deleteByUuid(record.entityUuid)
                else -> Log.w(TAG, "未知删除类型: ${record.entityType}")
            }
        }
    }

    private suspend fun syncDeletedRecordToRemote(record: DeletedRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            val apiService = RetrofitClient.getApiService(context)
            val dto = DeletedRecordDto(
                uuid = record.uuid,
                entityType = record.entityType,
                entityUuid = record.entityUuid,
                deletedAt = dateFormat.format(record.deletedAt)
            )
            val response = apiService.upsertDeletedRecord(dto)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "同步删除记录失败"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        // 如果已经有 imageKeys，不需要重新上传
        if (item.imageKeys.isNotEmpty()) {
            Log.d(TAG, "物品 ${item.uuid} 已有图片键，跳过上传: ${item.imageKeys.size} 个")
            return item
        }
        
        // 如果没有本地图片，不需要上传
        if (item.imageUris.isEmpty()) {
            Log.d(TAG, "物品 ${item.uuid} 没有本地图片，跳过上传")
            return item
        }

        Log.d(TAG, "开始上传物品图片: ${item.uuid}, 本地图片数量: ${item.imageUris.size}")

        val apiService = RetrofitClient.getApiService(context)
        val uploadedKeys = mutableListOf<String>()
        val failedUploads = mutableListOf<String>()

        for ((index, path) in item.imageUris.withIndex()) {
            Log.d(TAG, "上传图片 ${index + 1}/${item.imageUris.size}: $path")
            val uploadResult = uploadImage(apiService, path, item.uuid)
            if (uploadResult != null) {
                uploadedKeys.add(uploadResult)
                Log.d(TAG, "图片上传成功: $path -> $uploadResult")
            } else {
                failedUploads.add(path)
                Log.w(TAG, "图片上传失败: $path")
            }
        }

        if (uploadedKeys.isEmpty()) {
            Log.w(TAG, "物品 ${item.uuid} 的所有图片上传失败")
            return item
        }

        if (failedUploads.isNotEmpty()) {
            Log.w(TAG, "物品 ${item.uuid} 有 ${failedUploads.size} 张图片上传失败，${uploadedKeys.size} 张成功")
        } else {
            Log.i(TAG, "物品 ${item.uuid} 的所有图片上传成功: ${uploadedKeys.size} 张")
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
        return try {
            // 验证本地文件存在且有效
            val localFile = File(localPath)
            if (!localFile.exists()) {
                Log.e(TAG, "上传图片失败: 本地文件不存在: $localPath")
                return null
            }
            
            // 验证文件是否为有效的图片
            if (!com.example.itemremindertool.utils.ImageUtils.isValidImageFile(localPath)) {
                Log.e(TAG, "上传图片失败: 本地文件不是有效的图片: $localPath")
                return null
            }
            
            val bytes = readBytes(localPath) ?: run {
                Log.e(TAG, "上传图片失败: 无法读取文件: $localPath")
                return null
            }
            
            if (bytes.isEmpty()) {
                Log.e(TAG, "上传图片失败: 文件为空: $localPath")
                return null
            }
            
            val mimeType = resolveMimeType(localPath) ?: run {
                Log.e(TAG, "上传图片失败: 无法确定 MIME 类型: $localPath")
                return null
            }

            Log.d(TAG, "开始上传图片: $localPath, 大小: ${bytes.size} bytes, MIME: $mimeType, UUID: $itemUuid")

            val presignResponse = apiService.presignUpload(
                PresignUploadRequest(
                    mimeType = mimeType,
                    fileSize = bytes.size.toLong(),
                    itemUuid = itemUuid
                )
            )
            if (!presignResponse.isSuccessful || presignResponse.body()?.success != true) {
                Log.e(TAG, "获取上传签名失败: ${presignResponse.code()}, 响应: ${presignResponse.body()}")
                return null
            }

            val data = presignResponse.body()?.data ?: run {
                Log.e(TAG, "获取上传签名失败: 响应数据为空")
                return null
            }
            
            Log.d(TAG, "获取上传签名成功: objectKey=${data.objectKey}, uploadUrl=${data.uploadUrl}")
            Log.d(TAG, "requiredHeaders: ${data.requiredHeaders}")

            val requestBody = RequestBody.create(mimeType.toMediaTypeOrNull(), bytes)
            val requestBuilder = Request.Builder()
                .url(data.uploadUrl)
                .put(requestBody)
                .header("Content-Type", mimeType) // 明确设置 Content-Type
            
            // 设置服务器要求的请求头（如 x-amz-server-side-encryption）
            data.requiredHeaders?.forEach { (key, value) ->
                Log.d(TAG, "添加上传请求头: $key = $value")
                requestBuilder.header(key, value)
            }
            
            val request = requestBuilder.build()
            
            // 记录上传请求的详细信息
            Log.d(TAG, "上传请求 URL: ${request.url}")
            Log.d(TAG, "上传请求方法: ${request.method}")
            Log.d(TAG, "上传请求头: ${request.headers}")
            Log.d(TAG, "上传数据大小: ${bytes.size} bytes")

            uploadClient.newCall(request).execute().use { response ->
                val statusCode = response.code
                val statusMessage = response.message
                
                // 记录响应头
                Log.d(TAG, "上传响应状态码: $statusCode $statusMessage")
                Log.d(TAG, "上传响应头: ${response.headers}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "========== 图片上传失败 ==========")
                    Log.e(TAG, "objectKey: ${data.objectKey}")
                    Log.e(TAG, "状态码: $statusCode")
                    Log.e(TAG, "消息: $statusMessage")
                    Log.e(TAG, "错误响应: $errorBody")
                    Log.e(TAG, "上传URL: ${data.uploadUrl}")
                    Log.e(TAG, "====================================")
                    return null
                }
                
                // MinIO/S3 上传成功时应该返回 200 和 ETag
                val etag = response.header("ETag") ?: response.header("etag")
                val contentLength = response.header("Content-Length") ?: response.header("content-length")
                
                // 检查响应体，如果是 HTML 说明可能是错误页面
                val responseBody = response.body?.string() ?: ""
                if (responseBody.isNotEmpty()) {
                    // 检查是否为 HTML 错误页面
                    val bodyLower = responseBody.lowercase()
                    if (bodyLower.contains("<!doctype") || bodyLower.contains("<html")) {
                        Log.e(TAG, "========== 图片上传返回了 HTML 错误页面 ==========")
                        Log.e(TAG, "objectKey: ${data.objectKey}")
                        Log.e(TAG, "状态码: $statusCode")
                        Log.e(TAG, "响应: ${responseBody.take(500)}")
                        Log.e(TAG, "上传URL: ${data.uploadUrl}")
                        Log.e(TAG, "可能的原因: MinIO 配置问题或反向代理配置问题")
                        Log.e(TAG, "================================================")
                        return null
                    }
                    Log.d(TAG, "图片上传响应体: ${responseBody.take(200)}")
                }
                
                // 验证上传成功的关键指标
                // MinIO/S3 上传成功时必须返回 ETag，如果没有 ETag 说明上传失败
                if (statusCode == 200 && etag != null) {
                    Log.i(TAG, "========== 图片上传成功 ==========")
                    Log.i(TAG, "objectKey: ${data.objectKey}")
                    Log.i(TAG, "ETag: $etag")
                    Log.i(TAG, "Content-Length: $contentLength")
                    Log.i(TAG, "数据大小: ${bytes.size} bytes")
                    Log.i(TAG, "====================================")
                } else {
                    Log.e(TAG, "========== 图片上传失败 ==========")
                    Log.e(TAG, "objectKey: ${data.objectKey}")
                    Log.e(TAG, "状态码: $statusCode")
                    Log.e(TAG, "ETag: $etag")
                    Log.e(TAG, "Content-Length: $contentLength")
                    Log.e(TAG, "错误: MinIO/S3 上传成功必须返回 ETag，如果没有 ETag 说明实际上传失败")
                    Log.e(TAG, "可能的原因:")
                    Log.e(TAG, "1. 上传请求头不正确（缺少 x-amz-server-side-encryption 等）")
                    Log.e(TAG, "2. MinIO 配置问题")
                    Log.e(TAG, "3. 签名 URL 有问题")
                    Log.e(TAG, "4. 网络问题导致实际上传失败")
                    Log.e(TAG, "==========================================")
                    return null
                }
            }

            // 可选：验证上传是否成功（通过尝试获取文件元数据）
            // 注意：这会增加一次请求，但可以确保文件真的上传成功
            // 暂时注释掉，因为会增加延迟，如果下载失败会自动重新上传
            /*
            try {
                val verifyResponse = apiService.presignRead(data.objectKey)
                if (!verifyResponse.isSuccessful) {
                    Log.w(TAG, "上传验证失败: 无法获取下载签名: ${verifyResponse.code()}")
                } else {
                    Log.d(TAG, "上传验证成功: 可以获取下载签名")
                }
            } catch (e: Exception) {
                Log.w(TAG, "上传验证时出错", e)
            }
            */

            Log.i(TAG, "图片上传完成: $localPath -> ${data.objectKey}")
            data.objectKey
        } catch (e: Exception) {
            Log.e(TAG, "上传图片时发生异常: $localPath", e)
            null
        }
    }

    private suspend fun downloadRemoteImagesIfNeeded(
        item: Item,
        imageKeys: List<String>,
        itemDao: com.example.itemremindertool.data.dao.ItemDao
    ) {
        if (imageKeys.isEmpty()) {
            Log.d(TAG, "跳过下载图片: 物品 ${item.uuid} 没有图片键")
            return
        }

        // 检查本地图片是否存在且有效
        val existingValidImages = item.imageUris.filter { path ->
            val file = File(path)
            file.exists() && com.example.itemremindertool.utils.ImageUtils.isValidImageFile(path)
        }
        
        // 如果所有本地图片都有效，不需要下载
        if (existingValidImages.isNotEmpty() && existingValidImages.size == item.imageUris.size) {
            Log.d(TAG, "跳过下载图片: 物品 ${item.uuid} 的本地图片都有效")
            return
        }
        
        // 如果有无效的本地图片，需要重新下载
        if (existingValidImages.size < item.imageUris.size) {
            Log.w(TAG, "物品 ${item.uuid} 有 ${item.imageUris.size - existingValidImages.size} 个无效的本地图片，需要重新下载")
        }

        val apiService = RetrofitClient.getApiService(context)
        val downloaded = mutableListOf<String>()
        val remoteKeys = imageKeys.filter { isRemoteObjectKey(it) }
        
        Log.d(TAG, "开始下载图片: 物品 ${item.uuid}, 远程键数量: ${remoteKeys.size}")
        
        for (key in remoteKeys) {
            Log.d(TAG, "下载图片: objectKey=$key")
            val localPath = downloadImage(apiService, key)
            if (localPath != null) {
                downloaded.add(localPath)
                Log.d(TAG, "图片下载成功: $key -> $localPath")
            } else {
                Log.w(TAG, "图片下载失败: $key")
            }
        }

        if (downloaded.isEmpty()) {
            Log.w(TAG, "所有图片下载失败: 物品 ${item.uuid}")
            return
        }

        Log.i(TAG, "图片下载完成: 物品 ${item.uuid}, 成功下载 ${downloaded.size}/${remoteKeys.size} 张图片")
        
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
            val statusCode = resp.code
            val statusMessage = resp.message
            
            Log.d(TAG, "下载图片响应: objectKey=$objectKey, 状态码=$statusCode, 消息=$statusMessage")
            
            if (!resp.isSuccessful) {
                val errorBody = resp.body?.string() ?: ""
                Log.e(TAG, "下载图片失败: objectKey=$objectKey, 状态码=$statusCode, 消息=$statusMessage, 错误: ${errorBody.take(200)}")
                return null
            }
            
            // 检查 Content-Type，确保是图片类型
            val contentType = resp.header("Content-Type", "")?.lowercase() ?: ""
            Log.d(TAG, "下载图片 Content-Type: objectKey=$objectKey, Content-Type=$contentType")
            
            if (contentType.isNotEmpty() && !contentType.startsWith("image/")) {
                // 如果 Content-Type 是 text/html，说明可能是错误页面
                if (contentType.contains("text/html")) {
                    val errorBody = resp.body?.string() ?: ""
                    Log.e(TAG, "下载到的是 HTML 页面而不是图片: objectKey=$objectKey, Content-Type=$contentType, 预览: ${errorBody.take(200)}")
                    return null
                }
                Log.w(TAG, "下载的内容不是图片类型: objectKey=$objectKey, Content-Type=$contentType, 但继续尝试解析")
            }
            
            val bytes = resp.body?.bytes() ?: return null
            
            // 验证下载的数据不为空
            if (bytes.isEmpty()) {
                Log.e(TAG, "下载的图片数据为空: objectKey=$objectKey")
                return null
            }
            
            Log.d(TAG, "下载的图片数据大小: objectKey=$objectKey, 大小=${bytes.size} bytes")
            
            // 检查是否为 HTML 错误页面（常见错误页面的开头）
            if (isHtmlErrorPage(bytes)) {
                val htmlPreview = String(bytes.take(500).toByteArray(), Charsets.UTF_8)
                Log.e(TAG, "========== 图片下载失败：返回了 HTML 错误页面 ==========")
                Log.e(TAG, "objectKey: $objectKey")
                Log.e(TAG, "状态码: $statusCode")
                Log.e(TAG, "Content-Type: $contentType")
                Log.e(TAG, "数据大小: ${bytes.size} bytes")
                Log.e(TAG, "HTML 预览: ${htmlPreview.take(300)}")
                Log.e(TAG, "签名URL: ${data.signedUrl}")
                Log.e(TAG, "可能的原因:")
                Log.e(TAG, "1. 文件实际上传失败，MinIO 中不存在该文件")
                Log.e(TAG, "2. 反向代理配置问题，当文件不存在时返回了 HTML 错误页面")
                Log.e(TAG, "3. MinIO 配置了错误页面")
                Log.e(TAG, "建议: 检查 MinIO 中是否存在该文件，检查服务器配置")
                Log.e(TAG, "================================================")
                return null
            }
            
            // 验证是否为有效的图片格式
            if (!isValidImageData(bytes)) {
                Log.e(TAG, "下载的图片数据无效或损坏: $objectKey, 文件头: ${bytes.take(12).joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }}")
                return null
            }
            
            val filePath = writeCacheFile(objectKey, bytes)
            
            // 验证保存的文件是否有效
            if (filePath != null && !isValidImageFile(filePath)) {
                Log.e(TAG, "保存的图片文件无效，删除: $filePath")
                try {
                    File(filePath).delete()
                } catch (e: Exception) {
                    Log.e(TAG, "删除无效图片文件失败", e)
                }
                return null
            }
            
            return filePath
        }
    }
    
    /**
     * 检查是否为 HTML 错误页面
     */
    private fun isHtmlErrorPage(bytes: ByteArray): Boolean {
        if (bytes.size < 4) {
            return false
        }
        
        // 检查常见的 HTML 开头
        // <!DOCTYPE, <html, <HTML, <?xml (XML 错误页面)
        val start = bytes.take(20).toByteArray()
        val startString = String(start, Charsets.UTF_8).lowercase()
        
        return startString.startsWith("<!doctype") ||
               startString.startsWith("<html") ||
               startString.startsWith("<?xml") ||
               startString.startsWith("<html") ||
               startString.contains("error") ||
               startString.contains("404") ||
               startString.contains("not found")
    }
    
    /**
     * 验证字节数组是否为有效的图片数据
     */
    private fun isValidImageData(bytes: ByteArray): Boolean {
        if (bytes.size < 4) {
            return false
        }
        
        // 检查常见图片格式的文件头
        // JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return true
        }
        // PNG: 89 50 4E 47
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && 
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) {
            return true
        }
        // GIF: 47 49 46 38
        if (bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && 
            bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte()) {
            return true
        }
        // WebP: 检查 RIFF...WEBP
        if (bytes.size >= 12 && 
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && 
            bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() && 
            bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()) {
            return true
        }
        
        return false
    }
    
    /**
     * 验证文件是否为有效的图片文件
     */
    private fun isValidImageFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) {
                return false
            }
            
            // 尝试解码图片以验证文件完整性
            val options = android.graphics.BitmapFactory.Options()
            options.inJustDecodeBounds = true
            val result = android.graphics.BitmapFactory.decodeFile(filePath, options)
            
            // 如果能够读取图片尺寸，说明文件有效
            options.outWidth > 0 && options.outHeight > 0
        } catch (e: Exception) {
            Log.e(TAG, "验证图片文件失败: $filePath", e)
            false
        }
    }

    private fun writeCacheFile(objectKey: String, bytes: ByteArray): String? {
        // 根据文件头确定正确的扩展名
        val extension = detectImageExtension(bytes) ?: objectKey.substringAfterLast('.', "jpg")
        
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (externalDir != null) {
            val dir = File(externalDir, "ItemReminderTool")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val fileName = sha256Hex(objectKey) + "." + extension
            val file = File(dir, fileName)
            return try {
                // 使用 FileOutputStream 确保原子写入
                file.outputStream().use { output ->
                    output.write(bytes)
                    output.flush()
                }
                // 验证文件是否成功写入
                if (file.exists() && file.length() == bytes.size.toLong()) {
                    file.absolutePath
                } else {
                    Log.e(TAG, "文件写入验证失败: 期望大小=${bytes.size}, 实际大小=${file.length()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "写入默认图片目录失败", e)
                // 如果写入失败，尝试删除可能的部分文件
                try {
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (ignored: Exception) { }
                null
            }
        }
        return try {
            val dir = File(context.cacheDir, "remote_images")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val fileName = sha256Hex(objectKey) + "." + extension
            val file = File(dir, fileName)
            // 使用 FileOutputStream 确保原子写入
            file.outputStream().use { output ->
                output.write(bytes)
                output.flush()
            }
            // 验证文件是否成功写入
            if (file.exists() && file.length() == bytes.size.toLong()) {
                file.absolutePath
            } else {
                Log.e(TAG, "文件写入验证失败: 期望大小=${bytes.size}, 实际大小=${file.length()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入缓存图片失败", e)
            null
        }
    }
    
    /**
     * 根据文件头检测图片扩展名
     */
    private fun detectImageExtension(bytes: ByteArray): String? {
        if (bytes.size < 4) {
            return null
        }
        
        // JPEG: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return "jpg"
        }
        // PNG: 89 50 4E 47
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && 
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) {
            return "png"
        }
        // GIF: 47 49 46 38
        if (bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && 
            bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte()) {
            return "gif"
        }
        // WebP: 检查 RIFF...WEBP
        if (bytes.size >= 12 && 
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && 
            bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() && 
            bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()) {
            return "webp"
        }
        
        return null
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
            val dto = warehouseToDto(preparedWarehouse, warehouseDao)
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
        return ItemDto(
            uuid = item.uuid,
            name = item.name,
            description = item.description,
            categoryUuid = item.categoryUuid,
            warehouseUuid = item.warehouseUuid,
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

    private fun reminderToDto(reminder: ItemReminder): ItemReminderDto {
        return ItemReminderDto(
            uuid = reminder.uuid,
            itemUuid = reminder.itemUuid,
            reminderType = reminder.reminderType.name,
            reminderTime = reminder.reminderTime?.let { dateFormat.format(it) },
            dailyTime = reminder.dailyTime,
            monthlyDay = reminder.monthlyDay,
            monthlyTime = reminder.monthlyTime,
            yearlyMonth = reminder.yearlyMonth,
            yearlyDay = reminder.yearlyDay,
            yearlyTime = reminder.yearlyTime,
            reason = reminder.reason,
            isEnabled = reminder.isEnabled,
            createdAt = dateFormat.format(reminder.createdAt),
            updatedAt = dateFormat.format(reminder.updatedAt)
        )
    }
    
    private suspend fun warehouseToDto(
        warehouse: Warehouse,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao
    ): WarehouseDto {
        return WarehouseDto(
            uuid = warehouse.uuid,
            name = warehouse.name,
            description = warehouse.description,
            location = warehouse.location,
            capacity = warehouse.capacity,
            parentUuid = warehouse.parentUuid,
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
        return Item(
            uuid = dto.uuid,
            name = dto.name,
            description = dto.description ?: existing?.description ?: "",
            categoryUuid = dto.categoryUuid ?: existing?.categoryUuid,
            warehouseUuid = dto.warehouseUuid ?: existing?.warehouseUuid,
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
            uuid = dto.uuid,
            name = dto.name,
            description = dto.description ?: existing?.description ?: "",
            color = dto.color ?: existing?.color ?: "#6200EE",
            icon = dto.icon ?: existing?.icon ?: "category"
        )
    }

    private fun reminderFromDto(dto: ItemReminderDto, existing: ItemReminder?): ItemReminder {
        val createdAt = parseDateOrNull(dto.createdAt) ?: existing?.createdAt ?: Date()
        val updatedAt = parseDateOrNull(dto.updatedAt) ?: existing?.updatedAt ?: createdAt
        val reminderType = runCatching {
            ReminderType.valueOf(dto.reminderType.ifBlank { "ONCE" })
        }.getOrElse { ReminderType.ONCE }
        return ItemReminder(
            uuid = dto.uuid,
            itemUuid = dto.itemUuid,
            reminderType = reminderType,
            reminderTime = parseDateOrNull(dto.reminderTime),
            dailyTime = dto.dailyTime ?: existing?.dailyTime,
            monthlyDay = dto.monthlyDay ?: existing?.monthlyDay,
            monthlyTime = dto.monthlyTime ?: existing?.monthlyTime,
            yearlyMonth = dto.yearlyMonth ?: existing?.yearlyMonth,
            yearlyDay = dto.yearlyDay ?: existing?.yearlyDay,
            yearlyTime = dto.yearlyTime ?: existing?.yearlyTime,
            reason = dto.reason ?: existing?.reason ?: "",
            isEnabled = dto.isEnabled ?: existing?.isEnabled ?: true,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private suspend fun warehouseFromDto(
        dto: WarehouseDto,
        existing: Warehouse?,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao
    ): Warehouse {
        val createdAt = parseDateOrNull(dto.createdAt) ?: existing?.createdAt ?: Date()
        return Warehouse(
            uuid = dto.uuid,
            name = dto.name,
            description = dto.description ?: existing?.description ?: "",
            location = dto.location ?: existing?.location ?: "",
            capacity = dto.capacity ?: existing?.capacity,
            parentUuid = dto.parentUuid,
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
        resolvedItemUuid: String?
    ): ShoppingItem {
        val createdAt = parseDateOrNull(dto.createdAt) ?: existing?.createdAt ?: Date()
        val completedAt = parseDateOrNull(dto.completedAt) ?: existing?.completedAt
        val priority = runCatching {
            Priority.valueOf(dto.priority ?: existing?.priority?.name ?: "MEDIUM")
        }.getOrElse { Priority.MEDIUM }
        return ShoppingItem(
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
            itemUuid = resolvedItemUuid ?: existing?.itemUuid,
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

    private fun isSameWarehouse(local: Warehouse, remote: WarehouseDto): Boolean {
        return local.name == remote.name &&
            local.description == (remote.description ?: "") &&
            local.location == (remote.location ?: "") &&
            local.capacity == remote.capacity &&
            local.parentUuid == remote.parentUuid &&
            local.level == (remote.level ?: local.level)
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
        val warehouseUuid = item.warehouseUuid ?: ""
        val categoryUuid = item.categoryUuid ?: ""
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
        val localWarehouseUuid = local.warehouseUuid
        if (remoteWarehouseUuid != null && localWarehouseUuid != null && remoteWarehouseUuid != localWarehouseUuid) {
            return false
        }
        val remoteCategoryUuid = remote.categoryUuid
        val localCategoryUuid = local.categoryUuid
        if (remoteCategoryUuid != null && localCategoryUuid != null && remoteCategoryUuid != localCategoryUuid) {
            return false
        }
        return true
    }

    /**
     * 检查名称是否为示例数据（通过名称模式）
     */
    private fun isSampleDataName(name: String): Boolean {
        val sampleKeywords = listOf("示例", "Sample", "sample", "EXAMPLE", "Example", "演示", "Demo", "demo")
        return sampleKeywords.any { keyword -> name.contains(keyword) }
    }

    /**
     * 标记配色设置已更新，需要同步到服务器
     */
    fun markColorSettingsUpdated() {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        syncPrefs.edit().putString(KEY_SETTINGS_UPDATED_AT, now).apply()
        Log.d(TAG, "配色设置已标记为需要同步: $now")
    }

    companion object {
        private const val TAG = "SyncManager"
        private const val MAX_SYNC_ACTIVITY_EVENTS = 5
        private const val KEY_NEXT_RETRY_AT = "next_retry_at"
        private const val KEY_RESUME_ITEMS_PAGE = "resume_items_page"
        private const val KEY_RESUME_CATEGORIES_PAGE = "resume_categories_page"
        private const val KEY_RESUME_WAREHOUSES_PAGE = "resume_warehouses_page"
        private const val KEY_RESUME_SHOPPING_PAGE = "resume_shopping_page"
        private const val KEY_RESUME_DELETED_PAGE = "resume_deleted_page"
        private const val KEY_RESUME_REMINDERS_PAGE = "resume_reminders_page"
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
        val maxPages = 500 // 最多100000条记录的安全限制
        var consecutiveEmptyPages = 0
        var lastPageSeen = 0
        while (page <= maxPages) {
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
            if (data.page != page || data.page <= lastPageSeen) {
                Log.w(TAG, "分页异常，返回页=${data.page}，请求页=$page，终止拉取")
                syncPrefs.edit().remove(KEY_RESUME_ITEMS_PAGE).apply()
                break
            }
            lastPageSeen = data.page
            val currentPageItems = data.items
            items.addAll(currentPageItems)
            Log.d(TAG, "拉取物品: page=$page, 当前页=${currentPageItems.size}, 累计=${items.size}, 总数=${data.total}")
            
            // 检测空页面
            if (currentPageItems.isEmpty()) {
                consecutiveEmptyPages++
                if (consecutiveEmptyPages >= 3) {
                    Log.w(TAG, "连续3页无数据，终止拉取")
                    break
                }
            } else {
                consecutiveEmptyPages = 0
            }
            
            // 退出条件：1) 已到达总数 或 2) 当前页没有数据
            if (data.page * data.pageSize >= data.total || currentPageItems.isEmpty()) {
                syncPrefs.edit().remove(KEY_RESUME_ITEMS_PAGE).apply()
                break
            }
            page += 1
            syncPrefs.edit().putInt(KEY_RESUME_ITEMS_PAGE, page).apply()
        }
        if (page > maxPages) {
            Log.e(TAG, "达到最大页数限制: $maxPages")
        }
        return FetchResult(items, false, null)
    }

    private suspend fun fetchAllCategories(apiService: com.example.itemremindertool.network.ApiService): FetchResult<CategoryDto> {
        val categories = mutableListOf<CategoryDto>()
        var page = syncPrefs.getInt(KEY_RESUME_CATEGORIES_PAGE, 1).coerceAtLeast(1)
        val pageSize = 200
        val maxPages = 100 // 分类通常不会太多
        var consecutiveEmptyPages = 0
        var lastPageSeen = 0
        while (page <= maxPages) {
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
            if (data.page != page || data.page <= lastPageSeen) {
                Log.w(TAG, "分页异常，返回页=${data.page}，请求页=$page，终止拉取")
                syncPrefs.edit().remove(KEY_RESUME_CATEGORIES_PAGE).apply()
                break
            }
            lastPageSeen = data.page
            val currentPageCategories = data.categories
            categories.addAll(currentPageCategories)
            Log.d(TAG, "拉取分类: page=$page, 当前页=${currentPageCategories.size}, 累计=${categories.size}, 总数=${data.total}")
            
            // 检测空页面
            if (currentPageCategories.isEmpty()) {
                consecutiveEmptyPages++
                if (consecutiveEmptyPages >= 3) {
                    Log.w(TAG, "连续3页无数据，终止拉取")
                    break
                }
            } else {
                consecutiveEmptyPages = 0
            }
            
            // 退出条件：1) 已到达总数 或 2) 当前页没有数据
            if (data.page * data.pageSize >= data.total || currentPageCategories.isEmpty()) {
                syncPrefs.edit().remove(KEY_RESUME_CATEGORIES_PAGE).apply()
                break
            }
            page += 1
            syncPrefs.edit().putInt(KEY_RESUME_CATEGORIES_PAGE, page).apply()
        }
        if (page > maxPages) {
            Log.e(TAG, "达到最大页数限制: $maxPages")
        }
        return FetchResult(categories, false, null)
    }

    private suspend fun fetchAllWarehouses(apiService: com.example.itemremindertool.network.ApiService): FetchResult<WarehouseDto> {
        val warehouses = mutableListOf<WarehouseDto>()
        var page = syncPrefs.getInt(KEY_RESUME_WAREHOUSES_PAGE, 1).coerceAtLeast(1)
        val pageSize = 200
        val maxPages = 200 // 容器通常不会太多
        var consecutiveEmptyPages = 0
        var lastPageSeen = 0
        while (page <= maxPages) {
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
            if (data.page != page || data.page <= lastPageSeen) {
                Log.w(TAG, "分页异常，返回页=${data.page}，请求页=$page，终止拉取")
                syncPrefs.edit().remove(KEY_RESUME_WAREHOUSES_PAGE).apply()
                break
            }
            lastPageSeen = data.page
            val currentPageWarehouses = data.warehouses
            warehouses.addAll(currentPageWarehouses)
            Log.d(TAG, "拉取容器: page=$page, 当前页=${currentPageWarehouses.size}, 累计=${warehouses.size}, 总数=${data.total}")
            
            // 检测空页面
            if (currentPageWarehouses.isEmpty()) {
                consecutiveEmptyPages++
                if (consecutiveEmptyPages >= 3) {
                    Log.w(TAG, "连续3页无数据，终止拉取")
                    break
                }
            } else {
                consecutiveEmptyPages = 0
            }
            
            // 退出条件：1) 已到达总数 或 2) 当前页没有数据
            if (data.page * data.pageSize >= data.total || currentPageWarehouses.isEmpty()) {
                syncPrefs.edit().remove(KEY_RESUME_WAREHOUSES_PAGE).apply()
                break
            }
            page += 1
            syncPrefs.edit().putInt(KEY_RESUME_WAREHOUSES_PAGE, page).apply()
        }
        if (page > maxPages) {
            Log.e(TAG, "达到最大页数限制: $maxPages")
        }
        return FetchResult(warehouses, false, null)
    }

    private suspend fun fetchAllShoppingItems(apiService: com.example.itemremindertool.network.ApiService): FetchResult<ShoppingItemDto> {
        val shoppingItems = mutableListOf<ShoppingItemDto>()
        var page = syncPrefs.getInt(KEY_RESUME_SHOPPING_PAGE, 1).coerceAtLeast(1)
        val pageSize = 200
        val maxPages = 200 // 购物清单通常不会太多
        var consecutiveEmptyPages = 0
        var lastPageSeen = 0
        while (page <= maxPages) {
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
            if (data.page != page || data.page <= lastPageSeen) {
                Log.w(TAG, "分页异常，返回页=${data.page}，请求页=$page，终止拉取")
                syncPrefs.edit().remove(KEY_RESUME_SHOPPING_PAGE).apply()
                break
            }
            lastPageSeen = data.page
            val currentPageShoppingItems = data.shoppingItems
            shoppingItems.addAll(currentPageShoppingItems)
            Log.d(TAG, "拉取购物清单: page=$page, 当前页=${currentPageShoppingItems.size}, 累计=${shoppingItems.size}, 总数=${data.total}")
            
            // 检测空页面
            if (currentPageShoppingItems.isEmpty()) {
                consecutiveEmptyPages++
                if (consecutiveEmptyPages >= 3) {
                    Log.w(TAG, "连续3页无数据，终止拉取")
                    break
                }
            } else {
                consecutiveEmptyPages = 0
            }
            
            // 退出条件：1) 已到达总数 或 2) 当前页没有数据
            if (data.page * data.pageSize >= data.total || currentPageShoppingItems.isEmpty()) {
                syncPrefs.edit().remove(KEY_RESUME_SHOPPING_PAGE).apply()
                break
            }
            page += 1
            syncPrefs.edit().putInt(KEY_RESUME_SHOPPING_PAGE, page).apply()
        }
        if (page > maxPages) {
            Log.e(TAG, "达到最大页数限制: $maxPages")
        }
        return FetchResult(shoppingItems, false, null)
    }

    private suspend fun fetchAllReminders(apiService: com.example.itemremindertool.network.ApiService): FetchResult<ItemReminderDto> {
        val reminders = mutableListOf<ItemReminderDto>()
        var page = syncPrefs.getInt(KEY_RESUME_REMINDERS_PAGE, 1).coerceAtLeast(1)
        val pageSize = 200
        val maxPages = 200 // 提醒数量通常不会太多
        var consecutiveEmptyPages = 0
        var lastPageSeen = 0
        while (page <= maxPages) {
            val response = apiService.getReminders(page = page, pageSize = pageSize)
            if (response.code() == 429) {
                syncPrefs.edit().putInt(KEY_RESUME_REMINDERS_PAGE, page).apply()
                return FetchResult(reminders, true, response.headers()["Retry-After"]?.toLongOrNull() ?: 60L)
            }
            if (!response.isSuccessful || response.body()?.success != true) {
                Log.e(TAG, "拉取提醒失败：${response.code()}")
                break
            }
            val data = response.body()?.data ?: break
            if (data.page != page || data.page <= lastPageSeen) {
                Log.w(TAG, "分页异常，返回页=${data.page}，请求页=$page，终止拉取")
                syncPrefs.edit().remove(KEY_RESUME_REMINDERS_PAGE).apply()
                break
            }
            lastPageSeen = data.page
            val currentPageReminders = data.reminders
            reminders.addAll(currentPageReminders)
            Log.d(TAG, "拉取提醒: page=$page, 当前页=${currentPageReminders.size}, 累计=${reminders.size}, 总数=${data.total}")

            if (currentPageReminders.isEmpty()) {
                consecutiveEmptyPages++
                if (consecutiveEmptyPages >= 3) {
                    Log.w(TAG, "连续3页无数据，终止拉取")
                    break
                }
            } else {
                consecutiveEmptyPages = 0
            }

            if (data.page * data.pageSize >= data.total || currentPageReminders.isEmpty()) {
                syncPrefs.edit().remove(KEY_RESUME_REMINDERS_PAGE).apply()
                break
            }
            page += 1
            syncPrefs.edit().putInt(KEY_RESUME_REMINDERS_PAGE, page).apply()
        }
        if (page > maxPages) {
            Log.e(TAG, "达到最大页数限制: $maxPages")
        }
        return FetchResult(reminders, false, null)
    }

    private suspend fun fetchAllDeletedRecords(
        apiService: com.example.itemremindertool.network.ApiService
    ): FetchResult<DeletedRecordDto> {
        val records = mutableListOf<DeletedRecordDto>()
        var page = syncPrefs.getInt(KEY_RESUME_DELETED_PAGE, 1).coerceAtLeast(1)
        val pageSize = 200
        val maxPages = 200 // 删除记录通常不会太多
        var consecutiveEmptyPages = 0
        var lastPageSeen = 0
        while (page <= maxPages) {
            val response = apiService.getDeletedRecords(page = page, pageSize = pageSize)
            if (response.code() == 429) {
                syncPrefs.edit().putInt(KEY_RESUME_DELETED_PAGE, page).apply()
                return FetchResult(records, true, response.headers()["Retry-After"]?.toLongOrNull() ?: 60L)
            }
            if (!response.isSuccessful || response.body()?.success != true) {
                Log.e(TAG, "拉取删除记录失败：${response.code()}")
                break
            }
            val data = response.body()?.data ?: break
            if (data.page != page || data.page <= lastPageSeen) {
                Log.w(TAG, "分页异常，返回页=${data.page}，请求页=$page，终止拉取")
                syncPrefs.edit().remove(KEY_RESUME_DELETED_PAGE).apply()
                break
            }
            lastPageSeen = data.page
            val currentPageRecords = data.deletedRecords
            records.addAll(currentPageRecords)
            Log.d(TAG, "拉取删除记录: page=$page, 当前页=${currentPageRecords.size}, 累计=${records.size}, 总数=${data.total}")

            if (currentPageRecords.isEmpty()) {
                consecutiveEmptyPages++
                if (consecutiveEmptyPages >= 3) {
                    Log.w(TAG, "连续3页无数据，终止拉取")
                    break
                }
            } else {
                consecutiveEmptyPages = 0
            }

            if (data.page * data.pageSize >= data.total || currentPageRecords.isEmpty()) {
                syncPrefs.edit().remove(KEY_RESUME_DELETED_PAGE).apply()
                break
            }
            page += 1
            syncPrefs.edit().putInt(KEY_RESUME_DELETED_PAGE, page).apply()
        }
        if (page > maxPages) {
            Log.e(TAG, "达到最大页数限制: $maxPages")
        }
        return FetchResult(records, false, null)
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
            
            // 处理冲突：如果有冲突的记录，重新拉取最新版本
            val ackData = ackResponse.body()?.data
            val conflicts = ackData?.conflicts
            if (conflicts != null) {
                val hasConflicts = conflicts.items.isNotEmpty() ||
                    conflicts.categories.isNotEmpty() ||
                    conflicts.warehouses.isNotEmpty() ||
                    conflicts.shoppingItems.isNotEmpty() ||
                    conflicts.activityEvents.isNotEmpty()
                
                if (hasConflicts) {
                    Log.d(TAG, "检测到冲突，重新拉取冲突记录")
                    resolveConflicts(
                        conflicts,
                        apiService,
                        itemDao,
                        categoryDao,
                        warehouseDao,
                        shoppingItemDao,
                        activityEventDao
                    )
                }
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
        // 过滤示例数据：只包含非示例数据
        val items = itemDao.getAllItemsList()
            .filter { !it.isSample }
            .map {
                SyncSnapshotEntry(it.uuid, dateFormat.format(it.updatedAt))
            }
        val categories = categoryDao.getAllCategoriesSync()
            .map {
                SyncSnapshotEntry(it.uuid, null)
            }
        val warehouses = warehouseDao.getAllWarehousesSync()
            .filter { !it.isSample }
            .map {
                SyncSnapshotEntry(it.uuid, dateFormat.format(it.createdAt))
            }
        val shoppingItems = shoppingItemDao.getAllShoppingItemsSync()
            .filter { !it.isSample }
            .map {
                val updatedAt = it.completedAt ?: it.createdAt
                SyncSnapshotEntry(it.uuid, dateFormat.format(updatedAt))
            }
        val events = activityEventDao.getRecentEventsSync(MAX_SYNC_ACTIVITY_EVENTS).map {
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
            val entity = warehouseFromDto(dto, warehouseDao.getWarehouseByUuid(dto.uuid), warehouseDao)
            warehouseDao.insertWarehouse(entity)
            val inserted = warehouseDao.getWarehouseByUuid(entity.uuid) ?: entity
            if (dto.imageUri != null && inserted.imageUri.isNullOrBlank()) {
                val localPath = downloadImage(RetrofitClient.getApiService(context), dto.imageUri)
                if (localPath != null) {
                    warehouseDao.updateWarehouse(inserted.copy(imageUri = localPath))
                }
            }
        }
        payload.shoppingItems.forEach { dto ->
            val entity = shoppingItemFromDto(dto, shoppingItemDao.getShoppingItemByUuid(dto.uuid), null)
            shoppingItemDao.insertShoppingItem(entity)
            val inserted = shoppingItemDao.getShoppingItemByUuid(entity.uuid) ?: entity
            if (dto.imageUri != null && inserted.imageUri.isNullOrBlank()) {
                val localPath = downloadImage(RetrofitClient.getApiService(context), dto.imageUri)
                if (localPath != null) {
                    shoppingItemDao.updateShoppingItem(inserted.copy(imageUri = localPath))
                }
            }
        }
        payload.items.forEach { dto ->
            val entity = itemFromDto(dto, itemDao.getItemByUuid(dto.uuid), warehouseDao, categoryDao)
            itemDao.insertItem(entity)
            val inserted = itemDao.getItemByUuid(entity.uuid) ?: entity
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
                targetUuid = null,
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
            // 双重检查：确保不是示例数据
            if (item.isSample) {
                Log.d(TAG, "跳过示例物品: ${item.name} (UUID: $uuid)")
                continue
            }
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
            // 双重检查：确保不是示例数据
            if (warehouse.isSample) {
                Log.d(TAG, "跳过示例容器: ${warehouse.name} (UUID: $uuid)")
                continue
            }
            warehouses.add(warehouseToDto(warehouse, warehouseDao))
        }

        val shoppingItems = mutableListOf<ShoppingItemDto>()
        for (uuid in plan.shoppingItems) {
            val shoppingItem = shoppingItemDao.getShoppingItemByUuid(uuid) ?: continue
            // 双重检查：确保不是示例数据
            if (shoppingItem.isSample) {
                Log.d(TAG, "跳过示例购物项: ${shoppingItem.name} (UUID: $uuid)")
                continue
            }
            shoppingItems.add(shoppingItemToDto(shoppingItem))
        }

        val eventsMap = activityEventDao.getRecentEventsSync(MAX_SYNC_ACTIVITY_EVENTS).associateBy { it.uuid }
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
        // 只同步配色相关的设置
        val keysToSync = prefs.all.keys.filter { key ->
            key.startsWith("custom_color_") ||
                key == "color_scheme" ||
                key == "color_scheme_prev" ||
                key == "custom_color_selected" ||
                key == "custom_color_schemes"
        }
        val data = keysToSync.associateWith { key ->
            prefs.all[key]?.toString() ?: ""
        }
        // 如果没有配色数据，返回null表示不需要同步
        if (data.isEmpty()) {
            return null
        }
        // 使用当前时间作为更新时间
        val updatedAt = syncPrefs.getString(KEY_SETTINGS_UPDATED_AT, null) 
            ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
        return SettingsSnapshot(data = data, updatedAt = updatedAt)
    }

    private fun applySettingsSnapshot(snapshot: SettingsSnapshot?) {
        if (snapshot == null) return
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            // 不清空所有设置，只更新配色相关的设置
            snapshot.data.forEach { (key, value) ->
                if (key.startsWith("custom_color_") || 
                    key == "color_scheme" || 
                    key == "color_scheme_prev" ||
                    key == "custom_color_selected" ||
                    key == "custom_color_schemes") {
                    putString(key, value)
                }
            }
            apply()
        }
        syncPrefs.edit().putString(KEY_SETTINGS_UPDATED_AT, snapshot.updatedAt).apply()
        Log.d(TAG, "应用配色设置成功: ${snapshot.data.size} 项")
    }
    
    // ==================== 离线队列支持 ====================
    
    /**
     * 将实体加入同步队列（用于 Excel 导入等未走 Repository 的本地变更）。
     * 仅登录状态下入队；后续由 SyncQueueWorker 上传。
     */
    suspend fun enqueueForSync(
        entityType: String,
        entityUuid: String,
        operation: SyncOperation,
        entity: Any
    ) {
        if (!shouldSyncToRemote()) return
        addToOfflineQueue(entityType, entityUuid, operation, entity)
    }
    
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
     * 同步动态到远端
     */
    suspend fun syncActivityEventToRemote(event: ActivityEvent): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            val apiService = RetrofitClient.getApiService(context)
            val dto = ActivityEventDto(
                uuid = event.uuid,
                type = event.type.name,
                title = event.title,
                description = event.description,
                targetUuid = event.targetUuid,
                targetName = event.targetName,
                iconType = event.iconType,
                createdAt = dateFormat.format(event.createdAt),
                metadata = event.metadata
            )
            val response = apiService.upsertActivityEvent(dto)
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "动态同步成功: ${event.uuid}")
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "同步失败"
                Log.e(TAG, "动态同步失败: $error")
                addToOfflineQueue("activity_event", event.uuid, SyncOperation.UPDATE, event)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "动态同步异常", e)
            addToOfflineQueue("activity_event", event.uuid, SyncOperation.UPDATE, event)
            Result.failure(e)
        }
    }

    /**
     * 同步提醒到远端
     */
    suspend fun syncReminderToRemote(reminder: ItemReminder): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            val apiService = RetrofitClient.getApiService(context)
            val dto = reminderToDto(reminder)
            val response = apiService.upsertReminder(dto)
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "提醒同步成功: ${reminder.uuid}")
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "同步失败"
                Log.e(TAG, "提醒同步失败: $error")
                // 添加到离线队列
                addToOfflineQueue("reminder", reminder.uuid, SyncOperation.UPDATE, reminder)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "提醒同步异常", e)
            // 添加到离线队列
            addToOfflineQueue("reminder", reminder.uuid, SyncOperation.UPDATE, reminder)
            Result.failure(e)
        }
    }

    /**
     * 从远端删除提醒
     */
    suspend fun deleteReminderFromRemote(uuid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!shouldSyncToRemote()) {
                return@withContext Result.success(Unit)
            }
            val apiService = RetrofitClient.getApiService(context)
            val response = apiService.deleteReminder(uuid)
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "提醒删除同步成功: $uuid")
                Result.success(Unit)
            } else {
                val error = response.body()?.error?.message ?: "删除同步失败"
                Log.e(TAG, "提醒删除同步失败: $error")
                // 添加到离线队列
                addToOfflineQueue("reminder", uuid, SyncOperation.DELETE, mapOf("uuid" to uuid))
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "提醒删除同步异常", e)
            // 添加到离线队列
            addToOfflineQueue("reminder", uuid, SyncOperation.DELETE, mapOf("uuid" to uuid))
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
    
    /**
     * 解决冲突：重新拉取冲突的记录并更新本地数据库
     */
    private suspend fun resolveConflicts(
        conflicts: com.example.itemremindertool.network.dto.SyncConflicts,
        apiService: com.example.itemremindertool.network.ApiService,
        itemDao: com.example.itemremindertool.data.dao.ItemDao,
        categoryDao: com.example.itemremindertool.data.dao.CategoryDao,
        warehouseDao: com.example.itemremindertool.data.dao.WarehouseDao,
        shoppingItemDao: com.example.itemremindertool.data.dao.ShoppingItemDao,
        activityEventDao: com.example.itemremindertool.data.dao.ActivityEventDao
    ) {
        // 拉取冲突的物品
        for (uuid in conflicts.items) {
            try {
                val response = apiService.getItem(uuid)
                if (response.isSuccessful && response.body()?.success == true) {
                    val dto = response.body()?.data ?: continue
                    // 检查是否为示例数据（通过名称模式）
                    if (isSampleDataName(dto.name)) {
                        Log.d(TAG, "跳过示例物品冲突: ${dto.name} (UUID: $uuid)")
                        continue
                    }
                    val existing = itemDao.getItemByUuid(uuid)
                    val entity = itemFromDto(dto, existing, warehouseDao, categoryDao)
                    // 确保不会将示例数据标记为非示例
                    if (existing?.isSample == true) {
                        Log.d(TAG, "本地示例物品冲突，跳过更新: ${dto.name} (UUID: $uuid)")
                        continue
                    }
                    itemDao.insertItem(entity)
                    downloadRemoteImagesIfNeeded(entity, dto.imageUris ?: emptyList(), itemDao)
                    Log.d(TAG, "已解决物品冲突: $uuid")
                }
            } catch (e: Exception) {
                Log.e(TAG, "拉取冲突物品失败: $uuid", e)
            }
        }
        
        // 拉取冲突的分类
        for (uuid in conflicts.categories) {
            try {
                val response = apiService.getCategory(uuid)
                if (response.isSuccessful && response.body()?.success == true) {
                    val dto = response.body()?.data ?: continue
                    val existing = categoryDao.getCategoryByUuid(uuid)
                    val entity = categoryFromDto(dto, existing)
                    categoryDao.insertCategory(entity)
                    Log.d(TAG, "已解决分类冲突: $uuid")
                }
            } catch (e: Exception) {
                Log.e(TAG, "拉取冲突分类失败: $uuid", e)
            }
        }
        
        // 拉取冲突的容器
        for (uuid in conflicts.warehouses) {
            try {
                val response = apiService.getWarehouse(uuid)
                if (response.isSuccessful && response.body()?.success == true) {
                    val dto = response.body()?.data ?: continue
                    // 检查是否为示例数据（通过名称模式）
                    if (isSampleDataName(dto.name)) {
                        Log.d(TAG, "跳过示例容器冲突: ${dto.name} (UUID: $uuid)")
                        continue
                    }
                    val existing = warehouseDao.getWarehouseByUuid(uuid)
                    val entity = warehouseFromDto(dto, existing, warehouseDao)
                    // 确保不会将示例数据标记为非示例
                    if (existing?.isSample == true) {
                        Log.d(TAG, "本地示例容器冲突，跳过更新: ${dto.name} (UUID: $uuid)")
                        continue
                    }
                    warehouseDao.insertWarehouse(entity)
                    val inserted = warehouseDao.getWarehouseByUuid(entity.uuid) ?: entity
                    if (dto.imageUri != null && inserted.imageUri.isNullOrBlank()) {
                        val localPath = downloadImage(apiService, dto.imageUri)
                        if (localPath != null) {
                            warehouseDao.updateWarehouse(inserted.copy(imageUri = localPath))
                        }
                    }
                    Log.d(TAG, "已解决容器冲突: $uuid")
                }
            } catch (e: Exception) {
                Log.e(TAG, "拉取冲突容器失败: $uuid", e)
            }
        }
        
        // 拉取冲突的购物项
        for (uuid in conflicts.shoppingItems) {
            try {
                val response = apiService.getShoppingItem(uuid)
                if (response.isSuccessful && response.body()?.success == true) {
                    val dto = response.body()?.data ?: continue
                    // 检查是否为示例数据（通过名称模式）
                    if (isSampleDataName(dto.name)) {
                        Log.d(TAG, "跳过示例购物项冲突: ${dto.name} (UUID: $uuid)")
                        continue
                    }
                    val existing = shoppingItemDao.getShoppingItemByUuid(uuid)
                    val entity = shoppingItemFromDto(dto, existing, dto.itemUuid)
                    // 确保不会将示例数据标记为非示例
                    if (existing?.isSample == true) {
                        Log.d(TAG, "本地示例购物项冲突，跳过更新: ${dto.name} (UUID: $uuid)")
                        continue
                    }
                    shoppingItemDao.insertShoppingItem(entity)
                    val inserted = shoppingItemDao.getShoppingItemByUuid(entity.uuid) ?: entity
                    if (dto.imageUri != null && inserted.imageUri.isNullOrBlank()) {
                        val localPath = downloadImage(apiService, dto.imageUri)
                        if (localPath != null) {
                            shoppingItemDao.updateShoppingItem(inserted.copy(imageUri = localPath))
                        }
                    }
                    Log.d(TAG, "已解决购物项冲突: $uuid")
                }
            } catch (e: Exception) {
                Log.e(TAG, "拉取冲突购物项失败: $uuid", e)
            }
        }
        
        // 活动事件冲突：由于活动事件是只读的，冲突时直接使用服务器版本
        // 这里可以选择删除本地冲突的事件，或者保留（因为事件通常不会修改）
        Log.d(TAG, "活动事件冲突数量: ${conflicts.activityEvents.size}")
    }
}
