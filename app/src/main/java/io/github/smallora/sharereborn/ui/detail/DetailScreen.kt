package io.github.smallora.sharereborn.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.smallora.sharereborn.data.model.WeiboComment
import io.github.smallora.sharereborn.data.model.WeiboStatus
import io.github.smallora.sharereborn.ui.components.StatusCard
import io.github.smallora.sharereborn.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onActionGate: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("微博正文") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading && state.status == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.status == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "加载失败", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                else -> DetailContent(state, onLoadMore, onActionGate)
            }
        }
    }
}

@Composable
private fun DetailContent(
    state: DetailUiState,
    onLoadMore: () -> Unit,
    onActionGate: (String) -> Unit,
) {
    val status = state.status ?: return
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            StatusCard(
                status = status,
                onClick = {},
                onCommentClick = { onActionGate("发表评论") },
                onRepostClick = { onActionGate("转发") },
                onLikeClick = { onActionGate("点赞") },
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "评论",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    Format.count(status.commentsCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (state.comments.isEmpty() && state.loading) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }

        items(state.comments, key = { it.id }) { comment ->
            CommentRow(comment)
        }

        item {
            Box(
                Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.loadingMore -> CircularProgressIndicator()
                    !state.hasMore && state.comments.isNotEmpty() -> Text(
                        "没有更多评论了",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.hasMore -> Text(
                        "上滑加载更多",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: WeiboComment) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = comment.user.avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    comment.user.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (comment.likeCount > 0) {
                Text(
                    Format.count(comment.likeCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            Format.plainText(comment.text),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 38.dp),
        )
        if (comment.replyText.isNotEmpty()) {
            Text(
                text = "回复 @${comment.replyUser}：" + Format.plainText(comment.replyText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 38.dp, top = 2.dp),
            )
        }
        Text(
            Format.relativeTime(comment.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 38.dp, top = 4.dp),
        )
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 50.dp),
    )
}

@Composable
private fun EmptyBox() {
    Column(verticalArrangement = Arrangement.Center) {}
}
