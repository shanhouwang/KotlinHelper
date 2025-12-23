package com.example.test.list

// Intent 表示用户动作或生命周期事件，是 MVI 的输入。
sealed interface ListIntent {
    // 页面首次进入或需要初始化时触发。
    data object Load : ListIntent
    // 主动刷新数据（按钮/下拉刷新等）。
    data object Refresh : ListIntent
    // 点击某个列表项。
    data class ItemClicked(val id: String, val label: String) : ListIntent
}

// Effect 是一次性事件，比如 Toast/Snackbar。
sealed interface ListEffect {
    data class ShowMessage(val message: String) : ListEffect
}

// State 是 UI 的唯一数据来源。
data class ListState(
    val isLoading: Boolean = false,
    val items: List<ListItem> = emptyList(),
    val errorMessage: String? = null
)
