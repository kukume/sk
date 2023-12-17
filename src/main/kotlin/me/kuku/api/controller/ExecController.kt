@file:Suppress("unused")

package me.kuku.api.controller

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.kuku.api.utils.PlaywrightUtils
import me.kuku.api.utils.newPage
import me.kuku.utils.OkHttpUtils
import me.kuku.utils.toUrlEncode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ExecController(
    @Value("\${spring.ktor.port}") private val port: String
) {

    fun Routing.exec() {

        route("exec") {
            get("kuGou") {
                val phone = call.request.queryParameters.getOrFail("phone")
                val time = call.request.queryParameters.getOrFail("time")
                val js = OkHttpUtils.getStr("https://staticssl.kugou.com/common/js/min/login/kguser.v2.min.js?appid=1058")
                call.respond(ThymeleafContent("exec/kuGou", mapOf("script" to js, "phone" to phone, "time" to time)))
            }

            post("kuGou") {
                val receiveParameters = call.receiveParameters()
                val phone = receiveParameters.getOrFail("phone")
                val time = receiveParameters.getOrFail("time")
                val page = PlaywrightUtils.browser().newPage()
                withContext(Dispatchers.IO) {
                    page.navigate("http://localhost:$port/exec/kuGou?phone=$phone&time=$time")
                    val params = page.evaluate("document.getElementById(\"params\").innerText")?.toString()
                    val pk = page.evaluate("document.getElementById(\"pk\").innerText")?.toString()
                    call.respond(mapOf("params" to params, "pk" to pk))
                    page.context().browser().close()
                }
            }

            get("douYin") {
                val url = call.request.queryParameters.getOrFail("url")
                call.respondTemplate("/exec/douYin", mapOf("url" to url))
            }

            post("douYin") {
                val formData = call.receiveParameters()
                val url = formData.getOrFail("url")
                val page = PlaywrightUtils.browser().newPage {
                    setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36")
                }
                var data: String? = null
                var userAgent: String? = null
                page.onRequest {
                    val requestUrl = it.url()
                    if (requestUrl.startsWith(url)) {
                        data = requestUrl
                        userAgent = it.headers()["user-agent"]
                    }
                }
                try {
                    withContext(Dispatchers.IO) {
                        page.navigate("http://localhost:$port/exec/douYin?url=${url.toUrlEncode()}")
                        withTimeout(1000 * 30) {
                            while (true) {
                                if (data != null && userAgent != null) {
                                    call.respond(mapOf("url" to data, "userAgent" to userAgent))
                                    break
                                }
                                delay(1000)
                            }
                        }
                    }
                } finally {
                    page.context().browser().close()
                }
            }

            get("douYinSign") {
                call.respondTemplate("/exec/douYinSign")
            }

            post("douYinSign") {
                val parameters = call.receiveParameters()
                val nonce = parameters.getOrFail("nonce")
                val page = PlaywrightUtils.browser().newPage()
                try {
                    page.navigate("http://localhost:$port/exec/douYinSign")
                    val data = page.evaluate("""window.byted_acrawler.sign("", "$nonce")""").toString()
                    call.respond(mapOf("sign" to data))
                } finally {
                    page.context().browser().close()
                }
            }
        }

    }

}