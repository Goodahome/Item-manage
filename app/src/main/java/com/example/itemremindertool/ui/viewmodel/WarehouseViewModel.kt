package com.example.itemremindertool.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemremindertool.data.model.Warehouse
import com.example.itemremindertool.data.repository.ItemRepository
import com.example.itemremindertool.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WarehouseViewModel(
    private val warehouseRepository: WarehouseRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    val warehouses = warehouseRepository.getAllWarehouses()
    val topLevelWarehouses = warehouseRepository.getTopLevelWarehouses()

    private val _uiState = MutableStateFlow<WarehouseUiState>(WarehouseUiState())
    val uiState: StateFlow<WarehouseUiState> = _uiState.asStateFlow()

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
            warehouseRepository.insertWarehouse(warehouse)
        }
    }

    fun updateWarehouse(warehouse: Warehouse) {
        viewModelScope.launch {
            warehouseRepository.updateWarehouse(warehouse)
        }
    }

    fun deleteWarehouse(warehouse: Warehouse) {
        viewModelScope.launch {
            warehouseRepository.deleteWarehouse(warehouse)
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
    private val warehouseRepository: WarehouseRepository,
    private val itemRepository: ItemRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarehouseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WarehouseViewModel(warehouseRepository, itemRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

