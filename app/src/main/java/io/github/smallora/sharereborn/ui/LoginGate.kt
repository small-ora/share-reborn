package io.github.smallora.sharereborn.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * 游客态功能门禁：需要登录的操作统一弹这个，而不是报错。
 * 登录能力在 P2 接入，这里先明确告知用户。
 */
@Composable
fun LoginGateDialog(feature: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$feature 需要登录") },
        text = {
            Text(
                "当前是游客模式，可以浏览微博、看评论。\n" +
                    "发表评论、转发、点赞、发布等功能需要登录微博账号。\n\n" +
                    "登录功能会在后续版本接入。",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
    )
}
