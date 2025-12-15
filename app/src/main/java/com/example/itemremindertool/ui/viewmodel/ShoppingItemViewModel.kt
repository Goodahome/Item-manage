package com.example.itemremindertool.ui.viewmodel

import androidx.lifecycle.ViewModel
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
    private val shoppingItemRepository: ShoppingItemRepository
) : ViewModel() {

    val shoppingItems = shoppingItemRepository.getAllShoppingItems()
    val activeShoppingItems = shoppingItemRepository.getActiveShoppingItems()

    private val _uiState = MutableStateFlow<ShoppingItemUiState>(ShoppingItemUiState())
    val uiState: StateFlow<ShoppingItemUiState> = _uiState.asStateFlow()
    
    // 操作状态
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    fun loadShoppingItem(itemId: Long) {
        viewModelScope.launch {
            val item = shoppingItemRepository.getShoppingItemById(itemId)
            _uiState.value = _uiState.value.copy(selectedItem = item)
        }
    }

    fun insertShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Saving
                shoppingItemRepository.insertShoppingItem(item)
                _operationState.value = OperationState.Success("已添加到购物清单")
                delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("添加失败: ${e.message}")
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
                _operationState.value = OperationState.Success("更新成功")
                delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("更新失败: ${e.message}")
                delay(2000)
                _operationState.value = OperationState.Idle
            }
        }
    }

    fun deleteShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Deleting
                shoppingItemRepository.deleteShoppingItem(item)
                _operationState.value = OperationState.Success("删除成功")
                delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("删除失败: ${e.message}")
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
                val message = if (!item.isCompleted) "已标记为完成" else "已取消完成"
                _operationState.value = OperationState.Success(message)
                delay(2000)
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("操作失败: ${e.message}")
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
    private val shoppingItemRepository: ShoppingItemRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingItemViewModel(shoppingItemRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

