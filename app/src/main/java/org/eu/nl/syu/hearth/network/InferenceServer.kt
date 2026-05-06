/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.eu.nl.syu.hearth.network

import android.util.Log
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.json.Json
import org.eu.nl.syu.hearth.runtime.LiteRtEngineWrapper
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InferenceServer @Inject constructor(
    private val engineWrapper: LiteRtEngineWrapper
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    fun start(port: Int = 8080) {
        if (server != null) return

        server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    encodeDefaults = true
                })
            }
            install(CORS) {
                anyHost()
                allowHeader(io.ktor.http.HttpHeaders.ContentType)
            }
            routing {
                post("/v1/chat/completions") {
                    val request = call.receive<ChatCompletionRequest>()
                    handleChatCompletion(call, request)
                }
                get("/v1/models") {
                    // Basic models list
                    call.respond(mapOf(
                        "object" to "list",
                        "data" to listOf(
                            mapOf(
                                "id" to (engineWrapper.getLoadedModelPath() ?: "local-model"),
                                "object" to "model",
                                "created" to System.currentTimeMillis() / 1000,
                                "owned_by" to "hearth"
                            )
                        )
                    ))
                }
            }
        }.start(wait = false)
        Log.i("InferenceServer", "Server started on port $port")
    }

    private suspend fun handleChatCompletion(call: ApplicationCall, request: ChatCompletionRequest) {
        if (!engineWrapper.isInitialized()) {
            call.respond(io.ktor.http.HttpStatusCode.ServiceUnavailable, "Model not loaded")
            return
        }

        val prompt = buildPrompt(request.messages)
        val modelId = engineWrapper.getLoadedModelPath() ?: "local-model"
        val created = System.currentTimeMillis() / 1000
        val id = "chatcmpl-${UUID.randomUUID()}"

        if (request.stream) {
            call.respondBytesWriter(contentType = io.ktor.http.ContentType.Text.EventStream) {
                engineWrapper.sendRawPrompt(prompt)
                    .onCompletion {
                        writeStringUtf8("data: [DONE]\n\n")
                        flush()
                    }
                    .collect { delta ->
                        val chunk = ChatCompletionChunk(
                            id = id,
                            created = created,
                            model = modelId,
                            choices = listOf(
                                ChatCompletionChunkChoice(
                                    index = 0,
                                    delta = ChatCompletionDelta(content = delta)
                                )
                            )
                        )
                        writeStringUtf8("data: ${Json.encodeToString(ChatCompletionChunk.serializer(), chunk)}\n\n")
                        flush()
                    }
            }
        } else {
            val fullResponse = StringBuilder()
            engineWrapper.sendRawPrompt(prompt).collect { delta ->
                fullResponse.append(delta)
            }
            val response = ChatCompletionResponse(
                id = id,
                created = created,
                model = modelId,
                choices = listOf(
                    ChatCompletionChoice(
                        index = 0,
                        message = ChatCompletionMessage(role = "assistant", content = fullResponse.toString())
                    )
                )
            )
            call.respond(response)
        }
    }

    private fun buildPrompt(messages: List<ChatCompletionMessage>): String {
        val sb = StringBuilder()
        messages.forEach { msg ->
            val role = when (msg.role.lowercase()) {
                "user" -> "User"
                "assistant" -> "Assistant"
                "system" -> "System"
                else -> msg.role
            }
            sb.append("$role: ${msg.content}\n")
        }
        sb.append("Assistant: ")
        return sb.toString()
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        Log.i("InferenceServer", "Server stopped")
    }

    fun isRunning(): Boolean = server != null
}
