package com.example.itemremindertool.data.repository

import android.content.Context
import androidx.room.Transaction
import com.example.itemremindertool.data.dao.WarehouseDao
import com.example.itemremindertool.data.dao.DeletedRecordDao
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.data.model.DeletedRecord
import com.example.itemremindertool.data.database.AppDatabase
import java.util.Date
import kotlinx.coroutines.flow.Flow

class WarehouseRepository(
    private val warehouseDao: WarehouseDao,
    private val deletedRecordDao: DeletedRecordDao? = null,
    private val itemDao: com.example.itemremindertool.data.dao.ItemDao? = null,
    private val context: Context? = null
) {
    fun getAllWarehouses(): Flow<List<Warehouse>> = warehouseDao.getAllWarehouses()

    suspend fun getAllWarehousesSync(): List<Warehouse> = warehouseDao.getAllWarehousesSync()

    fun getTopLevelWarehouses(): Flow<List<Warehouse>> = warehouseDao.getTopLevelWarehouses()

    suspend fun getWarehouseById(id: Long): Warehouse? = warehouseDao.getWarehouseById(id)

    fun getChildWarehouses(parentId: Long): Flow<List<Warehouse>> = warehouseDao.getChildWarehouses(parentId)

    suspend fun getChildWarehousesSync(parentId: Long): List<Warehouse> = warehouseDao.getChildWarehousesSync(parentId)

    suspend fun insertWarehouse(warehouse: Warehouse): Long {
        val warehouseId = warehouseDao.insertWarehouse(warehouse)
        // 记录动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_ADDED,
                    title = it.getString(com.example.itemremindertool.R.string.event_created_warehouse),
                    description = warehouse.name,
                    targetId = warehouseId,
                    targetName = warehouse.name,
                    iconType = "add_warehouse",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
            } catch (e: Exception) {
                android.util.Log.e("WarehouseRepository", "记录创建容器动态失败", e)
            }
        }
        return warehouseId
    }

    suspend fun updateWarehouse(warehouse: Warehouse) {
        warehouseDao.updateWarehouse(warehouse)
        // 记录动态
        context?.let {
            try {
                val activityEventDao = AppDatabase.getDatabase(it).activityEventDao()
                val event = com.example.itemremindertool.data.model.ActivityEvent(
                    type = com.example.itemremindertool.data.model.ActivityEventType.WAREHOUSE_UPDATED,
                    title = it.getString(com.example.itemremindertool.R.string.event_updated_warehouse),
                    description = warehouse.name,
                    targetId = warehouse.id,
                    targetName = warehouse.name,
                    iconType = "update_warehouse",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
            } catch (e: Exception) {
                android.util.Log.e("WarehouseRepository", "记录更新容器动态失败", e)
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
        var childWarehouseCount = 0
        var itemCount = 0
        
        // 1. 递归删除所有子容器
        val childIds = getAllChildWarehouseIds(warehouse.id)
        childWarehouseCount = childIds.size
        
        // 先删除子容器中的物品
        childIds.forEach { childId ->
            itemDao?.let { dao ->
                val count = dao.getItemCountByWarehouse(childId)
                itemCount += count
                dao.deleteItemsByWarehouse(childId)
            }
            // 删除子容器
            warehouseDao.deleteWarehouseById(childId)
            // 记录删除操作
            deletedRecordDao?.insertDeletedRecord(
                DeletedRecord(
                    entityType = "warehouse",
                    entityId = childId,
                    deletedAt = Date()
                )
            )
        }
        
        // 2. 删除当前容器中的物品
        itemDao?.let { dao ->
            val count = dao.getItemCountByWarehouse(warehouse.id)
            itemCount += count
            dao.deleteItemsByWarehouse(warehouse.id)
        }
        
        // 3. 删除当前容器
        warehouseDao.deleteWarehouse(warehouse)
        // 记录删除操作
        deletedRecordDao?.insertDeletedRecord(
            DeletedRecord(
                entityType = "warehouse",
                entityId = warehouse.id,
                deletedAt = Date()
            )
        )
        
        return Pair(childWarehouseCount, itemCount)
    }
    
    /**
     * 获取删除容器时的统计信息（子容器数量和物品数量）
     */
    suspend fun getDeleteStatistics(warehouse: Warehouse): Pair<Int, Int> {
        val childIds = getAllChildWarehouseIds(warehouse.id)
        var itemCount = 0
        
        // 统计所有子容器中的物品
        childIds.forEach { childId ->
            itemDao?.let { dao ->
                itemCount += dao.getItemCountByWarehouse(childId)
            }
        }
        
        // 统计当前容器中的物品
        itemDao?.let { dao ->
            itemCount += dao.getItemCountByWarehouse(warehouse.id)
        }
        
        return Pair(childIds.size, itemCount)
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
                    targetId = warehouse.id,
                    targetName = warehouse.name,
                    iconType = "delete_warehouse",
                    createdAt = Date()
                )
                activityEventDao.insert(event)
            } catch (e: Exception) {
                android.util.Log.e("WarehouseRepository", "记录删除容器动态失败", e)
            }
        }
    }

    @androidx.room.Transaction
    suspend fun deleteWarehouseById(id: Long) {
        // 在事务中删除数据和记录删除操作，确保原子性
        warehouseDao.deleteWarehouseById(id)
        // 记录删除操作
        deletedRecordDao?.insertDeletedRecord(
            DeletedRecord(
                entityType = "warehouse",
                entityId = id,
                deletedAt = Date()
            )
        )
    }

    /**
     * 获取容器的完整路径（从顶层到当前容器）
     */
    suspend fun getWarehousePath(warehouseId: Long): List<Warehouse> {
        val path = mutableListOf<Warehouse>()
        var current: Warehouse? = getWarehouseById(warehouseId)
        val visitedIds = mutableSetOf<Long>() // 防止循环引用
        
        while (current != null && !visitedIds.contains(current.id)) {
            visitedIds.add(current.id)
            path.add(0, current) // 添加到开头，保持从顶层到当前的顺序
            
            // 防止无限循环：如果路径过长（超过5层），停止
            if (path.size > 5) {
                break
            }
            
            current = current.parentId?.let { parentId ->
                if (visitedIds.contains(parentId)) {
                    null // 检测到循环，停止
                } else {
                    getWarehouseById(parentId)
                }
            }
        }
        
        return path
    }

    /**
     * 递归获取所有子容器的ID（包括子容器的子容器）
     */
    suspend fun getAllChildWarehouseIds(parentId: Long): List<Long> {
        val allIds = mutableListOf<Long>()
        val directChildren = getChildWarehousesSync(parentId)
        
        directChildren.forEach { child ->
            allIds.add(child.id)
            // 递归获取子容器的子容器
            allIds.addAll(getAllChildWarehouseIds(child.id))
        }
        
        return allIds
    }
}

