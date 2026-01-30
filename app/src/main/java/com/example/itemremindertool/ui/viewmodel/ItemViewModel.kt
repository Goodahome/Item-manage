package com.example.itemremindertool.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    application: Application,
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val warehouseRepository: WarehouseRepository
) : AndroidViewModel(application) {

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

    fun loadItemByUuid(itemUuid: String) {
        viewModelScope.launch {
            val item = itemRepository.getItemByUuid(itemUuid)
            _uiState.value = _uiState.value.copy(selectedItem = item)
        }
    }

    fun insertItem(item: Item, onSuccess: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Saving
                itemRepository.insertItem(item.copy(updatedAt = Date()))
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_save_success)
                )
                onSuccess?.invoke(item.uuid)
                kotlinx.coroutines.delay(2000) // 成功消息显示2秒
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                // 如果是连接池关闭错误，等待一下然后重试（可能是数据库正在恢复）
                if (e.message?.contains("connection pool has been closed") == true) {
                    kotlinx.coroutines.delay(1000) // 等待1秒让数据库恢复
                    try {
                        itemRepository.insertItem(item.copy(updatedAt = Date()))
                        _operationState.value = OperationState.Success(
                            getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_save_success)
                        )
                        onSuccess?.invoke(item.uuid)
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = OperationState.Idle
                    } catch (retryException: Exception) {
                        _operationState.value = OperationState.Error(
                            getApplication<Application>().getString(
                                com.example.itemremindertool.R.string.operation_save_failed,
                                retryException.message ?: ""
                            )
                        )
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = OperationState.Idle
                    }
                } else {
                    _operationState.value = OperationState.Error(
                        getApplication<Application>().getString(
                            com.example.itemremindertool.R.string.operation_save_failed,
                            e.message ?: ""
                        )
                    )
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
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_update_success)
                )
                kotlinx.coroutines.delay(2000) // 成功消息显示2秒
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                // 如果是连接池关闭错误，等待一下然后重试（可能是数据库正在恢复）
                if (e.message?.contains("connection pool has been closed") == true) {
                    kotlinx.coroutines.delay(1000) // 等待1秒让数据库恢复
                    try {
                        itemRepository.updateItem(item.copy(updatedAt = Date()))
                        _operationState.value = OperationState.Success(
                            getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_update_success)
                        )
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = OperationState.Idle
                    } catch (retryException: Exception) {
                        _operationState.value = OperationState.Error(
                            getApplication<Application>().getString(
                                com.example.itemremindertool.R.string.operation_update_failed,
                                retryException.message ?: ""
                            )
                        )
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = OperationState.Idle
                    }
                } else {
                    _operationState.value = OperationState.Error(
                        getApplication<Application>().getString(
                            com.example.itemremindertool.R.string.operation_update_failed,
                            e.message ?: ""
                        )
                    )
                    kotlinx.coroutines.delay(2000) // 错误消息也显示2秒
                    _operationState.value = OperationState.Idle
                }
            }
        }
    }
    
    /**
     * 使用物品（减少数量并记录使用事件）
     */
    fun useItem(item: Item, usedQuantity: Int) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Saving
                itemRepository.useItem(item, usedQuantity)
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_use_success)
                )
                kotlinx.coroutines.delay(2000) // 成功消息显示2秒
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                // 如果是连接池关闭错误，等待一下然后重试（可能是数据库正在恢复）
                if (e.message?.contains("connection pool has been closed") == true) {
                    kotlinx.coroutines.delay(1000) // 等待1秒让数据库恢复
                    try {
                        itemRepository.useItem(item, usedQuantity)
                        _operationState.value = OperationState.Success(
                            getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_use_success)
                        )
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = OperationState.Idle
                    } catch (retryException: Exception) {
                        _operationState.value = OperationState.Error(
                            getApplication<Application>().getString(
                                com.example.itemremindertool.R.string.operation_use_failed,
                                retryException.message ?: ""
                            )
                        )
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = OperationState.Idle
                    }
                } else {
                    _operationState.value = OperationState.Error(
                        getApplication<Application>().getString(
                            com.example.itemremindertool.R.string.operation_use_failed,
                            e.message ?: ""
                        )
                    )
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
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_delete_success)
                )
                kotlinx.coroutines.delay(2000) // 成功消息显示2秒
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.operation_delete_failed,
                        e.message ?: ""
                    )
                )
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
    
    suspend fun getItemByUuid(itemUuid: String): Item? {
        return itemRepository.getItemByUuid(itemUuid)
    }
    
    fun searchItemsByName(query: String) = itemRepository.searchItemsByName(query)
    
    suspend fun getAllItemsList(): List<Item> = itemRepository.getAllItemsList()
    
    /**
     * 批量删除物品
     */
    fun batchDeleteItems(items: List<Item>) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Deleting
                itemRepository.batchDeleteItems(items)
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.batch_delete_success,
                        items.size
                    )
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.batch_delete_failed,
                        e.message ?: ""
                    )
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            }
        }
    }
    
    /**
     * 批量移动物品
     */
    fun batchMoveItems(items: List<Item>, targetWarehouseUuid: String?, targetWarehouseName: String?) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Saving
                itemRepository.batchMoveItems(items, targetWarehouseUuid, targetWarehouseName)
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.batch_move_success,
                        items.size
                    )
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.batch_move_failed,
                        e.message ?: ""
                    )
                )
                kotlinx.coroutines.delay(2000)
                _operationState.value = OperationState.Idle
            }
        }
    }
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
    private val application: Application,
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val warehouseRepository: WarehouseRepository
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(application, itemRepository, categoryRepository, warehouseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

