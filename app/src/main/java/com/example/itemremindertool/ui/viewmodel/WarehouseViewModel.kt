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

    private val _uiState = MutableStateFlow<WarehouseUiState>(WarehouseUiState())
    val uiState: StateFlow<WarehouseUiState> = _uiState.asStateFlow()

    fun loadWarehouse(warehouseId: Long) {
        viewModelScope.launch {
            val warehouse = warehouseRepository.getWarehouseById(warehouseId)
            _uiState.value = _uiState.value.copy(selectedWarehouse = warehouse)
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
}

data class WarehouseUiState(
    val selectedWarehouse: Warehouse? = null,
    val warehouseItems: List<com.example.itemremindertool.data.model.Item> = emptyList(),
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

