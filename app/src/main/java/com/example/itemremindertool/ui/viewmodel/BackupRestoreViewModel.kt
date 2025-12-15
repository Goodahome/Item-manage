package com.example.itemremindertool.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class BackupRestoreViewModel : ViewModel() {
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()
    
    fun setState(state: OperationState) {
        _operationState.value = state
    }
    
    fun showSuccess(message: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Success(message)
            delay(2000)
            _operationState.value = OperationState.Idle
        }
    }
    
    fun showError(message: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Error(message)
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

