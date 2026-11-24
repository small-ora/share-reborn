package io.github.smallora.sharereborn.data

import android.content.Context
import io.github.smallora.sharereborn.data.model.Page
import io.github.smallora.sharereborn.data.model.PageInfo
import io.github.smallora.sharereborn.data.model.WeiboComment
import io.github.smallora.sharereborn.data.model.WeiboPic
import io.github.smallora.sharereborn.data.model.WeiboStatus
import io.github.smallora.sharereborn.data.model.WeiboUser
import org.json.JSONArray
import org.json.JSONObject

/**
 * 微博网页端公开接口封装（游客态）。
 * 统一处理：游客会话注入、登录态失效自动刷新、JSON 映射。
 */
class WeiboApi(private val context: Context) {

    private val appContext = context.applicationContext

    // ---------------- 接口 ----------------

    /** 推荐流（首页）。maxId 用于上拉加载更多。 */
    suspend fun hotTimeline(maxId: String? = null): Page<WeiboStatus> {
        val sb = StringBuilder("https://weibo.com/ajax/feed/hottimeline?since_id=0&refresh=0")
            .append("&group_id=102803&containerid=102803&extparam=discover%7Cnew_feed")
        if (!maxId.isNullOrEmpty()) sb.append("&max_id=").append(maxId)
        val json = call(sb.toString())
        val root = JSONObject(json)
        checkOk(root)
        val arr = root.optJSONArray("statuses") ?: JSONArray()
        val items = (0 until arr.length()).mapNotNull { i ->
            runCatching { parseStatus(arr.getJSONObject(i)) }.getOrNull()
        }
        return Page(items, root.optString("max_id").ifEmpty { null })
    }

    /** 微博详情 */
    suspend fun statusDetail(mblogid: String): WeiboStatus {
        val json = call("https://weibo.com/ajax/statuses/show?id=$mblogid")
        return parseStatus(JSONObject(json))
    }

    /** 评论列表（只读）。 */
    suspend fun comments(id: String, maxId: String? = null, count: Int = 20): Page<WeiboComment> {
        val sb = StringBuilder("https://weibo.com/ajax/statuses/buildComments")
            .append("?is_reload=1&id=").append(id)
            .append("&is_show_bulletin=2&count=").append(count)
        if (!maxId.isNullOrEmpty()) sb.append("&max_id=").append(maxId)
        val root = JSONObject(call(sb.toString()))
        checkOk(root)
        val arr = root.optJSONArray("data") ?: JSONArray()
        val items = (0 until arr.length()).mapNotNull { i ->
            runCatching { parseComment(arr.getJSONObject(i)) }.getOrNull()
        }
        return Page(items, root.optString("max_id").ifEmpty { null })
    }

    // ---------------- 请求封装 ----------------

    private suspend fun call(url: String): String {
        var cookie = GuestSession.ensure(appContext)
        var body = Http.get(url, cookie)
        // 登录态/票据失效：刷新一次票据后重试
        if (needsLogin(body)) {
            cookie = GuestSession.ensure(appContext, force = true)
            body = Http.get(url, cookie)
        }
        return body
    }

    private fun needsLogin(body: String): Boolean =
        body.contains("\"ok\":-100") || body.contains("\"ok\": -100") || body.contains("passport.weibo.com/sso/signin")

    private fun checkOk(root: JSONObject) {
        val ok = root.optInt("ok", -1)
        if (ok != 1) throw IllegalStateException("接口返回 ok=$ok")
    }

    // ---------------- JSON 映射 ----------------

    private fun parseStatus(o: JSONObject): WeiboStatus {
        val text = o.optString("text_raw").ifEmpty { o.optString("text") }
        val pics = parsePics(o)
        val page = parsePageInfo(o.optJSONObject("page_info"))
        val retweeted = o.optJSONObject("retweeted_status")?.let { rt ->
            runCatching { parseStatus(rt) }.getOrNull()
        }
        return WeiboStatus(
            id = o.optString("idstr"),
            mblogid = o.optString("mblogid"),
            text = text,
            createdAt = o.optString("created_at"),
            user = parseUser(o.optJSONObject("user")),
            pics = pics,
            pageInfo = page,
            retweeted = retweeted,
            commentsCount = o.optInt("comments_count"),
            attitudesCount = o.optInt("attitudes_count"),
            repostsCount = o.optInt("reposts_count"),
        )
    }

    private fun parseUser(o: JSONObject?): WeiboUser {
        if (o == null) return WeiboUser()
        return WeiboUser(
            id = o.optString("idstr").ifEmpty { o.optString("id") },
            name = o.optString("screen_name"),
            avatar = o.optString("avatar_hd")
                .ifEmpty { o.optString("avatar_large") }
                .ifEmpty { o.optString("profile_image_url") },
            verified = o.optBoolean("verified"),
            verifiedType = o.optInt("verified_type", -1),
        )
    }

    private fun parsePics(o: JSONObject): List<WeiboPic> {
        val ids = o.optJSONArray("pic_ids") ?: JSONArray()
        val infos = o.optJSONObject("pic_infos")
        if (infos == null || ids.length() == 0) return emptyList()
        val out = ArrayList<WeiboPic>(ids.length())
        for (i in 0 until ids.length()) {
            val id = ids.optString(i)
            val info = infos.optJSONObject(id) ?: continue
            out.add(
                WeiboPic(
                    large = pickUrl(info, "largest", "large", "bmiddle", "thumbnail"),
                    thumb = pickUrl(info, "thumbnail", "bmiddle", "large"),
                )
            )
        }
        return out
    }

    private fun pickUrl(info: JSONObject, vararg keys: String): String {
        for (k in keys) {
            val v = info.opt(k)
            when (v) {
                is JSONObject -> {
                    val url = v.optString("url")
                    if (url.isNotEmpty() && !url.startsWith("http://ssrf")) return url
                }
                is String -> if (v.isNotEmpty()) return v
            }
        }
        return ""
    }

    private fun parsePageInfo(o: JSONObject?): PageInfo? {
        if (o == null) return null
        val cover = when (val pic = o.opt("page_pic")) {
            is JSONObject -> pic.optString("url")
            is String -> pic
            else -> ""
        }
        val objectType = o.optString("object_type")
        val type = o.optInt("type", -1)
        val title = o.optString("page_title").ifEmpty { o.optString("content1") }
        val desc = o.optString("content2")
        // 过滤掉纯链接卡片中没内容的
        if (cover.isEmpty() && title.isEmpty()) return null
        return PageInfo(objectType = objectType, type = type, cover = cover, title = title, desc = desc)
    }

    private fun parseComment(o: JSONObject): WeiboComment {
        val reply = o.optJSONObject("reply_comment")
        return WeiboComment(
            id = o.optString("idstr").ifEmpty { o.optString("id") },
            text = o.optString("text_raw").ifEmpty { o.optString("text") },
            createdAt = o.optString("created_at"),
            user = parseUser(o.optJSONObject("user")),
            likeCount = o.optInt("like_count"),
            replyText = reply?.optString("text_raw")
                ?: reply?.optString("text").orEmpty(),
            replyUser = parseUser(reply?.optJSONObject("user")).name,
        )
    }
}
