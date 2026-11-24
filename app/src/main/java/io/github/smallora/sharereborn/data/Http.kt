package io.github.smallora.sharereborn.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 极简 HTTP 客户端（无第三方依赖）。
 * 统一注入移动端 UA / Referer / Cookie，并给出可读的错误信息。
 */
object Http {

    fun ua(): String =
        "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    suspend fun get(url: String, cookie: String = ""): String = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", ua())
            setRequestProperty("Referer", "https://weibo.com/")
            setRequestProperty("Accept", "application/json, text/plain, */*")
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9")
            if (cookie.isNotEmpty()) setRequestProperty("Cookie", cookie)
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) throw HttpException(code, body.take(160))
            body
        } finally {
            conn.disconnect()
        }
    }

    suspend fun postForm(url: String, form: Map<String, String>, cookie: String = ""): String =
        withContext(Dispatchers.IO) {
            val body = form.entries.joinToString("&") { (k, v) ->
                URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
            }
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("User-Agent", ua())
                setRequestProperty("Referer", "https://passport.weibo.com/visitor/visitor")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                if (cookie.isNotEmpty()) setRequestProperty("Cookie", cookie)
            }
            try {
                conn.outputStream.use { it.write(body.toByteArray()) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                if (code !in 200..299) throw HttpException(code, text.take(160))
                text
            } finally {
                conn.disconnect()
            }
        }

    /** 从 JSONP 包裹里取出 JSON 对象文本：gen_callback({...}); */
    fun unwrapJsonp(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
    }
}

class HttpException(val code: Int, val body: String) :
    Exception("HTTP $code: $body")

/**
 * 游客会话：通过访客票据接口拿 SUB / SUBP，缓存到 SharedPreferences，
 * 失效（接口返回登录要求）时自动刷新一次。
 */
object GuestSession {

    private const val PREF = "guest_session"
    private const val KEY_SUB = "sub"
    private const val KEY_SUBP = "subp"

    private val mutex = Mutex()
    private var sub: String = ""
    private var subp: String = ""

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun cookies(): String = buildString {
        if (sub.isNotEmpty()) append("SUB=$sub; ")
        if (subp.isNotEmpty()) append("SUBP=$subp")
    }.trim().removeSuffix(";").trim()

    fun hasSession(): Boolean = sub.isNotEmpty()

    /** 优先用缓存；没有或强制刷新时重新获取票据。 */
    suspend fun ensure(context: Context, force: Boolean = false): String = mutex.withLock {
        if (!force && sub.isNotEmpty()) return cookies()

        val p = prefs(context)
        if (!force) {
            val s = p.getString(KEY_SUB, null)
            val sp = p.getString(KEY_SUBP, null)
            if (!s.isNullOrEmpty()) {
                sub = s
                subp = sp.orEmpty()
                return cookies()
            }
        }
        fetchTicket()
        p.edit().putString(KEY_SUB, sub).putString(KEY_SUBP, subp).apply()
        cookies()
    }

    private suspend fun fetchTicket() {
        val fp = """{"os":"1","browser":"Chrome88,0,4324,122","fonts":"undefined","screenInfo":"1920*1080*24","plugins":""}"""
        val gen = Http.postForm(
            "https://passport.weibo.com/visitor/genvisitor",
            mapOf("cb" to "gen_callback", "fp" to fp),
        )
        val tid = JSONObject(Http.unwrapJsonp(gen))
            .getJSONObject("data")
            .getString("tid")

        val incarnate = Http.get(
            "https://passport.weibo.com/visitor/visitor?a=incarnate&t=$tid&w=2&c=095&gc=" +
                "&cb=cross_domain&from=weibo&_rand=${System.currentTimeMillis()}"
        )
        val data = JSONObject(Http.unwrapJsonp(incarnate)).getJSONObject("data")
        sub = data.optString("sub")
        subp = data.optString("subp")
        check(sub.isNotEmpty()) { "访客票据为空" }
    }
}
