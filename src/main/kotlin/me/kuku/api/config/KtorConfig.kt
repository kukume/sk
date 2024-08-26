@file:Suppress("unused")

package me.kuku.api.config

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kuku.pojo.CommonResult
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.net.URLDecoder

fun Application.config() {

    install(CallLogging)

    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
            isCacheable = false
        })
    }

    install(ContentNegotiation) {
        val mapper = ObjectMapper()
        mapper.apply {
            setDefaultPrettyPrinter(
                DefaultPrettyPrinter().apply {
                    indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                    indentObjectsWith(DefaultIndenter("  ", "\n"))
                }
            )
        }
        mapper.registerKotlinModule()
        register(ContentType.Application.Json, JacksonConverter(mapper, true))
        register(ContentType.Application.FormUrlEncoded, FormUrlEncodedConverter(mapper))
    }

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


    install(Routing) {

        install(DoubleReceive)

        staticResources("static", "/BOOT-INF/classes/static")
        staticResources("static", "/static")

    }

}


class FormUrlEncodedConverter(private val objectMapper: ObjectMapper) : ContentConverter {

    override suspend fun serializeNullable(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?
    ): OutgoingContent {
        return OutputStreamContent(
            {
                val jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(value))
                val sb = StringBuilder()
                jsonNode.fieldNames().forEach {
                    sb.append(it).append(jsonNode.get(it)).append("&")
                }
                objectMapper.writeValue(this, sb.removeSuffix("&").toString())
            },
            contentType.withCharsetIfNeeded(charset)
        )
    }

    override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any? {
        try {
            return withContext(Dispatchers.IO) {
                val reader = content.toInputStream().reader(charset)
                val body = reader.readText()
                val objectNode = objectMapper.createObjectNode()
                body.split("&").forEach {
                    val arr = it.split("=")
                    val k = arr[0]
                    val v = URLDecoder.decode(arr[1], "utf-8")
                    objectNode.put(k, v)
                }
                objectMapper.treeToValue(objectNode, objectMapper.constructType(typeInfo.reifiedType))
            }
        } catch (deserializeFailure: Exception) {
            val convertException = JsonConvertException("Illegal json parameter found", deserializeFailure)

            when (deserializeFailure) {
                is JsonParseException -> throw convertException
                is JsonMappingException -> throw convertException
                else -> throw deserializeFailure
            }
        }
    }
}