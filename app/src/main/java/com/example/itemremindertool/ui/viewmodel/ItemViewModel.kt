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

    // 操作状态
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

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

    fun insertItem(item: Item, onSuccess: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Saving
                val itemId = itemRepository.insertItem(item.copy(updatedAt = Date()))
                _operationState.value = OperationState.Success("保存成功")
                onSuccess?.invoke(itemId) // 回调返回插入后的 ID
                kotlinx.coroutines.delay(2000) // 成功消息显示2秒
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                // 如果是连接池关闭错误，等待一下然后重试（可能是数据库正在恢复）
                if (e.message?.contains("connection pool has been closed") == true) {
                    kotlinx.coroutines.delay(1000) // 等待1秒让数据库恢复
                    try {
                        val itemId = itemRepository.insertItem(item.copy(updatedAt = Date()))
                        _operationState.value = OperationState.Success("保存成功")
                        onSuccess?.invoke(itemId) // 回调返回插入后的 ID
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = OperationState.Idle
                    } catch (retryException: Exception) {
                        _operationState.value = OperationState.Error("保存失败: ${retryException.message}")
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = OperationState.Idle
                    }
                } else {
                    _operationState.value = OperationState.Error("保存失败: ${e.message}")
                    kotlinx.coroutines.delay(2000) // 错误消息也显示2秒
                    _operationState.value = OperationState.Idle
                }
            }
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Saving
                itemRepository.updateItem(item.copy(updatedAt = Date()))
                _operationState.value = OperationState.Success("更新成功")
                kotlinx.coroutines.delay(2000) // 成功消息显示2秒
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                // 如果是连接池关闭错误，等待一下然后重试（可能是数据库正在恢复）
                if (e.message?.contains("connection pool has been closed") == true) {
                    kotlinx.coroutines.delay(1000) // 等待1秒让数据库恢复
                    try {
                        itemRepository.updateItem(item.copy(updatedAt = Date()))
                        _operationState.value = OperationState.Success("更新成功")
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = OperationState.Idle
                    } catch (retryException: Exception) {
                        _operationState.value = OperationState.Error("更新失败: ${retryException.message}")
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = OperationState.Idle
                    }
                } else {
                    _operationState.value = OperationState.Error("更新失败: ${e.message}")
                    kotlinx.coroutines.delay(2000) // 错误消息也显示2秒
                    _operationState.value = OperationState.Idle
                }
            }
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Deleting
                itemRepository.deleteItem(item)
                _operationState.value = OperationState.Success("删除成功")
                kotlinx.coroutines.delay(2000) // 成功消息显示2秒
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("删除失败: ${e.message}")
                kotlinx.coroutines.delay(2000) // 错误消息也显示2秒
                _operationState.value = OperationState.Idle
            }
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

sealed class OperationState {
    object Idle : OperationState()
    object Saving : OperationState()
    object Deleting : OperationState()
    object Syncing : OperationState() // 云端同步中
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}

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

