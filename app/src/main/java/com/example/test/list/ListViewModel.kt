package com.example.test.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ViewModel 负责处理 Intent，并更新 State/Effect。
class ListViewModel : ViewModel() {
    private val repository = FakeListRepository()

    // StateFlow: UI 持续观察的状态流。
    private val _state = MutableStateFlow(ListState())
    val state: StateFlow<ListState> = _state.asStateFlow()

    // SharedFlow: 一次性事件（例如 Snackbar）。
    private val _effect = MutableSharedFlow<ListEffect>()
    val effect: SharedFlow<ListEffect> = _effect.asSharedFlow()

    // UI 把 Intent 送进来，ViewModel 统一处理。
    fun dispatch(intent: ListIntent) {
        when (intent) {
            ListIntent.Load -> {
                // 首次进入时只加载一次，避免重复请求。
                if (_state.value.items.isEmpty()) {
                    loadItems()
                }
            }
            ListIntent.Refresh -> loadItems()
            is ListIntent.ItemClicked -> {
                emitMessage("点击了：${intent.label}")
            }
        }
    }

    private fun loadItems() {
        viewModelScope.launch {
            // 先更新为加载中状态。
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val hadItems = _state.value.items.isNotEmpty()
            try {
                // 调用仓库模拟网络请求。
                val items = repository.fetchItems()
                _state.update { it.copy(isLoading = false, items = items, errorMessage = null) }
            } catch (e: Exception) {
                val message = e.message ?: "未知错误"
                // 首次加载失败显示错误页；刷新失败只弹提示。
                _state.update {
                    it.copy(isLoading = false, errorMessage = if (hadItems) null else message)
                }
                if (hadItems) {
                    _effect.emit(ListEffect.ShowMessage("刷新失败：$message"))
                }
            }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _effect.emit(ListEffect.ShowMessage(message))
        }
    }
}
