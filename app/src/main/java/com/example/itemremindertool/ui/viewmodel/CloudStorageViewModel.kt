package com.example.itemremindertool.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class CloudStorageViewModel : ViewModel() {
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()
    
    fun setState(state: OperationState) {
        _operationState.value = state
    }
    
    fun showSuccess(message: String) {
        // 立即更新状态，确保UI能立即看到
        _operationState.value = OperationState.Success(message)
        viewModelScope.launch {
            delay(2000)
            _operationState.value = OperationState.Idle
        }
    }
    
    fun showError(message: String) {
        // 立即更新状态，确保UI能立即看到
        _operationState.value = OperationState.Error(message)
        viewModelScope.launch {
            delay(2000)
            _operationState.value = OperationState.Idle
        }
    }
    
    fun showSaving() {
        _operationState.value = OperationState.Saving
    }
    
    fun reset() {
        _operationState.value = OperationState.Idle
    }
}

