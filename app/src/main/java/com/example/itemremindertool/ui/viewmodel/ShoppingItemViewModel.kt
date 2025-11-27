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
import java.util.Date

class ShoppingItemViewModel(
    private val shoppingItemRepository: ShoppingItemRepository
) : ViewModel() {

    val shoppingItems = shoppingItemRepository.getAllShoppingItems()
    val activeShoppingItems = shoppingItemRepository.getActiveShoppingItems()

    private val _uiState = MutableStateFlow<ShoppingItemUiState>(ShoppingItemUiState())
    val uiState: StateFlow<ShoppingItemUiState> = _uiState.asStateFlow()

    fun loadShoppingItem(itemId: Long) {
        viewModelScope.launch {
            val item = shoppingItemRepository.getShoppingItemById(itemId)
            _uiState.value = _uiState.value.copy(selectedItem = item)
        }
    }

    fun insertShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingItemRepository.insertShoppingItem(item)
        }
    }

    fun updateShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingItemRepository.updateShoppingItem(item)
        }
    }

    fun deleteShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingItemRepository.deleteShoppingItem(item)
        }
    }

    fun toggleComplete(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingItemRepository.updateShoppingItem(
                item.copy(
                    isCompleted = !item.isCompleted,
                    completedAt = if (!item.isCompleted) Date() else null
                )
            )
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

