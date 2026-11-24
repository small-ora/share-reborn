package io.github.smallora.sharereborn.ui.util

import android.text.Html
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {

    private val dateFmt = SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US)

    /** 微博正文可能是 HTML（含 <a>/<br>/<img alt="[表情]">），转成纯文本。 */
    fun plainText(raw: String): String {
        if (raw.isEmpty()) return ""
        var s = raw.replace(Regex("<img[^>]*?alt=\"([^\"]*)\"[^>]*?>"), "$1")
        s = s.replace(Regex("<br\\s*/?>"), "\n")
        s = Html.fromHtml(s, Html.FROM_HTML_MODE_COMPACT).toString()
        return s.replace('\u00a0', ' ').trim()
    }

    /** 微博时间 "Tue Nov 24 08:13:48 +0800 2026" → 相对时间 */
    fun relativeTime(createdAt: String): String {
        val date = runCatching { dateFmt.parse(createdAt) }.getOrNull() ?: return createdAt
        val diff = (System.currentTimeMillis() - date.time) / 1000
        return when {
            diff < 60 -> "刚刚"
            diff < 3600 -> "${diff / 60} 分钟前"
            diff < 86_400 -> "${diff / 3600} 小时前"
            diff < 86_400 * 30L -> "${diff / 86_400} 天前"
            else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date.time))
        }
    }

    /** 计数：1234 → 1.2万 */
    fun count(n: Int): String = when {
        n <= 0 -> ""
        n < 10_000 -> n.toString()
        else -> String.format(Locale.US, "%.1f万", n / 10_000f)
    }
}
