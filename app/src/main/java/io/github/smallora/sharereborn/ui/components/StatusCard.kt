package io.github.smallora.sharereborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.smallora.sharereborn.data.model.WeiboPic
import io.github.smallora.sharereborn.data.model.WeiboStatus
import io.github.smallora.sharereborn.ui.util.Format

/** 微博卡片：头像/昵称/时间 + 正文 + 配图 + 视频卡 + 转发内容 + 底部操作栏 */
@Composable
fun StatusCard(
    status: WeiboStatus,
    onClick: () -> Unit,
    onCommentClick: () -> Unit,
    onRepostClick: () -> Unit,
    onLikeClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Header(status)
            Spacer(Modifier.height(6.dp))
            val text = Format.plainText(status.text)
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 12,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (status.pics.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                PicGrid(status.pics)
            }
            status.pageInfo?.let { page ->
                if (page.cover.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    VideoCard(cover = page.cover, title = page.title)
                }
            }
            status.retweeted?.let { rt ->
                Spacer(Modifier.height(8.dp))
                RetweetBlock(rt)
            }
            Spacer(Modifier.height(8.dp))
            ActionRow(status, onRepostClick, onCommentClick, onLikeClick)
        }
    }
    HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun Header(status: WeiboStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = status.user.avatar,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = status.user.name.ifEmpty { "微博用户" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (status.user.verified) {
                    Spacer(Modifier.width(4.dp))
                    Text("V", color = Color(0xFFFF8A00), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                text = Format.relativeTime(status.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PicGrid(pics: List<WeiboPic>) {
    when (pics.size) {
        1 -> AsyncImage(
            model = pics.first().large,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        else -> {
            val rows = pics.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { pic ->
                            AsyncImage(
                                model = pic.thumb.ifEmpty { pic.large },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCard(cover: String, title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Icon(
            imageVector = Icons.Outlined.PlayCircle,
            contentDescription = "视频",
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center),
        )
        if (title.isNotEmpty()) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color(0x88000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun RetweetBlock(rt: WeiboStatus) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
    ) {
        Text(
            text = "@" + rt.user.name.ifEmpty { "原微博" },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val text = Format.plainText(rt.text)
        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (rt.pics.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rt.pics.take(3).forEach { pic ->
                    AsyncImage(
                        model = pic.thumb.ifEmpty { pic.large },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    status: WeiboStatus,
    onRepost: () -> Unit,
    onComment: () -> Unit,
    onLike: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ActionItem(
            icon = { Icon(Icons.Outlined.Repeat, "转发", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            text = Format.count(status.repostsCount),
            onClick = onRepost,
        )
        ActionItem(
            icon = { Icon(Icons.Outlined.ChatBubbleOutline, "评论", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            text = Format.count(status.commentsCount),
            onClick = onComment,
        )
        ActionItem(
            icon = { Icon(Icons.Outlined.FavoriteBorder, "赞", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            text = Format.count(status.attitudesCount),
            onClick = onLike,
        )
    }
}

@Composable
private fun ActionItem(icon: @Composable () -> Unit, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        icon()
        if (text.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
