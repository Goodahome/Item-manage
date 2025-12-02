package com.example.itemremindertool.data.repository

import com.example.itemremindertool.data.dao.WarehouseDao
import com.example.itemremindertool.data.model.Warehouse
import kotlinx.coroutines.flow.Flow

class WarehouseRepository(private val warehouseDao: WarehouseDao) {
    fun getAllWarehouses(): Flow<List<Warehouse>> = warehouseDao.getAllWarehouses()

    fun getTopLevelWarehouses(): Flow<List<Warehouse>> = warehouseDao.getTopLevelWarehouses()

    suspend fun getWarehouseById(id: Long): Warehouse? = warehouseDao.getWarehouseById(id)

    fun getChildWarehouses(parentId: Long): Flow<List<Warehouse>> = warehouseDao.getChildWarehouses(parentId)

    suspend fun getChildWarehousesSync(parentId: Long): List<Warehouse> = warehouseDao.getChildWarehousesSync(parentId)

    suspend fun insertWarehouse(warehouse: Warehouse): Long = warehouseDao.insertWarehouse(warehouse)

    suspend fun updateWarehouse(warehouse: Warehouse) = warehouseDao.updateWarehouse(warehouse)

    suspend fun deleteWarehouse(warehouse: Warehouse) = warehouseDao.deleteWarehouse(warehouse)

    suspend fun deleteWarehouseById(id: Long) = warehouseDao.deleteWarehouseById(id)

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

