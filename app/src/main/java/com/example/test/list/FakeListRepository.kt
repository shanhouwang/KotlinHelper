package com.example.test.list

import android.util.Log
import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// 假数据源：用延迟来模拟网络请求。
class FakeListRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val tag = "FakeListRepository"

    suspend fun fetchItems(): List<ListItem> = withContext(dispatcher) {
        val totalStart = System.currentTimeMillis()
        try {
            coroutineScope {
                // 使用 async 并发请求，模拟多个接口同时执行。
                val bannersDeferred = async { fetchBanners() }
                val articlesDeferred = async { fetchArticles() }
                val usersDeferred = async { fetchUsers() }
                val statsDeferred = async { fetchStats() }
                val adsDeferred = async { fetchAds() }

                // 随机失败，用来演示错误处理。
                maybeFail()

                // 等待所有并发任务返回结果（总耗时≈最长的那个请求）。
                val banners = bannersDeferred.await()
                val articles = articlesDeferred.await()
                val users = usersDeferred.await()
                val stats = statsDeferred.await()
                val ads = adsDeferred.await()

                log("fetchItems 总耗时=${System.currentTimeMillis() - totalStart}ms")

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
        } finally {}
    }

    private suspend fun fetchBanners(): List<BannerItem> {
        // 模拟接口耗时。
        randomDelay(300, 700)
        return listOf(
            BannerItem("banner-1", "Kotlin MVI", "状态/意图/效果 统一管理"),
            BannerItem("banner-2", "Compose 列表", "LazyColumn 展示多种条目类型")
        )
    }

    private suspend fun fetchArticles(): List<ArticleItem> {
        // 模拟接口耗时。
        randomDelay(500, 900)
        return listOf(
            ArticleItem("article-1", "协程入门", "launch、async 与结构化并发"),
            ArticleItem("article-2", "StateFlow 基础", "为何适合 Compose"),
            ArticleItem("article-3", "错误处理", "展示重试与错误态")
        )
    }

    private suspend fun fetchUsers(): List<UserItem> {
        // 模拟接口耗时。
        randomDelay(200, 600)
        return listOf(
            UserItem("user-1", "小安", "Android 开发"),
            UserItem("user-2", "小乐", "后端开发"),
            UserItem("user-3", "小米", "设计师")
        )
    }

    private suspend fun fetchStats(): List<StatItem> {
        // 模拟接口耗时。
        randomDelay(300, 700)
        return listOf(
            StatItem("stat-1", "进行中", "128"),
            StatItem("stat-2", "待处理", "24"),
            StatItem("stat-3", "已完成", "512")
        )
    }

    private suspend fun fetchAds(): List<AdItem> {
        // 模拟接口耗时。
        randomDelay(250, 650)
        return listOf(
            AdItem("ad-1", "试试我们的协程课程！"),
            AdItem("ad-2", "用 Compose 更快构建界面")
        )
    }

    private suspend fun randomDelay(minMs: Long, maxMs: Long) {
        // 随机延迟让数据更像网络。
        delay(Random.nextLong(minMs, maxMs))
    }

    private fun maybeFail() {
        // 20% 概率失败，用来看到错误态与重试逻辑。
        if (Random.nextInt(0, 10) < 2) {
            throw IOException("模拟网络错误")
        }
    }

    private fun log(message: String) {
        Log.d(tag, message)
    }
}
