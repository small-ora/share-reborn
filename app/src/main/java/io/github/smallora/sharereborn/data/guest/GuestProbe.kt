package io.github.smallora.sharereborn.data.guest

import android.os.Build
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 游客（未登录）模式可行性自检。
 *
 * 目的：在不提供任何微博账号的前提下，验证「访客票据 + weibo.com 公开 AJAX 接口」
 * 这条链路在本机/本网络下是否可用，为 P1 的游客浏览链路提供依据。
 *
 * 结论会展示在 App 界面上，便于在真机验证（无需 adb）。
 */
object GuestProbe {

    data class Step(
        val title: String,
        val ok: Boolean,
        val detail: String,
    )

    private fun ua(): String =
        "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private var sub: String = ""
    private var subp: String = ""

    private fun cookieHeader(): String = buildString {
        if (sub.isNotEmpty()) append("SUB=$sub; ")
        if (subp.isNotEmpty()) append("SUBP=$subp")
    }.trim().removeSuffix(";")

    suspend fun runAll(onStep: (Step) -> Unit) {
        onStep(
            Step(
                "运行环境",
                true,
                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.SUPPORTED_ABIS.firstOrNull() ?: "?"}",
            )
        )

        // 1. 访客票据（genvisitor + incarnate）
        val ticket = runCatching { fetchVisitorTicket() }
        ticket.fold(
            onSuccess = {
                onStep(Step("① 访客票据", true, "已获取 SUB(${sub.take(16)}…) / SUBP(${subp.take(12)}…)"))
            },
            onFailure = {
                onStep(Step("① 访客票据", false, it.message ?: it.toString()))
                return
            },
        )

        // 2. 推荐流
        var firstId = ""
        var firstMblog = ""
        val timeline = runCatching { fetchHotTimeline() }
        timeline.fold(
            onSuccess = { (count, id, mblog, preview) ->
                firstId = id
                firstMblog = mblog
                onStep(Step("② 推荐流 hottimeline", count > 0, "拿到 $count 条；首条预览：${preview.take(40)}"))
            },
            onFailure = { onStep(Step("② 推荐流 hottimeline", false, it.message ?: it.toString())) },
        )
        if (firstMblog.isEmpty()) return

        // 3. 微博详情
        val detail = runCatching { fetchStatusDetail(firstMblog) }
        detail.fold(
            onSuccess = { (text, comments) ->
                onStep(Step("③ 微博详情 statuses/show", text.isNotEmpty(), "正文 ${text.length} 字，评论数 $comments"))
            },
            onFailure = { onStep(Step("③ 微博详情 statuses/show", false, it.message ?: it.toString())) },
        )

        // 4. 评论列表
        val comments = runCatching { fetchComments(firstId) }
        comments.fold(
            onSuccess = { (count, first) ->
                onStep(Step("④ 评论 buildComments", count > 0, "拿到 $count 条；首条：${first.take(40)}"))
            },
            onFailure = { onStep(Step("④ 评论 buildComments", false, it.message ?: it.toString())) },
        )

