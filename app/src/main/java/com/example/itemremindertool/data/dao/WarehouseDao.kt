package com.example.itemremindertool.data.dao

import androidx.room.*
import com.example.itemremindertool.data.model.Warehouse
import kotlinx.coroutines.flow.Flow

@Dao
interface WarehouseDao {
    @Query("SELECT * FROM warehouses ORDER BY name ASC")
    fun getAllWarehouses(): Flow<List<Warehouse>>
    
    @Query("SELECT * FROM warehouses ORDER BY name ASC")
    suspend fun getAllWarehousesSync(): List<Warehouse>

    @Query("SELECT * FROM warehouses WHERE id = :id")
    suspend fun getWarehouseById(id: Long): Warehouse?

    @Query("SELECT * FROM warehouses WHERE parentId IS NULL ORDER BY name ASC")
    fun getTopLevelWarehouses(): Flow<List<Warehouse>>

    @Query("SELECT * FROM warehouses WHERE parentId = :parentId ORDER BY name ASC")
    fun getChildWarehouses(parentId: Long): Flow<List<Warehouse>>

    @Query("SELECT * FROM warehouses WHERE parentId = :parentId ORDER BY name ASC")
    suspend fun getChildWarehousesSync(parentId: Long): List<Warehouse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: Warehouse): Long

    @Update
    suspend fun updateWarehouse(warehouse: Warehouse)

    @Delete
    suspend fun deleteWarehouse(warehouse: Warehouse)

    @Query("DELETE FROM warehouses WHERE id = :id")
    suspend fun deleteWarehouseById(id: Long)
}

