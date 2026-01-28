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

    @Query("SELECT * FROM warehouses WHERE uuid = :uuid LIMIT 1")
    suspend fun getWarehouseByUuid(uuid: String): Warehouse?

    @Query("SELECT * FROM warehouses WHERE parentUuid IS NULL ORDER BY name ASC")
    fun getTopLevelWarehouses(): Flow<List<Warehouse>>

    @Query("SELECT * FROM warehouses WHERE parentUuid = :parentUuid ORDER BY name ASC")
    fun getChildWarehouses(parentUuid: String): Flow<List<Warehouse>>

    @Query("SELECT * FROM warehouses WHERE parentUuid = :parentUuid ORDER BY name ASC")
    suspend fun getChildWarehousesSync(parentUuid: String): List<Warehouse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: Warehouse): Long

    @Update
    suspend fun updateWarehouse(warehouse: Warehouse): Int

    @Delete
    suspend fun deleteWarehouse(warehouse: Warehouse): Int

    @Query("DELETE FROM warehouses WHERE uuid = :uuid")
    suspend fun deleteWarehouseByUuid(uuid: String): Int
}

