package com.example.itemremindertool.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.itemremindertool.data.model.ShoppingItem
import com.example.itemremindertool.data.repository.ShoppingItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Date

class ShoppingItemViewModel(
    application: Application,
    private val shoppingItemRepository: ShoppingItemRepository
) : AndroidViewModel(application) {

    val shoppingItems = shoppingItemRepository.getAllShoppingItems()
    val activeShoppingItems = shoppingItemRepository.getActiveShoppingItems()

    private val _uiState = MutableStateFlow<ShoppingItemUiState>(ShoppingItemUiState())
    val uiState: StateFlow<ShoppingItemUiState> = _uiState.asStateFlow()
    
    // 操作状态
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    fun loadShoppingItem(itemId: String) {
        viewModelScope.launch {
            val item = shoppingItemRepository.getShoppingItemByUuid(itemId)
            _uiState.value = _uiState.value.copy(selectedItem = item)
        }
    }

    fun insertShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Saving
                shoppingItemRepository.insertShoppingItem(item)
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_add_shopping_success)
                )
                delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.operation_add_shopping_failed,
                        e.message ?: ""
                    )
                )
                delay(2000)
                _operationState.value = OperationState.Idle
            }
        }
    }

    fun updateShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Saving
                shoppingItemRepository.updateShoppingItem(item)
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_update_success)
                )
                delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.operation_update_failed,
                        e.message ?: ""
                    )
                )
                delay(2000)
                _operationState.value = OperationState.Idle
            }
        }
    }

    fun deleteShoppingItem(item: ShoppingItem, recordPurchaseEvent: Boolean = true) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Deleting
                shoppingItemRepository.deleteShoppingItem(item, recordPurchaseEvent)
                _operationState.value = OperationState.Success(
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_delete_success)
                )
                delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.operation_delete_failed,
                        e.message ?: ""
                    )
                )
                delay(2000)
                _operationState.value = OperationState.Idle
            }
        }
    }

    fun toggleComplete(item: ShoppingItem) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Saving
                shoppingItemRepository.updateShoppingItem(
                    item.copy(
                        isCompleted = !item.isCompleted,
                        completedAt = if (!item.isCompleted) Date() else null
                    )
                )
                val message = if (!item.isCompleted) {
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_mark_complete)
                } else {
                    getApplication<Application>().getString(com.example.itemremindertool.R.string.operation_mark_incomplete)
                }
                _operationState.value = OperationState.Success(message)
                delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(
                    getApplication<Application>().getString(
                        com.example.itemremindertool.R.string.operation_failed,
                        e.message ?: ""
                    )
                )
                delay(2000)
                _operationState.value = OperationState.Idle
            }
        }
    }
}

data class ShoppingItemUiState(
    val selectedItem: ShoppingItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ShoppingItemViewModelFactory(
    private val application: Application,
    private val shoppingItemRepository: ShoppingItemRepository
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingItemViewModel(application, shoppingItemRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

