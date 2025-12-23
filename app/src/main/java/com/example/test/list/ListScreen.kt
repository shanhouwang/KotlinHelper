package com.example.test.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ListRoute(
    viewModel: ListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 首次进入页面时触发加载，只执行一次。
    LaunchedEffect(Unit) {
        viewModel.dispatch(ListIntent.Load)
    }

    // 收集一次性事件（例如 Snackbar）。
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ListEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ListScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::dispatch
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListScreen(
    state: ListState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ListIntent) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "MVI 列表") },
                actions = {
                    TextButton(onClick = { onIntent(ListIntent.Refresh) }) {
                        Text(text = "刷新")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 根据状态决定显示哪种页面。
            when {
                state.isLoading && state.items.isEmpty() -> {
                    LoadingView()
                }
                state.errorMessage != null && state.items.isEmpty() -> {
                    ErrorView(
                        message = state.errorMessage,
                        onRetry = { onIntent(ListIntent.Refresh) }
                    )
                }
                state.items.isEmpty() -> {
                    EmptyView(onRetry = { onIntent(ListIntent.Refresh) })
                }
                else -> {
                    ListContent(
                        items = state.items,
                        onIntent = onIntent
                    )

                    // 刷新中时显示顶部进度条。
                    if (state.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 发生错误时的占位页。
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "加载失败", style = MaterialTheme.typography.titleMedium)
        Text(text = message, textAlign = TextAlign.Center)
        Button(onClick = onRetry) {
            Text(text = "重试")
        }
    }
}

@Composable
private fun EmptyView(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 空列表占位页。
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "暂无数据", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onRetry) {
            Text(text = "加载")
        }
    }
}

@Composable
private fun ListContent(
    items: List<ListItem>,
    onIntent: (ListIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    // LazyColumn 用于渲染多个 item 类型。
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            // 根据不同类型选择不同的 UI。
            when (item) {
                is SectionHeaderItem -> SectionHeaderRow(item)
                is BannerItem -> BannerCard(
                    item = item,
                    onClick = {
                        onIntent(ListIntent.ItemClicked(item.id, item.title))
                    }
                )
                is ArticleItem -> ArticleRow(
                    item = item,
                    onClick = {
                        onIntent(ListIntent.ItemClicked(item.id, item.title))
                    }
                )
                is UserItem -> UserRow(
                    item = item,
                    onClick = {
                        onIntent(ListIntent.ItemClicked(item.id, item.name))
                    }
                )
                is StatItem -> StatRow(item)
                is AdItem -> AdRow(item)
                is FooterItem -> FooterRow(item)
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(item: SectionHeaderItem) {
    Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun BannerCard(
    item: BannerItem,
    onClick: () -> Unit
) {
    // Banner 使用 Card 展示。
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = item.subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ArticleRow(
    item: ArticleItem,
    onClick: () -> Unit
) {
    // 文章条目。
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun UserRow(
    item: UserItem,
    onClick: () -> Unit
) {
    // 用户条目，左右排版。
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = item.name, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = item.role, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatRow(item: StatItem) {
    // 统计数据条目。
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = item.label, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = item.value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AdRow(item: AdItem) {
    // 广告/推广条目。
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = item.text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FooterRow(item: FooterItem) {
    // 列表底部提示。
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = item.hint, style = MaterialTheme.typography.bodySmall)
    }
}
