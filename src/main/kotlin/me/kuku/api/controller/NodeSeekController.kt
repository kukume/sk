package me.kuku.api.controller

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import me.kuku.api.logic.NodeSeekLogic

fun Application.nodeSeek() {
    routing {

        route("nodeseek") {

            get("sign") {
                val cookie = call.request.queryParameters.getOrFail("cookie")
                val randomStr = call.request.queryParameters["random"] ?: "false"
                val random = randomStr.toBooleanStrictOrNull() ?: false
                call.respond("{}")
                NodeSeekLogic.sign(cookie, random)
            }

            get("sign/query") {
                val cookie = call.request.queryParameters.getOrFail("cookie")
                call.respond(NodeSeekLogic.querySign(cookie))
            }

        }

    }
}