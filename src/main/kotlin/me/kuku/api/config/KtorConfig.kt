@file:Suppress("unused")

package me.kuku.api.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.kuku.pojo.CommonResult
import org.springframework.stereotype.Component

@Component
class KtorConfig {

    fun Application.statusPages() {

        install(StatusPages) {

            exception<MissingRequestParameterException> { call, cause ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    CommonResult.failure(code = 400, message = cause.message ?: "参数丢失", data = null)
                )
            }

            exception<Throwable> { call, cause ->
                call.respond(HttpStatusCode.InternalServerError, CommonResult.failure<Unit>(cause.toString()))
                throw cause
            }
        }

    }

    fun Application.routing() {

        install(Routing) {

            install(DoubleReceive)

            staticResources("static", "/BOOT-INF/classes/static")
            staticResources("static", "/static")

        }
    }

}
