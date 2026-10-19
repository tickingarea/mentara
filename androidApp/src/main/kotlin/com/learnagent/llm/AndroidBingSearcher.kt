package com.learnagent.llm

import android.util.Base64
import com.learnagent.core.WebResult
import com.learnagent.core.WebSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 免费联网搜索：抓取必应中国的搜索结果页（cn.bing.com）。
 * 零 Key、零注册、国内直连——作为博查/Tavily 都未配置时的自动兜底。
 * 说明：网页结构可能变化导致偶发解析失败（属正常），失败时返回空列表、不影响回答。
 */
class AndroidBingSearcher : WebSearch {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun stripTags(s: String): String {
        val noTags = s.replace(Regex("<[^>]*>"), " ")
        // 程序化解码常见 HTML 实体（amp/quot/apos/lt/gt/nbsp 及 &#数字;）
        val decoded = Regex("&(#?)([a-zA-Z0-9]{1,10});").replace(noTags) { m ->
            val kind = m.groupValues[1]
            val name = m.groupValues[2]
            when {
                kind == "#" -> name.toIntOrNull()?.let { it.toChar().toString() } ?: " "
                name == "nbsp" -> " "
                name == "amp" -> "&"
                name == "quot" -> "\""
                name == "apos" -> "'"
                name == "lt" -> "<"
                name == "gt" -> ">"
                else -> " "
            }
        }
        return decoded.replace(Regex("\\s+"), " ").trim()
    }

    /** 必应新外链格式（/ck/a?...&u=a1<base64url>）还原真实地址 */
    private fun unwrap(url: String): String {
        if (!url.contains("/ck/a?")) return url
        return runCatching {
            val m = Regex("[?&]u=a1([A-Za-z0-9_\\-]+)").find(url)?.groupValues?.get(1) ?: return url
            val bytes = Base64.decode(m, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            URLDecoder.decode(String(bytes, Charsets.UTF_8), "UTF-8")
        }.getOrDefault(url)
    }

    override suspend fun search(query: String): List<WebResult> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")
        val req = Request.Builder()
            .url("https://cn.bing.com/search?q=$q&count=8&mkt=zh-CN&setlang=zh-CN")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
            )
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .get()
            .build()
        val html = runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use ""
                resp.body?.string().orEmpty()
            }
        }.getOrDefault("")
        if (html.isBlank()) return@withContext emptyList()

        val out = ArrayList<WebResult>(6)
        val chunks = html.split("<li class=\"b_algo\"")
        for (i in 1 until chunks.size) {
            val chunk = chunks[i].substringBefore("</li>")
            val hrefM = Regex("<h2[^>]*>\\s*<a[^>]+href=\"(https?://[^\"]+)\"", RegexOption.DOT_MATCHES_ALL).find(chunk) ?: continue
            val url = unwrap(hrefM.groupValues[1])
            val titleM = Regex("<h2[^>]*>\\s*<a[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL).find(chunk)
            val title = stripTags(titleM?.groupValues?.lastOrNull().orEmpty())
            val snipM = Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL).find(chunk)
            val snippet = stripTags(snipM?.groupValues?.lastOrNull().orEmpty())
            if (title.isNotBlank()) {
                out.add(WebResult(title = title, url = url, content = snippet.take(400)))
            }
            if (out.size >= 6) break
        }
        out
    }
}
