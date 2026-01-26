package com.example.itemremindertool.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.data.repository.ItemRepository
import com.example.itemremindertool.data.repository.WarehouseRepository
import com.example.itemremindertool.ui.viewmodel.OperationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WarehouseViewModel(
    application: Application,
    private val warehouseRepository: WarehouseRepository,
    private val itemRepository: ItemRepository
) : AndroidViewModel(application) {

    val warehouses = warehouseRepository.getAllWarehouses()
    val topLevelWarehouses = warehouseRepository.getTopLevelWarehouses()

    private val _uiState = MutableStateFlow<WarehouseUiState>(WarehouseUiState())
    val uiState: StateFlow<WarehouseUiState> = _uiState.asStateFlow()
    
    // 操作状态
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    fun loadWarehouse(warehouseId: Long) {
        viewModelScope.launch {
            try {
                val warehouse = warehouseRepository.getWarehouseById(warehouseId)
                if (warehouse != null) {
                    val path = try {
                        warehouseRepository.getWarehousePath(warehouseId)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val children = try {
                        warehouseRepository.getChildWarehousesSync(warehouseId)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    _uiState.value = _uiState.value.copy(
                        selectedWarehouse = warehouse,
                        warehousePath = path,
                        childWarehouses = children
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        selectedWarehouse = null,
                        warehousePath = emptyList(),
                        childWarehouses = emptyList()
                    )
                }
            } catch (e: Exception) {
                // 处理异常，避免崩溃
                _uiState.value = _uiState.value.copy(
                    selectedWarehouse = null,
                    warehousePath = emptyList(),
                    childWarehouses = emptyList(),
                    error = e.message
                )
            }
        }
    }

    fun loadChildWarehouses(parentId: Long) {
        viewModelScope.launch {
            val children = warehouseRepository.getChildWarehousesSync(parentId)
            _uiState.value = _uiState.value.copy(childWarehouses = children)
        }
    }

    fun loadWarehouseItems(warehouseId: Long) {
        viewModelScope.launch {
            itemRepository.getItemsByWarehouse(warehouseId).collect { items ->
                _uiState.value = _uiState.value.copy(warehouseItems = items)
            }
        }
    }

    fun insertWarehouse(warehouse: Warehouse) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Saving
                warehouseRepository.insertWarehouse(warehouse)
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_save_success)
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.operation_save_failed,
                        e.message ?: ""
                    )
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            }
        }
    }

    fun updateWarehouse(warehouse: Warehouse) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Saving
                warehouseRepository.updateWarehouse(warehouse)
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_update_success)
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.operation_update_failed,
                        e.message ?: ""
                    )
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            }
        }
    }

    /**
     * 获取删除容器时的统计信息
     */
    suspend fun getDeleteStatistics(warehouse: Warehouse): Pair<Int, Int> {
        return warehouseRepository.getDeleteStatistics(warehouse)
    }
    
    fun deleteWarehouse(warehouse: Warehouse) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Deleting
                warehouseRepository.deleteWarehouse(warehouse)
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_delete_success)
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.operation_delete_failed,
                        e.message ?: ""
                    )
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            }
        }
    }
    
    /**
     * 删除子容器，将其中的物品移动到父容器
     */
    fun deleteSubWarehouse(warehouse: Warehouse) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Deleting
                warehouseRepository.deleteSubWarehouse(warehouse)
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_delete_success)
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.operation_delete_failed,
                        e.message ?: ""
                    )
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            }
        }
    }

    /**
     * 计算容器的层级（基于父容器）
     *
     * 注意：这里不做最大层级裁剪，仅返回真实层级，
     * 上层通过设置（是否开启无限容器模式）来决定是否允许保存。
     */
    suspend fun calculateLevel(parentId: Long?): Int {
        if (parentId == null) return 1 // 顶层容器
        val parent = warehouseRepository.getWarehouseById(parentId) ?: return 1
        return parent.level + 1
    }
}

data class WarehouseUiState(
    val selectedWarehouse: Warehouse? = null,
    val warehouseItems: List<com.example.itemremindertool.data.model.Item> = emptyList(),
    val warehousePath: List<Warehouse> = emptyList(), // 容器路径（面包屑）
    val childWarehouses: List<Warehouse> = emptyList(), // 子容器列表
    val isLoading: Boolean = false,
    val error: String? = null
)

class WarehouseViewModelFactory(
    private val application: Application,
    private val warehouseRepository: WarehouseRepository,
    private val itemRepository: ItemRepository
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarehouseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WarehouseViewModel(application, warehouseRepository, itemRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

