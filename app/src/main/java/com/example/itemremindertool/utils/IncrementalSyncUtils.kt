package com.example.itemremindertool.utils

import android.content.Context
import android.util.Log
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.data.model.Category
import com.example.itemremindertool.data.model.ItemReminder
import com.example.itemremindertool.data.model.DeletedRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 增量同步工具类
 * 用于比较并合并本地和云端数据，只同步不同的数据
 */
object IncrementalSyncUtils {
    private const val TAG = "IncrementalSyncUtils"
    
    /**
     * 增量合并云端数据到本地
     * 策略：
     * 1. 如果本地没有该数据，则添加
     * 2. 如果本地有该数据，比较 updatedAt，保留最新的
     * 3. 不会删除本地存在但云端不存在的数据（以本地为主）
     */
    suspend fun incrementalMergeCloudData(
        context: Context,
        cloudDatabasePath: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 打开云端数据库（临时数据库文件）
            val cloudDbFile = File(cloudDatabasePath)
            if (!cloudDbFile.exists()) {
                return@withContext Result.failure(IllegalStateException("云端数据库文件不存在: $cloudDatabasePath"))
            }
            
            val cloudDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                cloudDatabasePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY or android.database.sqlite.SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )
            
            val localDb = AppDatabase.getDatabase(context)
            var mergedCount = 0
            
