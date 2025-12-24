package com.example.test.list

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray

// 假数据源：用真实 OkHttp 请求模拟网络数据。
class FakeListRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val tag = "FakeListRepository"
    private val client = OkHttpClient()
    // 演示用的公共接口，适合学习如何把“回调式请求”变成 suspend。
    private val baseUrl = "https://jsonplaceholder.typicode.com"

    // suspend 表示这是“可挂起”的函数：它可以在内部调用挂起函数，
    // 等待时不会阻塞线程（线程会让给其它协程）。
    suspend fun fetchItems(): List<ListItem> = withContext(dispatcher) {
        val totalStart = System.currentTimeMillis()
        try {
            // supervisorScope：某个子协程失败，不会取消其他兄弟协程。
            supervisorScope {
                // 使用 async 并发请求，模拟多个接口同时执行。
                val bannersDeferred = async {
                    safeFetch("banners", emptyList<BannerItem>()) { fetchBanners() }
                }
                val articlesDeferred = async {
                    safeFetch("articles", emptyList<ArticleItem>()) { fetchArticles() }
                }
                val usersDeferred = async {
                    safeFetch("users", emptyList<UserItem>()) { fetchUsers() }
                }
                val statsDeferred = async {
                    safeFetch("stats", emptyList<StatItem>()) { fetchStats() }
                }
                val adsDeferred = async {
                    safeFetch("ads", emptyList<AdItem>()) { fetchAds() }
                }

                // await 也是挂起点：如果结果还没好，会挂起当前协程而不是阻塞线程。
                // 等待所有并发任务返回结果（总耗时≈最长的那个请求）。
                val banners = bannersDeferred.await()
                val articles = articlesDeferred.await()
                val users = usersDeferred.await()
                val stats = statsDeferred.await()
                val ads = adsDeferred.await()

                // 把不同类型的条目拼装成一个列表。
                buildList {
                    add(SectionHeaderItem("section-banners", "横幅"))
                    addAll(banners)
                    add(SectionHeaderItem("section-articles", "文章"))
                    addAll(articles)
                    add(SectionHeaderItem("section-users", "用户"))
                    addAll(users)
                    add(SectionHeaderItem("section-stats", "统计"))
                    addAll(stats)
                    add(SectionHeaderItem("section-ads", "赞助"))
                    addAll(ads)
                    add(FooterItem("footer", "列表结束"))
                }
            }
        } finally {
            log("fetchItems 总耗时=${System.currentTimeMillis() - totalStart}ms")
        }
    }

    // 这些函数是 suspend，因为它们内部调用了网络请求（挂起函数）。
    private suspend fun fetchBanners(): List<BannerItem> {
        val json = httpGet("$baseUrl/posts?_limit=2")
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val body = item.getString("body").replace('\n', ' ')
            BannerItem(
                id = "banner-${item.getInt("id")}",
                title = item.getString("title"),
                subtitle = body.take(40)
            )
        }
    }

    // suspend 并不等于新线程，它只是让协程在需要等待时“暂停一下”。
    private suspend fun fetchArticles(): List<ArticleItem> {
        val json = httpGet("$baseUrl/posts?_start=2&_limit=3")
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val body = item.getString("body").replace('\n', ' ')
            ArticleItem(
                id = "article-${item.getInt("id")}",
                title = item.getString("title"),
                summary = body.take(60)
            )
        }
    }

    private suspend fun fetchUsers(): List<UserItem> {
        val json = httpGet("$baseUrl/users")
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            UserItem(
                id = "user-${item.getInt("id")}",
                name = item.getString("name"),
                role = item.optString("username")
            )
        }
    }

    private suspend fun fetchStats(): List<StatItem> {
        val json = httpGet("$baseUrl/todos?_limit=3")
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            StatItem(
                id = "stat-${item.getInt("id")}",
                label = item.getString("title").take(8),
                value = if (item.getBoolean("completed")) "已完成" else "进行中"
            )
        }
    }

    private suspend fun fetchAds(): List<AdItem> {
        val json = httpGet("$baseUrl/comments?_limit=2")
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val name = item.getString("name")
            val email = item.getString("email")
            AdItem(
                id = "ad-${item.getInt("id")}",
                text = "$name · $email"
            )
        }
    }

    private fun log(message: String) {
        Log.d(tag, message)
    }

    // 保护性包装：单个请求失败时返回兜底数据，不影响其他请求。
    private suspend fun <T> safeFetch(
        name: String,
        fallback: T,
        block: suspend () -> T
    ): T {
        return runCatching { block() }
            .getOrElse { error ->
                log("$name 请求失败：${error.message}")
                fallback
            }
    }

    // 把 OkHttp 的回调风格改成挂起函数：等待网络结果时不会阻塞线程。
    private suspend fun httpGet(url: String): String =
        suspendCancellableCoroutine { cont ->
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)

            // 协程取消时同步取消网络请求。
            cont.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) {
                        cont.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        if (!resp.isSuccessful) {
                            val code = resp.code
                            if (cont.isActive) {
                                cont.resumeWithException(IOException("HTTP $code"))
                            }
                            return
                        }
                        val body = resp.body?.string().orEmpty()
                        if (cont.isActive) {
                            cont.resume(body)
                        }
                    }
                }
            })
        }
}
