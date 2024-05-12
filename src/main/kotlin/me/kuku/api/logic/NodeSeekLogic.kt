package me.kuku.api.logic

import com.microsoft.playwright.Page
import com.microsoft.playwright.PlaywrightException
import com.microsoft.playwright.Route
import com.microsoft.playwright.options.Cookie
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.kuku.api.utils.PlaywrightBrowser
import me.kuku.api.utils.PlaywrightUtils
import me.kuku.api.utils.addCookie
import me.kuku.utils.toJsonNode
import java.util.concurrent.TimeUnit

object NodeSeekLogic {

    private val cache = mutableMapOf<String, Any>()

    private val mutes = Mutex()

    suspend fun login(username: String, password: String, token: String? = null): String {
        mutes.withLock {
            val page = PlaywrightUtils.browser(PlaywrightBrowser.Firefox).newPage()
            page.context().addCookies(listOf(Cookie("session", "dcac85f02024655cfc6b347371bc22c1").also {
                it.domain = ".nodeseek.com"
                it.path = "/"
            }))
            try {
                page.navigate("https://www.nodeseek.com/signIn.html")
                page.waitForSelector("#stacked-email")
                if (token != null) {
                    // v2
                    return newLogin(page, username, password, token)
                } else {
                    // v3
                    page.locator("#stacked-email").fill(username)
                    page.locator("#stacked-password").fill(password)
                    page.click("#login-panel > form > fieldset > div.login-btn-group > button")
                    TimeUnit.SECONDS.sleep(2)
                    try {
                        val text = page.evaluate("document.querySelector(\".msc-title\").innerText").toString()
                        error(text)
                    } catch (e: PlaywrightException) {
                        val cookies = page.context().cookies()
                        for (cookie in cookies) {
                            if (cookie.name == "session" && cookie.value != "dcac85f02024655cfc6b347371bc22c1") {
                                return "${cookie.name}=${cookie.value}; "
                            }
                        }
                        error("登录失败，通过recaptchaV3失败")
                    }
                }
            } finally {
                page.context().browser().close()
            }
        }
    }

    @Suppress("DuplicatedCode")
    private suspend fun newLogin(page: Page, username: String, password: String, token: String): String {
        val url = "https://www.nodeseek.com/api/account/signIn"
        page.route(url) {
            val headers = it.request().headers()
            headers["content-type"] = "application/json"
            it.resume(
                Route.ResumeOptions().setMethod("POST").setPostData("""
                        {"username":"$username","password":"$password","version":"v2","token":"$token"}
                    """.trimIndent()).setHeaders(headers)
            )
        }
        val ss = CompletableDeferred<String>()
        page.onRequestFinished {
            if (it.url() == url) {
                val text = it.response().text()
                ss.complete(text)
            }
        }
        page.navigate(url)
        val jsonNode =  ss.await().toJsonNode()
        if (!jsonNode["success"].asBoolean()) error(jsonNode["message"].asText())
        val sb = StringBuilder()
        page.context().cookies().filter { it.name == "session" }.forEach { sb.append("${it.name}=${it.value}; ") }
        return sb.toString()
    }

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