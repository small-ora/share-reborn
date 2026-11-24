package io.github.smallora.sharereborn.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.smallora.sharereborn.data.model.WeiboStatus
import io.github.smallora.sharereborn.ui.components.StatusCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeed(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenDetail: (WeiboStatus) -> Unit,
    onActionGate: (String) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.loading && state.items.isEmpty() -> CenteredLoading()
            state.error != null && state.items.isEmpty() -> CenteredError(state.error, onRetry)
            state.items.isEmpty() && state.loadedOnce -> CenteredEmpty(onRetry)
            else -> FeedList(state, onLoadMore, onOpenDetail, onActionGate)
        }
    }
}

@Composable
private fun FeedList(
    state: HomeUiState,
    onLoadMore: () -> Unit,
    onOpenDetail: (WeiboStatus) -> Unit,
    onActionGate: (String) -> Unit,
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // 滚到接近底部时自动加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.items.size - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(state.items, key = { _, item -> item.id + item.mblogid }) { _, status ->
            StatusCard(
                status = status,
                onClick = { onOpenDetail(status) },
                onCommentClick = { onActionGate("发表评论") },
                onRepostClick = { onActionGate("转发") },
                onLikeClick = { onActionGate("点赞") },
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.loadingMore -> CircularProgressIndicator(Modifier.padding(4.dp))
                    !state.hasMore -> Text(
                        "没有更多了",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("加载失败", style = MaterialTheme.typography.titleMedium)
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        )
        Button(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun CenteredEmpty(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("暂时没有内容", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("重新加载") }
    }
}
