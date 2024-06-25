package me.kuku.api.logic

import com.microsoft.playwright.Route
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.kuku.api.utils.PlaywrightBrowser
import me.kuku.api.utils.PlaywrightUtils
import me.kuku.api.utils.addCookie
import me.kuku.utils.toJsonNode

object NodeSeekLogic {

    private val cache = mutableMapOf<String, Any>()

    private val mutes = Mutex()

    @Suppress("DuplicatedCode")
    suspend fun sign(cookie: String, random: Boolean = false) {
        mutes.withLock {
            val page = PlaywrightUtils.browser(PlaywrightBrowser.Firefox).newPage()
            try {
                page.context().addCookie(cookie, ".nodeseek.com")
                page.navigate("https://www.nodeseek.com")
                page.waitForSelector("#nsk-head")
                val url = "https://www.nodeseek.com/api/attendance?random=$random"
                page.route(url) {
                    val headers = it.request().headers()
                    headers["content-type"] = "text/plain"
                    headers["referer"] = "https://www.nodeseek.com/board"
                    it.resume(Route.ResumeOptions().setMethod("POST").setPostData("").setHeaders(headers))
                }
                val ss = CompletableDeferred<String>()
                page.onRequestFinished {
                    if (it.url() == url) {
                        val text = it.response().text()
                        ss.complete(text)
                    }
                }
                page.navigate(url)
                val jsonNode = ss.await().toJsonNode()
                cache.put(cookie, jsonNode)
            } finally {
                page.context().browser().close()
            }
        }
    }

    fun querySign(cookie: String): Any {
        return cache.remove(cookie) ?: error("没有查询到NodeSeek签到结果")
    }

}