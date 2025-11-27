package com.example.itemremindertool.data.repository

import com.example.itemremindertool.data.dao.WarehouseDao
import com.example.itemremindertool.data.model.Warehouse
import kotlinx.coroutines.flow.Flow

class WarehouseRepository(private val warehouseDao: WarehouseDao) {
    fun getAllWarehouses(): Flow<List<Warehouse>> = warehouseDao.getAllWarehouses()

    suspend fun getWarehouseById(id: Long): Warehouse? = warehouseDao.getWarehouseById(id)

    suspend fun insertWarehouse(warehouse: Warehouse): Long = warehouseDao.insertWarehouse(warehouse)

    suspend fun updateWarehouse(warehouse: Warehouse) = warehouseDao.updateWarehouse(warehouse)

    suspend fun deleteWarehouse(warehouse: Warehouse) = warehouseDao.deleteWarehouse(warehouse)

    suspend fun deleteWarehouseById(id: Long) = warehouseDao.deleteWarehouseById(id)
}

