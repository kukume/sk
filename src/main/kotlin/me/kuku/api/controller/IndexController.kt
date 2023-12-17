@file:Suppress("unused")

package me.kuku.api.controller

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.springframework.stereotype.Component

@Component
class IndexController {

    fun Routing.index() {

        get {
            call.respond(mapOf("message" to "Powered by Ktor", "github" to "https://github.com/kukume/api"))
        }

        staticResources("favicon.ico", "BOOT-INF/classes/static", "favicon.ico")
        staticResources("favicon.ico", "static", "favicon.ico")

    }
}