        // 5. 用户资料
        val uid = runCatching { fetchFirstUid() }.getOrNull().orEmpty()
        if (uid.isNotEmpty()) {
            val profile = runCatching { fetchProfile(uid) }
            profile.fold(
                onSuccess = { name ->
                    onStep(Step("⑤ 用户资料 profile/info", name.isNotEmpty(), "昵称：$name"))
                },
                onFailure = { onStep(Step("⑤ 用户资料 profile/info", false, it.message ?: it.toString())) },
            )
        }
    }

    // ---------- 具体请求 ----------

    private fun fetchVisitorTicket() {
        val fp = """{"os":"1","browser":"Chrome88,0,4324,122","fonts":"undefined","screenInfo":"1920*1080*24","plugins":""}"""
        val body = "cb=gen_callback&fp=" + java.net.URLEncoder.encode(fp, "UTF-8")
        val gen = post(
            "https://passport.weibo.com/visitor/genvisitor",
            body,
            mapOf("Content-Type" to "application/x-www-form-urlencoded"),
        )
        val tid = JSONObject(gen.substringAfter("(", "").substringBeforeLast(")")).getJSONObject("data").getString("tid")
        val incarnate = get(
            "https://passport.weibo.com/visitor/visitor?a=incarnate&t=$tid&w=2&c=095&gc=&cb=cross_domain&from=weibo&_rand=${System.currentTimeMillis()}"
        )
        val data = JSONObject(incarnate.substringAfter("(", "").substringBeforeLast(")")).getJSONObject("data")
        sub = data.optString("sub")
        subp = data.optString("subp")
        check(sub.isNotEmpty()) { "访客票据为空" }
    }

    private fun fetchHotTimeline(): Quad {
        val json = get(
            "https://weibo.com/ajax/feed/hottimeline?since_id=0&refresh=0&group_id=102803&containerid=102803&extparam=discover%7Cnew_feed"
        )
        val obj = JSONObject(json)
        check(obj.optInt("ok", -1) == 1) { "ok=${obj.optInt("ok")}" }
        val arr = obj.optJSONArray("statuses") ?: return Quad(0, "", "", "")
        if (arr.length() == 0) return Quad(0, "", "", "")
        val s = arr.getJSONObject(0)
        val text = s.optString("text_raw").ifEmpty { s.optString("text") }
        return Quad(arr.length(), s.optString("idstr"), s.optString("mblogid"), text)
    }

    private fun fetchStatusDetail(mblogid: String): Pair<String, Int> {
        val obj = JSONObject(get("https://weibo.com/ajax/statuses/show?id=$mblogid"))
        val text = obj.optString("text_raw").ifEmpty { obj.optString("text") }
        return text to obj.optInt("comments_count", -1)
    }

    private fun fetchComments(id: String): Pair<Int, String> {
        val obj = JSONObject(
            get("https://weibo.com/ajax/statuses/buildComments?is_reload=1&id=$id&is_show_bulletin=2&count=20")
        )
        check(obj.optInt("ok", -1) == 1) { "ok=${obj.optInt("ok")}" }
        val arr = obj.optJSONArray("data") ?: return 0 to ""
        val first = if (arr.length() > 0) arr.getJSONObject(0).optString("text_raw") else ""
        return arr.length() to first
    }

    private fun fetchFirstUid(): String {
        val json = get(
            "https://weibo.com/ajax/feed/hottimeline?since_id=0&refresh=0&group_id=102803&containerid=102803&extparam=discover%7Cnew_feed"
        )
        val arr = JSONObject(json).optJSONArray("statuses") ?: return ""
        if (arr.length() == 0) return ""
        return arr.getJSONObject(0).optJSONObject("user")?.optString("idstr").orEmpty()
    }

    private fun fetchProfile(uid: String): String {
        val obj = JSONObject(get("https://weibo.com/ajax/profile/info?uid=$uid"))
        check(obj.optInt("ok", -1) == 1) { "ok=${obj.optInt("ok")}" }
        return obj.optJSONObject("data")?.optJSONObject("user")?.optString("screen_name").orEmpty()
    }

    // ---------- HTTP ----------

    private fun get(url: String): String = request(url, null)

    private fun post(url: String, body: String, headers: Map<String, String>): String =
        request(url, body, headers)

    private fun request(url: String, body: String?, extraHeaders: Map<String, String> = emptyMap()): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            instanceFollowRedirects = true
            requestMethod = if (body == null) "GET" else "POST"
            setRequestProperty("User-Agent", ua())
            setRequestProperty("Referer", "https://weibo.com/")
            setRequestProperty("Accept", "application/json, text/plain, */*")
            val ck = cookieHeader()
            if (ck.isNotEmpty()) setRequestProperty("Cookie", ck)
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
        }
        try {
            if (body != null) conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            check(code in 200..299) { "HTTP $code: ${text.take(120)}" }
            return text
        } finally {
            conn.disconnect()
        }
    }

    private data class Quad(val count: Int, val id: String, val mblogid: String, val preview: String)
}
