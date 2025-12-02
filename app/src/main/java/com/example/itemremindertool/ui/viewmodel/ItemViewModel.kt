package com.example.itemremindertool.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemremindertool.data.model.Item
import com.example.itemremindertool.data.model.ItemStatus
import com.example.itemremindertool.data.repository.CategoryRepository
import com.example.itemremindertool.data.repository.ItemRepository
import com.example.itemremindertool.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ItemViewModel(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val warehouseRepository: WarehouseRepository
) : ViewModel() {

    val items = itemRepository.getAllItems()

    private val _uiState = MutableStateFlow<ItemUiState>(ItemUiState())
    val uiState: StateFlow<ItemUiState> = _uiState.asStateFlow()

    // 临时存储识别得到的特征码
    private val _pendingFeatureCode = MutableStateFlow<String?>(null)
    val pendingFeatureCode: StateFlow<String?> = _pendingFeatureCode.asStateFlow()
    
    fun setPendingFeatureCode(featureCode: String?) {
        _pendingFeatureCode.value = featureCode
    }
    
    fun clearPendingFeatureCode() {
        _pendingFeatureCode.value = null
    }

    fun loadItem(itemId: Long) {
        viewModelScope.launch {
            val item = itemRepository.getItemById(itemId)
            _uiState.value = _uiState.value.copy(selectedItem = item)
        }
    }

    fun insertItem(item: Item) {
        viewModelScope.launch {
            itemRepository.insertItem(item.copy(updatedAt = Date()))
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            itemRepository.updateItem(item.copy(updatedAt = Date()))
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            itemRepository.deleteItem(item)
        }
    }

    fun getItemByBarcode(barcode: String, onResult: (Item?) -> Unit) {
        viewModelScope.launch {
            val item = itemRepository.getItemByBarcode(barcode)
            onResult(item)
        }
    }
    
    fun searchItemsByName(query: String) = itemRepository.searchItemsByName(query)
    
    suspend fun getAllItemsList(): List<Item> = itemRepository.getAllItemsList()
}

data class ItemUiState(
    val selectedItem: Item? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ItemViewModelFactory(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val warehouseRepository: WarehouseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(itemRepository, categoryRepository, warehouseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