            try {
                // 1. 合并物品数据
                val cloudItems = getCloudItems(cloudDb)
                val localItems = localDb.itemDao().getAllItemsList()
                val localItemsMap = localItems.associateBy { it.uuid }
                
                // 获取本地删除记录
                val deletedItems = localDb.deletedRecordDao().getDeletedRecordsByType("item")
                val deletedItemUuids = deletedItems.map { it.entityUuid }.toSet()
                
                for (cloudItem in cloudItems) {
                    // 如果该物品在删除记录中，说明本地主动删除过，不恢复
                    if (deletedItemUuids.contains(cloudItem.uuid)) {
                        Log.d(TAG, "跳过已删除的物品: ${cloudItem.name} (UUID: ${cloudItem.uuid})")
                        continue
                    }
                    
                    val localItem = localItemsMap[cloudItem.uuid]
                    if (localItem == null) {
                        // 本地没有，直接添加
                        localDb.itemDao().insertItem(cloudItem)
                        mergedCount++
                        Log.d(TAG, "添加物品: ${cloudItem.name}")
                    } else {
                        // 本地有，比较 updatedAt，保留最新的
                        val cloudTime = cloudItem.updatedAt?.time ?: 0L
                        val localTime = localItem.updatedAt?.time ?: 0L
                        if (cloudTime > localTime) {
                            localDb.itemDao().updateItem(cloudItem)
                            mergedCount++
                            Log.d(TAG, "更新物品: ${cloudItem.name}")
                        }
                    }
                }
                
                // 2. 合并容器数据（Warehouse 没有时间戳字段，通过名称和描述比较）
                val cloudWarehouses = getCloudWarehouses(cloudDb)
                val localWarehouses = localDb.warehouseDao().getAllWarehousesSync()
                val localWarehousesMap = localWarehouses.associateBy { it.uuid }
                
                // 获取本地删除记录
                val deletedWarehouses = localDb.deletedRecordDao().getDeletedRecordsByType("warehouse")
                val deletedWarehouseUuids = deletedWarehouses.map { it.entityUuid }.toSet()
                
                for (cloudWarehouse in cloudWarehouses) {
                    // 如果该容器在删除记录中，说明本地主动删除过，不恢复
                    if (deletedWarehouseUuids.contains(cloudWarehouse.uuid)) {
                        Log.d(TAG, "跳过已删除的容器: ${cloudWarehouse.name} (UUID: ${cloudWarehouse.uuid})")
                        continue
                    }
                    
                    val localWarehouse = localWarehousesMap[cloudWarehouse.uuid]
                    if (localWarehouse == null) {
                        localDb.warehouseDao().insertWarehouse(cloudWarehouse)
                        mergedCount++
                        Log.d(TAG, "添加容器: ${cloudWarehouse.name}")
                    } else {
                        // Warehouse 没有 updatedAt，比较名称和描述是否有变化
                        if (cloudWarehouse.name != localWarehouse.name || 
                            cloudWarehouse.description != localWarehouse.description ||
                            cloudWarehouse.parentUuid != localWarehouse.parentUuid) {
                            localDb.warehouseDao().updateWarehouse(cloudWarehouse)
                            mergedCount++
                            Log.d(TAG, "更新容器: ${cloudWarehouse.name}")
                        }
                    }
                }
                
                // 3. 合并分类数据（Category 没有时间戳字段，通过名称和颜色比较）
                val cloudCategories = getCloudCategories(cloudDb)
                val localCategories = localDb.categoryDao().getAllCategoriesSync()
                val localCategoriesMap = localCategories.associateBy { it.uuid }
                
                for (cloudCategory in cloudCategories) {
                    val localCategory = localCategoriesMap[cloudCategory.uuid]
                    if (localCategory == null) {
                        localDb.categoryDao().insertCategory(cloudCategory)
                        mergedCount++
                        Log.d(TAG, "添加分类: ${cloudCategory.name}")
                    } else {
                        // Category 没有 updatedAt，比较名称和颜色是否有变化
                        if (cloudCategory.name != localCategory.name || 
                            cloudCategory.color != localCategory.color) {
                            localDb.categoryDao().updateCategory(cloudCategory)
                            mergedCount++
                            Log.d(TAG, "更新分类: ${cloudCategory.name}")
                        }
                    }
                }
                
                // 4. 合并购物车数据（ShoppingItem 使用 createdAt 比较）
                val cloudShoppingItems = getCloudShoppingItems(cloudDb)
                val localShoppingItems = localDb.shoppingItemDao().getAllShoppingItemsSync()
                val localShoppingItemsMap = localShoppingItems.associateBy { it.uuid }
                
                for (cloudShoppingItem in cloudShoppingItems) {
                    val localShoppingItem = localShoppingItemsMap[cloudShoppingItem.uuid]
                    if (localShoppingItem == null) {
                        localDb.shoppingItemDao().insertShoppingItem(cloudShoppingItem)
                        mergedCount++
                        Log.d(TAG, "添加购物项: ${cloudShoppingItem.name}")
                    } else {
                        // ShoppingItem 使用 createdAt 作为更新时间比较
                        val cloudTime = cloudShoppingItem.createdAt.time
                        val localTime = localShoppingItem.createdAt.time
                        if (cloudTime > localTime) {
                            localDb.shoppingItemDao().updateShoppingItem(cloudShoppingItem)
                            mergedCount++
                            Log.d(TAG, "更新购物项: ${cloudShoppingItem.name}")
                        }
                    }
                }
                
                // 5. 合并提醒数据
                val cloudReminders = getCloudReminders(cloudDb)
                val localReminders = localDb.itemReminderDao().getAllRemindersSync()
                val localRemindersMap = localReminders.associateBy { it.uuid }
                
                for (cloudReminder in cloudReminders) {
                    val localReminder = localRemindersMap[cloudReminder.uuid]
                    if (localReminder == null) {
                        localDb.itemReminderDao().insertReminder(cloudReminder)
                        mergedCount++
                        Log.d(TAG, "添加提醒: ${cloudReminder.uuid}")
                    } else {
                        val cloudTime = cloudReminder.updatedAt.time
                        val localTime = localReminder.updatedAt.time
                        if (cloudTime > localTime) {
                            localDb.itemReminderDao().updateReminder(cloudReminder)
                            mergedCount++
                            Log.d(TAG, "更新提醒: ${cloudReminder.uuid}")
                        }
                    }
                }
                
                // 6. 合并删除记录（确保删除操作在多设备间同步）
                val cloudDeletedRecords = getCloudDeletedRecords(cloudDb)
                val localDeletedRecords = localDb.deletedRecordDao().getAllDeletedRecords()
                val localDeletedRecordsMap = localDeletedRecords.associateBy { "${it.entityType}_${it.entityUuid}" }
                
                for (cloudRecord in cloudDeletedRecords) {
                    val key = "${cloudRecord.entityType}_${cloudRecord.entityUuid}"
                    val localRecord = localDeletedRecordsMap[key]
                    if (localRecord == null) {
                        // 云端有删除记录但本地没有，添加删除记录
                        localDb.deletedRecordDao().insertDeletedRecord(cloudRecord)
                        // 如果本地还有对应的数据，需要删除它
                        when (cloudRecord.entityType) {
                            "item" -> {
                                try {
                                    val item = localDb.itemDao().getItemByUuid(cloudRecord.entityUuid)
                                    if (item != null) {
                                        localDb.itemDao().deleteItem(item)
                                        Log.d(TAG, "根据云端删除记录删除物品: ${cloudRecord.entityUuid}")
                                    }
                                } catch (e: Exception) {
                                    // 物品可能已经不存在，忽略
                                }
                            }
                            "warehouse" -> {
                                try {
                                    val warehouse = localDb.warehouseDao().getWarehouseByUuid(cloudRecord.entityUuid)
                                    if (warehouse != null) {
                                        localDb.warehouseDao().deleteWarehouse(warehouse)
                                        Log.d(TAG, "根据云端删除记录删除容器: ${cloudRecord.entityUuid}")
                                    }
                                } catch (e: Exception) {
                                    // 容器可能已经不存在，忽略
                                }
                            }
                        }
                        mergedCount++
                    } else {
                        // 如果云端的删除时间更新，更新本地删除记录
                        if (cloudRecord.deletedAt.time > localRecord.deletedAt.time) {
                            localDb.deletedRecordDao().insertDeletedRecord(cloudRecord)
                            // 如果本地还有对应的数据，需要删除它
                            when (cloudRecord.entityType) {
                                "item" -> {
                                    try {
                                        val item = localDb.itemDao().getItemByUuid(cloudRecord.entityUuid)
                                        if (item != null) {
                                            localDb.itemDao().deleteItem(item)
                                            Log.d(TAG, "根据云端删除记录删除物品: ${cloudRecord.entityUuid}")
                                        }
                                    } catch (e: Exception) {
                                        // 物品可能已经不存在，忽略
                                    }
                                }
                                "warehouse" -> {
                                    try {
                                        val warehouse = localDb.warehouseDao().getWarehouseByUuid(cloudRecord.entityUuid)
                                        if (warehouse != null) {
                                            localDb.warehouseDao().deleteWarehouse(warehouse)
                                            Log.d(TAG, "根据云端删除记录删除容器: ${cloudRecord.entityUuid}")
                                        }
                                    } catch (e: Exception) {
                                        // 容器可能已经不存在，忽略
                                    }
                                }
                            }
                            mergedCount++
                        }
                    }
                }
                
                Log.d(TAG, "增量同步完成，共合并 $mergedCount 条数据")
                Result.success(mergedCount)
            } finally {
                cloudDb.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "增量同步失败", e)
            Result.failure(e)
        }
    }
    
    private fun getCloudItems(db: android.database.sqlite.SQLiteDatabase): List<Item> {
        val items = mutableListOf<Item>()
        val cursor = db.query("items", null, null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                try {
                    val item = parseItemFromCursor(it)
                    items.add(item)
                } catch (e: Exception) {
                    Log.e(TAG, "解析物品失败", e)
                }
            }
        }
        return items
    }
    
    private fun getCloudWarehouses(db: android.database.sqlite.SQLiteDatabase): List<Warehouse> {
        val warehouses = mutableListOf<Warehouse>()
        val cursor = db.query("warehouses", null, null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                try {
                    val warehouse = parseWarehouseFromCursor(it)
                    warehouses.add(warehouse)
                } catch (e: Exception) {
                    Log.e(TAG, "解析容器失败", e)
                }
            }
        }
        return warehouses
    }
    
    private fun getCloudCategories(db: android.database.sqlite.SQLiteDatabase): List<Category> {
        val categories = mutableListOf<Category>()
        val cursor = db.query("categories", null, null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                try {
                    val category = parseCategoryFromCursor(it)
                    categories.add(category)
                } catch (e: Exception) {
                    Log.e(TAG, "解析分类失败", e)
                }
            }
        }
        return categories
    }
    
    private fun getCloudShoppingItems(db: android.database.sqlite.SQLiteDatabase): List<ShoppingItem> {
        val items = mutableListOf<ShoppingItem>()
        val cursor = db.query("shopping_items", null, null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                try {
                    val item = parseShoppingItemFromCursor(it)
                    items.add(item)
                } catch (e: Exception) {
                    Log.e(TAG, "解析购物项失败", e)
                }
            }
        }
        return items
    }
    
    private fun getCloudReminders(db: android.database.sqlite.SQLiteDatabase): List<ItemReminder> {
        val reminders = mutableListOf<ItemReminder>()
        val cursor = db.query("item_reminders", null, null, null, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                try {
                    val reminder = parseReminderFromCursor(it)
                    reminders.add(reminder)
                } catch (e: Exception) {
                    Log.e(TAG, "解析提醒失败", e)
                }
            }
        }
        return reminders
    }
    
    // 解析方法根据实际的数据模型实现
    private fun parseItemFromCursor(cursor: android.database.Cursor): Item {
        // Item 的 status 是计算属性，不是数据库字段，不需要从数据库读取
        val uuidCol = cursor.getColumnIndex("uuid")
        val warehouseUuidCol = cursor.getColumnIndex("warehouseUuid")
        val categoryUuidCol = cursor.getColumnIndex("categoryUuid")
        val expiryDateCol = cursor.getColumnIndexOrThrow("expiryDate")
        val purchaseDateCol = cursor.getColumnIndex("purchaseDate")
        val barcodeCol = cursor.getColumnIndex("barcode")
        val imageUriCol = cursor.getColumnIndex("imageUri")
        val imageUrisCol = cursor.getColumnIndex("imageUris")
        val imageKeysCol = cursor.getColumnIndex("imageKeys")
        val priceCol = cursor.getColumnIndex("price")
        val featureCodeCol = cursor.getColumnIndex("featureCode")
        val tagsCol = cursor.getColumnIndex("tags")
        val isSampleCol = cursor.getColumnIndex("isSample")
        val primaryImageIndexCol = cursor.getColumnIndex("primaryImageIndex")
        
        return Item(
            uuid = if (uuidCol >= 0 && !cursor.isNull(uuidCol)) cursor.getString(uuidCol) else java.util.UUID.randomUUID().toString(),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
            quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
            warehouseUuid = if (warehouseUuidCol >= 0 && !cursor.isNull(warehouseUuidCol)) cursor.getString(warehouseUuidCol) else null,
            categoryUuid = if (categoryUuidCol >= 0 && !cursor.isNull(categoryUuidCol)) cursor.getString(categoryUuidCol) else null,
            expiryDate = if (expiryDateCol >= 0 && !cursor.isNull(expiryDateCol)) java.util.Date(cursor.getLong(expiryDateCol)) else null,
            purchaseDate = if (purchaseDateCol >= 0 && !cursor.isNull(purchaseDateCol)) java.util.Date(cursor.getLong(purchaseDateCol)) else null,
            price = if (priceCol >= 0 && !cursor.isNull(priceCol)) cursor.getDouble(priceCol) else null,
            barcode = if (barcodeCol >= 0 && !cursor.isNull(barcodeCol)) cursor.getString(barcodeCol) else null,
            imageUri = if (imageUriCol >= 0 && !cursor.isNull(imageUriCol)) cursor.getString(imageUriCol) else null,
            imageUris = if (imageUrisCol >= 0 && !cursor.isNull(imageUrisCol)) {
                try {
                    val imageUrisJson = cursor.getString(imageUrisCol)
                    if (imageUrisJson.isNullOrEmpty() || imageUrisJson == "[]") {
                        emptyList()
                    } else {
                        emptyList() // 暂时返回空列表，需要JSON解析
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList(),
            imageKeys = if (imageKeysCol >= 0 && !cursor.isNull(imageKeysCol)) {
                try {
                    val imageKeysJson = cursor.getString(imageKeysCol)
                    if (imageKeysJson.isNullOrEmpty() || imageKeysJson == "[]") {
                        emptyList()
                    } else {
                        emptyList() // 暂时返回空列表，需要JSON解析
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList(),
            featureCode = if (featureCodeCol >= 0 && !cursor.isNull(featureCodeCol)) cursor.getString(featureCodeCol) else null,
            enableStockAlert = cursor.getInt(cursor.getColumnIndexOrThrow("enableStockAlert")) != 0,
            tags = if (tagsCol >= 0 && !cursor.isNull(tagsCol)) {
                // tags 可能存储为 JSON 字符串，需要解析
                try {
                    val tagsJson = cursor.getString(tagsCol)
                    if (tagsJson.isNullOrEmpty() || tagsJson == "[]") {
                        emptyList()
                    } else {
                        // 尝试解析 JSON 数组格式的字符串
                        // 这里简化处理，实际可能需要完整的 JSON 解析
                        emptyList() // 暂时返回空列表，tags 不是关键字段
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList(),
            isSample = if (isSampleCol >= 0) cursor.getInt(isSampleCol) != 0 else false,
            primaryImageIndex = if (primaryImageIndexCol >= 0) cursor.getInt(primaryImageIndexCol) else 0,
            createdAt = java.util.Date(cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))),
            updatedAt = java.util.Date(cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")))
        )
    }
    
    private fun parseWarehouseFromCursor(cursor: android.database.Cursor): Warehouse {
        val descriptionCol = cursor.getColumnIndex("description")
        val locationCol = cursor.getColumnIndex("location")
        val capacityCol = cursor.getColumnIndex("capacity")
        val parentUuidCol = cursor.getColumnIndex("parentUuid")
        val uuidCol = cursor.getColumnIndex("uuid")
        val imageUriCol = cursor.getColumnIndex("imageUri")
        val imageKeyCol = cursor.getColumnIndex("imageKey")
        val createdAtCol = cursor.getColumnIndex("createdAt")
        val isSampleCol = cursor.getColumnIndex("isSample")
        
        return Warehouse(
            uuid = if (uuidCol >= 0 && !cursor.isNull(uuidCol)) cursor.getString(uuidCol) else java.util.UUID.randomUUID().toString(),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            description = if (descriptionCol >= 0 && !cursor.isNull(descriptionCol)) cursor.getString(descriptionCol) else "",
            location = if (locationCol >= 0 && !cursor.isNull(locationCol)) cursor.getString(locationCol) else "",
            capacity = if (capacityCol >= 0 && !cursor.isNull(capacityCol)) cursor.getInt(capacityCol) else null,
            parentUuid = if (parentUuidCol >= 0 && !cursor.isNull(parentUuidCol)) cursor.getString(parentUuidCol) else null,
            level = cursor.getInt(cursor.getColumnIndexOrThrow("level")),
            imageUri = if (imageUriCol >= 0 && !cursor.isNull(imageUriCol)) cursor.getString(imageUriCol) else null,
            imageKey = if (imageKeyCol >= 0 && !cursor.isNull(imageKeyCol)) cursor.getString(imageKeyCol) else null,
            createdAt = if (createdAtCol >= 0 && !cursor.isNull(createdAtCol)) java.util.Date(cursor.getLong(createdAtCol)) else java.util.Date(),
            isSample = if (isSampleCol >= 0) cursor.getInt(isSampleCol) != 0 else false
        )
    }
    
    private fun parseCategoryFromCursor(cursor: android.database.Cursor): Category {
        val uuidCol = cursor.getColumnIndex("uuid")
        val descriptionCol = cursor.getColumnIndex("description")
        val colorCol = cursor.getColumnIndex("color")
        val iconCol = cursor.getColumnIndex("icon")
        
        return Category(
            uuid = if (uuidCol >= 0 && !cursor.isNull(uuidCol)) cursor.getString(uuidCol) else java.util.UUID.randomUUID().toString(),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            description = if (descriptionCol >= 0 && !cursor.isNull(descriptionCol)) cursor.getString(descriptionCol) else "",
            color = if (colorCol >= 0 && !cursor.isNull(colorCol)) cursor.getString(colorCol) else "#6200EE",
            icon = if (iconCol >= 0 && !cursor.isNull(iconCol)) cursor.getString(iconCol) else "category"
        )
    }
    
    private fun parseShoppingItemFromCursor(cursor: android.database.Cursor): ShoppingItem {
        val uuidCol = cursor.getColumnIndex("uuid")
        val descriptionCol = cursor.getColumnIndex("description")
        val itemUuidCol = cursor.getColumnIndex("itemUuid")
        val imageUriCol = cursor.getColumnIndex("imageUri")
        val imageKeyCol = cursor.getColumnIndex("imageKey")
        val completedAtCol = cursor.getColumnIndex("completedAt")
        val isSampleCol = cursor.getColumnIndex("isSample")
        
        return ShoppingItem(
            uuid = if (uuidCol >= 0 && !cursor.isNull(uuidCol)) cursor.getString(uuidCol) else java.util.UUID.randomUUID().toString(),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            description = if (descriptionCol >= 0 && !cursor.isNull(descriptionCol)) cursor.getString(descriptionCol) else "",
            quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
            isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow("isCompleted")) != 0,
            priority = com.example.itemremindertool.data.model.Priority.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("priority"))),
            itemUuid = if (itemUuidCol >= 0 && !cursor.isNull(itemUuidCol)) cursor.getString(itemUuidCol) else null,
            imageUri = if (imageUriCol >= 0 && !cursor.isNull(imageUriCol)) cursor.getString(imageUriCol) else null,
            imageKey = if (imageKeyCol >= 0 && !cursor.isNull(imageKeyCol)) cursor.getString(imageKeyCol) else null,
            createdAt = java.util.Date(cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))),
            completedAt = if (completedAtCol >= 0 && !cursor.isNull(completedAtCol)) java.util.Date(cursor.getLong(completedAtCol)) else null,
            isSample = if (isSampleCol >= 0) cursor.getInt(isSampleCol) != 0 else false
        )
    }
    
    private fun parseReminderFromCursor(cursor: android.database.Cursor): ItemReminder {
        val uuidCol = cursor.getColumnIndex("uuid")
        val itemUuidCol = cursor.getColumnIndex("itemUuid")
        val reminderTimeCol = cursor.getColumnIndex("reminderTime")
        val dailyTimeCol = cursor.getColumnIndex("dailyTime")
        val monthlyDayCol = cursor.getColumnIndex("monthlyDay")
        val monthlyTimeCol = cursor.getColumnIndex("monthlyTime")
        val yearlyMonthCol = cursor.getColumnIndex("yearlyMonth")
        val yearlyDayCol = cursor.getColumnIndex("yearlyDay")
        val yearlyTimeCol = cursor.getColumnIndex("yearlyTime")
        
        return ItemReminder(
            uuid = if (uuidCol >= 0 && !cursor.isNull(uuidCol)) cursor.getString(uuidCol) else java.util.UUID.randomUUID().toString(),
            itemUuid = if (itemUuidCol >= 0 && !cursor.isNull(itemUuidCol)) cursor.getString(itemUuidCol) else {
                // 兼容旧数据库，尝试从itemId字段获取，但需要先查找对应的UUID
                // 如果找不到，使用空字符串（ItemReminder的itemUuid是必需的，但这里先兼容处理）
                val itemIdCol = cursor.getColumnIndex("itemId")
                if (itemIdCol >= 0 && !cursor.isNull(itemIdCol)) {
                    // 旧数据库可能使用itemId，这里需要查询对应的UUID
                    // 但在这个上下文中无法访问数据库，所以先返回空字符串
                    // 实际使用时应该确保数据库迁移完成
                    ""
                } else {
                    cursor.getString(cursor.getColumnIndexOrThrow("itemUuid"))
                }
            },
            reminderType = com.example.itemremindertool.data.model.ReminderType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("reminderType"))),
            reminderTime = if (reminderTimeCol >= 0 && !cursor.isNull(reminderTimeCol)) java.util.Date(cursor.getLong(reminderTimeCol)) else null,
            dailyTime = if (dailyTimeCol >= 0 && !cursor.isNull(dailyTimeCol)) cursor.getString(dailyTimeCol) else null,
            monthlyDay = if (monthlyDayCol >= 0 && !cursor.isNull(monthlyDayCol) && cursor.getInt(monthlyDayCol) != 0) cursor.getInt(monthlyDayCol) else null,
            monthlyTime = if (monthlyTimeCol >= 0 && !cursor.isNull(monthlyTimeCol)) cursor.getString(monthlyTimeCol) else null,
            yearlyMonth = if (yearlyMonthCol >= 0 && !cursor.isNull(yearlyMonthCol) && cursor.getInt(yearlyMonthCol) != 0) cursor.getInt(yearlyMonthCol) else null,
            yearlyDay = if (yearlyDayCol >= 0 && !cursor.isNull(yearlyDayCol) && cursor.getInt(yearlyDayCol) != 0) cursor.getInt(yearlyDayCol) else null,
            yearlyTime = if (yearlyTimeCol >= 0 && !cursor.isNull(yearlyTimeCol)) cursor.getString(yearlyTimeCol) else null,
            reason = cursor.getString(cursor.getColumnIndexOrThrow("reason")),
            isEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("isEnabled")) != 0,
            createdAt = java.util.Date(cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))),
            updatedAt = java.util.Date(cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")))
        )
    }
    
    private fun getCloudDeletedRecords(db: android.database.sqlite.SQLiteDatabase): List<DeletedRecord> {
        val records = mutableListOf<DeletedRecord>()
        try {
            val cursor = db.query("deleted_records", null, null, null, null, null, null)
            cursor.use {
                while (it.moveToNext()) {
                    try {
                        val uuidCol = it.getColumnIndex("uuid")
                        val entityUuidCol = it.getColumnIndex("entityUuid")
                        val entityIdCol = it.getColumnIndex("entityId")
                        
                        // 优先使用entityUuid，如果没有则尝试从entityId转换（需要查询对应表的UUID）
                        val entityUuid = if (entityUuidCol >= 0 && !it.isNull(entityUuidCol)) {
                            it.getString(entityUuidCol)
                        } else if (entityIdCol >= 0 && !it.isNull(entityIdCol)) {
                            // 旧数据库可能使用entityId，需要查询对应的UUID
                            // 这里简化处理，实际应该查询对应表的UUID
                            val entityType = it.getString(it.getColumnIndexOrThrow("entityType"))
                            val entityId = it.getLong(entityIdCol)
                            // 尝试从对应表查询UUID
                            when (entityType) {
                                "item" -> {
                                    val itemCursor = db.query("items", arrayOf("uuid"), "id = ?", arrayOf(entityId.toString()), null, null, null)
                                    itemCursor.use { ic ->
                                        if (ic.moveToFirst()) {
                                            ic.getString(ic.getColumnIndexOrThrow("uuid"))
                                        } else {
                                            null
                                        }
                                    } ?: ""
                                }
                                "warehouse" -> {
                                    val warehouseCursor = db.query("warehouses", arrayOf("uuid"), "id = ?", arrayOf(entityId.toString()), null, null, null)
                                    warehouseCursor.use { wc ->
                                        if (wc.moveToFirst()) {
                                            wc.getString(wc.getColumnIndexOrThrow("uuid"))
                                        } else {
                                            null
                                        }
                                    } ?: ""
                                }
                                "category" -> {
                                    val categoryCursor = db.query("categories", arrayOf("uuid"), "id = ?", arrayOf(entityId.toString()), null, null, null)
                                    categoryCursor.use { cc ->
                                        if (cc.moveToFirst()) {
                                            cc.getString(cc.getColumnIndexOrThrow("uuid"))
                                        } else {
                                            null
                                        }
                                    } ?: ""
                                }
                                else -> ""
                            }
                        } else {
                            ""
                        }
                        
                        val record = DeletedRecord(
                            uuid = if (uuidCol >= 0 && !it.isNull(uuidCol)) it.getString(uuidCol) else java.util.UUID.randomUUID().toString(),
                            entityType = it.getString(it.getColumnIndexOrThrow("entityType")),
                            entityUuid = entityUuid,
                            deletedAt = java.util.Date(it.getLong(it.getColumnIndexOrThrow("deletedAt")))
                        )
                        records.add(record)
                    } catch (e: Exception) {
                        Log.e(TAG, "解析删除记录失败", e)
                    }
                }
            }
        } catch (e: Exception) {
            // 如果表不存在（旧版本数据库），忽略
            Log.d(TAG, "删除记录表不存在，跳过删除记录同步")
        }
        return records
    }
}

