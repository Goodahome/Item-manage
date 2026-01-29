package com.example.itemremindertool.data.repository

import android.content.Context
import androidx.room.Transaction
import com.example.itemremindertool.data.dao.WarehouseDao
import com.example.itemremindertool.data.dao.DeletedRecordDao
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.data.model.DeletedRecord
import com.example.itemremindertool.data.database.AppDatabase
import com.example.itemremindertool.sync.SyncManager
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WarehouseRepository(
    private val warehouseDao: WarehouseDao,
    private val deletedRecordDao: DeletedRecordDao? = null,
    private val itemDao: com.example.itemremindertool.data.dao.ItemDao? = null,
    private val context: Context? = null
) {
    private val syncManager: SyncManager? by lazy {
        context?.let { SyncManager.getInstance(it) }
    }
    fun getAllWarehouses(): Flow<List<Warehouse>> = warehouseDao.getAllWarehouses()

    suspend fun getAllWarehousesSync(): List<Warehouse> = warehouseDao.getAllWarehousesSync()

    fun getTopLevelWarehouses(): Flow<List<Warehouse>> = warehouseDao.getTopLevelWarehouses()

    fun getChildWarehouses(parentUuid: String): Flow<List<Warehouse>> = warehouseDao.getChildWarehouses(parentUuid)

    suspend fun getChildWarehousesSync(parentUuid: String): List<Warehouse> = warehouseDao.getChildWarehousesSync(parentUuid)

    suspend fun insertWarehouse(warehouse: Warehouse) {
        // 1. 本地写入
        warehouseDao.insertWarehouse(warehouse)
        val savedWarehouse = warehouseDao.getWarehouseByUuid(warehouse.uuid) ?: warehouse
        
        // 2. 记录动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_ADDED,
                    title = it.getString(com.example.itemremindertool.R.string.event_created_warehouse),
                    description = warehouse.name,
                    targetUuid = savedWarehouse.uuid,
                    targetName = warehouse.name,
                    iconType = "add_warehouse",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
                syncManager?.let { manager ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            manager.syncActivityEventToRemote(event)
                        } catch (e: Exception) {
                            android.util.Log.e("WarehouseRepository", "同步创建容器动态失败", e)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WarehouseRepository", "记录创建容器动态失败", e)
            }
        }
        
        // 3. 远端同步（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncWarehouseToRemote(savedWarehouse)
                } catch (e: Exception) {
                    android.util.Log.e("WarehouseRepository", "同步容器到远端失败", e)
                }
            }
        }
    }

    suspend fun updateWarehouse(warehouse: Warehouse) {
        // 1. 本地更新
        warehouseDao.updateWarehouse(warehouse)
        
        // 2. 记录动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_UPDATED,
                    title = it.getString(com.example.itemremindertool.R.string.event_updated_warehouse),
                    description = warehouse.name,
                    targetUuid = warehouse.uuid,
                    targetName = warehouse.name,
                    iconType = "update_warehouse",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
                syncManager?.let { manager ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            manager.syncActivityEventToRemote(event)
                        } catch (e: Exception) {
                            android.util.Log.e("WarehouseRepository", "同步更新容器动态失败", e)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WarehouseRepository", "记录更新容器动态失败", e)
            }
        }
        
        // 3. 远端同步（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncWarehouseToRemote(warehouse)
                } catch (e: Exception) {
                    android.util.Log.e("WarehouseRepository", "同步容器到远端失败", e)
                }
            }
        }
    }

    suspend fun updateWarehouseSilently(warehouse: Warehouse) {
        warehouseDao.updateWarehouse(warehouse)
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    manager.syncWarehouseToRemote(warehouse)
                } catch (e: Exception) {
                    android.util.Log.e("WarehouseRepository", "静默同步容器失败", e)
                }
            }
        }
    }

    /**
     * 递归删除容器及其所有子容器和物品
     * @param warehouse 要删除的容器
     * @return 删除统计信息 (子容器数量, 物品数量)
     */
    @androidx.room.Transaction
    suspend fun deleteWarehouseRecursively(warehouse: Warehouse): Pair<Int, Int> {
        val rootWarehouse = warehouseDao.getWarehouseByUuid(warehouse.uuid) ?: warehouse
        var childWarehouseCount = 0
        var itemCount = 0

        val warehouseUuidsToDelete = mutableListOf<String>()
        val itemUuidsToDelete = mutableListOf<String>()
        
        // 1. 递归获取所有子容器
        val childWarehouseUuids = getAllChildWarehouseUuids(rootWarehouse.uuid)
        val childWarehouses = childWarehouseUuids.mapNotNull { uuid -> 
            warehouseDao.getWarehouseByUuid(uuid)
        }
        childWarehouseCount = childWarehouses.size

        childWarehouses.forEach { child ->
            warehouseUuidsToDelete.add(child.uuid)
            itemDao?.let { dao ->
                val count = dao.getItemCountByWarehouse(child.uuid)
                itemCount += count
                val items = dao.getItemsByWarehouseSync(child.uuid)
                itemUuidsToDelete.addAll(items.filterNot { it.isSample }.map { it.uuid })
                dao.deleteItemsByWarehouse(child.uuid)
            }
            warehouseDao.deleteWarehouseByUuid(child.uuid)
            deletedRecordDao?.insertDeletedRecord(
                DeletedRecord(
                    entityType = "warehouse",
                    entityUuid = child.uuid,
                    deletedAt = Date()
                )
            )
        }
        
        // 2. 删除当前容器中的物品
        itemDao?.let { dao ->
            val count = dao.getItemCountByWarehouse(rootWarehouse.uuid)
            itemCount += count
            val items = dao.getItemsByWarehouseSync(rootWarehouse.uuid)
            itemUuidsToDelete.addAll(items.filterNot { it.isSample }.map { it.uuid })
            dao.deleteItemsByWarehouse(rootWarehouse.uuid)
        }
        
        // 3. 删除当前容器
        warehouseDao.deleteWarehouse(rootWarehouse)
        deletedRecordDao?.insertDeletedRecord(
            DeletedRecord(
                entityType = "warehouse",
                entityUuid = rootWarehouse.uuid,
                deletedAt = Date()
            )
        )
        
        // 4. 远端同步删除（异步，不阻塞）
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    itemUuidsToDelete.distinct().forEach { itemUuid ->
                        manager.deleteItemFromRemote(itemUuid)
                    }
                    childWarehouses.forEach { w ->
                        manager.deleteWarehouseFromRemote(w.uuid)
                    }
                    manager.deleteWarehouseFromRemote(rootWarehouse.uuid)
                } catch (e: Exception) {
                    android.util.Log.e("WarehouseRepository", "同步删除容器到远端失败", e)
                }
            }
        }
        
        return Pair(childWarehouseCount, itemCount)
    }
    
    /**
     * 获取删除容器时的统计信息（子容器数量和物品数量）
     */
    suspend fun getDeleteStatistics(warehouse: Warehouse): Pair<Int, Int> {
        val childWarehouseUuids = getAllChildWarehouseUuids(warehouse.uuid)
        val childWarehouses = childWarehouseUuids.mapNotNull { uuid -> 
            warehouseDao.getWarehouseByUuid(uuid)
        }
        var itemCount = 0
        
        childWarehouses.forEach { child ->
            itemDao?.let { dao ->
                itemCount += dao.getItemCountByWarehouse(child.uuid)
            }
        }
        
        itemDao?.let { dao ->
            itemCount += dao.getItemCountByWarehouse(warehouse.uuid)
        }
        
        return Pair(childWarehouses.size, itemCount)
    }

    /**
     * 删除子容器，将其中的物品移动到父容器
     * @param warehouse 要删除的子容器
     */
    @androidx.room.Transaction
    suspend fun deleteSubWarehouse(warehouse: Warehouse) {
        val parentUuid = warehouse.parentUuid
        if (parentUuid == null) {
            deleteWarehouse(warehouse)
            return
        }
        val parentWarehouse = getWarehouseByUuid(parentUuid)
        if (parentWarehouse == null) {
            deleteWarehouse(warehouse)
            return
        }
        val parentWarehouseUuid = parentWarehouse.uuid
        val childUuids = getAllChildWarehouseUuids(warehouse.uuid)
        val warehouseUuidsToDelete = mutableListOf<String>().apply {
            add(warehouse.uuid)
            addAll(childUuids)
        }
        val movedItems = mutableListOf<com.example.itemremindertool.data.model.Item>()
        
        itemDao?.let { dao ->
            val itemsFlow = dao.getItemsByWarehouse(warehouse.uuid)
            val itemList = itemsFlow.first()
            itemList.forEach { item ->
                val updatedItem = item.copy(
                    warehouseUuid = parentWarehouseUuid,
                    updatedAt = Date()
                )
                dao.updateItem(updatedItem)
                movedItems.add(updatedItem)
            }
        }
        
        childUuids.forEach { childUuid ->
            val childWarehouse = getWarehouseByUuid(childUuid)
            itemDao?.let { dao ->
                childWarehouse?.let { c ->
                    val itemsFlow = dao.getItemsByWarehouse(c.uuid)
                    val itemList = itemsFlow.first()
                    itemList.forEach { item ->
                        val updatedItem = item.copy(
                            warehouseUuid = parentWarehouseUuid,
                            updatedAt = Date()
                        )
                        dao.updateItem(updatedItem)
                        movedItems.add(updatedItem)
                    }
                }
            }
            warehouseDao.deleteWarehouseByUuid(childUuid)
            deletedRecordDao?.insertDeletedRecord(
                DeletedRecord(
                    entityType = "warehouse",
                    entityUuid = childUuid,
                    deletedAt = Date()
                )
            )
        }
        
        warehouseDao.deleteWarehouse(warehouse)
        deletedRecordDao?.insertDeletedRecord(
            DeletedRecord(
                entityType = "warehouse",
                entityUuid = warehouse.uuid,
                deletedAt = Date()
            )
        )
        
        syncManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    movedItems.filterNot { it.isSample }.forEach { item ->
                        manager.syncItemToRemote(item)
                    }
                    warehouseUuidsToDelete.forEach { uuid ->
                        manager.deleteWarehouseFromRemote(uuid)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WarehouseRepository", "同步删除子容器到远端失败", e)
                }
            }
        }

        // 5. 记录动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_DELETED,
                    title = it.getString(com.example.itemremindertool.R.string.event_deleted_warehouse),
                    description = warehouse.name,
                    targetUuid = warehouse.uuid,
                    targetName = warehouse.name,
                    iconType = "delete_warehouse",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
                syncManager?.let { manager ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            manager.syncActivityEventToRemote(event)
                        } catch (e: Exception) {
                            android.util.Log.e("WarehouseRepository", "同步删除容器动态失败", e)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WarehouseRepository", "记录删除容器动态失败", e)
            }
        }
    }

    @androidx.room.Transaction
    suspend fun deleteWarehouse(warehouse: Warehouse) {
        // 使用递归删除方法
        deleteWarehouseRecursively(warehouse)
        // 记录动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_DELETED,
                    title = it.getString(com.example.itemremindertool.R.string.event_deleted_warehouse),
                    description = warehouse.name,
                    targetUuid = warehouse.uuid,
                    targetName = warehouse.name,
                    iconType = "delete_warehouse",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
                syncManager?.let { manager ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            manager.syncActivityEventToRemote(event)
                        } catch (e: Exception) {
                            android.util.Log.e("WarehouseRepository", "同步删除容器动态失败", e)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WarehouseRepository", "记录删除容器动态失败", e)
            }
        }
    }

    @androidx.room.Transaction
    suspend fun deleteWarehouseByUuid(uuid: String) {
        val warehouse = warehouseDao.getWarehouseByUuid(uuid)
        warehouseDao.deleteWarehouseByUuid(uuid)
        if (warehouse != null) {
            deletedRecordDao?.insertDeletedRecord(
                DeletedRecord(
                    entityType = "warehouse",
                    entityUuid = warehouse.uuid,
                    deletedAt = Date()
                )
            )
        }
    }
    
    suspend fun getWarehouseByUuid(uuid: String): Warehouse? = warehouseDao.getWarehouseByUuid(uuid)

    /**
     * 获取容器的完整路径（从顶层到当前容器）
     */
    suspend fun getWarehousePath(warehouseUuid: String): List<Warehouse> {
        val path = mutableListOf<Warehouse>()
        var current: Warehouse? = getWarehouseByUuid(warehouseUuid)
        val visitedUuids = mutableSetOf<String>()
        
        while (current != null && !visitedUuids.contains(current.uuid)) {
            visitedUuids.add(current.uuid)
            path.add(0, current)
            if (path.size > 5) break
            current = current.parentUuid?.let { pu ->
                val parent = getWarehouseByUuid(pu)
                if (parent != null && visitedUuids.contains(parent.uuid)) null else parent
            }
        }
        return path
    }
    
    /**
     * 递归获取所有子容器的UUID（包括子容器的子容器）
     */
    suspend fun getAllChildWarehouseUuids(parentUuid: String): List<String> {
        val allUuids = mutableListOf<String>()
        val directChildren = getChildWarehousesSync(parentUuid)
        
        directChildren.forEach { child ->
            allUuids.add(child.uuid)
            // 递归获取子容器的子容器
            allUuids.addAll(getAllChildWarehouseUuids(child.uuid))
        }
        
        return allUuids
    }
}

