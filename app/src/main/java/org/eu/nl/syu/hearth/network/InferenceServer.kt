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
import io.ktor.server.plugins.calllogging.CallLogging
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
import org.slf4j.event.Level
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
            install(CallLogging) {
                level = Level.INFO
            }
            routing {
                post("/v1/chat/completions") {
                    val request = call.receive<ChatCompletionRequest>()
                    handleChatCompletion(call, request)
                }
                get("/v1/models") {
                    val modelPath = engineWrapper.getLoadedModelPath()
                    val models = if (modelPath != null) {
                        listOf(
                            ModelRecord(
                                id = modelPath,
                                created = System.currentTimeMillis() / 1000
                            )
                        )
                    } else {
                        emptyList()
                    }
                    val response = ModelListResponse(data = models)
                    call.respond(response)
                }
            }
        }.start(wait = false)
        Log.i("InferenceServer", "Server started on port $port")
    }

    private suspend fun handleChatCompletion(call: ApplicationCall, request: ChatCompletionRequest) {
        Log.i("InferenceServer", "Handling chat completion request (stream=${request.stream})")
        if (!engineWrapper.isInitialized()) {
            Log.w("InferenceServer", "Request failed: Model not loaded")
            call.respond(io.ktor.http.HttpStatusCode.ServiceUnavailable, "Model not loaded")
            return
        }

        val prompt = buildPrompt(request.messages)
        val modelId = engineWrapper.getLoadedModelPath() ?: "local-model"
        val created = System.currentTimeMillis() / 1000
        val id = "chatcmpl-${UUID.randomUUID()}"

        if (request.stream) {
            Log.i("InferenceServer", "Starting streaming response")
            try {
                call.respondBytesWriter(contentType = io.ktor.http.ContentType.Text.EventStream) {
                    engineWrapper.sendRawPromptStructured(prompt, enableThinking = true)
                        .onCompletion { cause ->
                            if (cause != null) {
                                Log.e("InferenceServer", "Stream completed with error: ${cause.message}", cause)
                            } else {
                                Log.i("InferenceServer", "Stream completed successfully")
                            }
                            try {
                                writeStringUtf8("data: [DONE]\n\n")
                                flush()
                            } catch (e: Exception) {
                                Log.w("InferenceServer", "Failed to write completion tag: ${e.message}")
                            }
                        }
                        .collect { delta ->
                            try {
                                val chunk = ChatCompletionChunk(
                                    id = id,
                                    created = created,
                                    model = modelId,
                                    choices = listOf(
                                        ChatCompletionChunkChoice(
                                            index = 0,
                                            delta = ChatCompletionDelta(
                                                content = delta.content,
                                                reasoningContent = delta.thought
                                            )
                                        )
                                    )
                                )
                                writeStringUtf8("data: ${Json.encodeToString(ChatCompletionChunk.serializer(), chunk)}\n\n")
                                flush()
                            } catch (e: Exception) {
                                Log.e("InferenceServer", "Error writing chunk to stream: ${e.message}")
                                throw e
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e("InferenceServer", "Crashed during streaming response: ${e.message}", e)
                // We can't really respond with another status if we already started respondBytesWriter
            }
        } else {
            Log.i("InferenceServer", "Generating non-streaming response")
            try {
                val fullResponse = StringBuilder()
                val fullReasoning = StringBuilder()
                engineWrapper.sendRawPromptStructured(prompt, enableThinking = true).collect { delta ->
                    delta.content?.let { fullResponse.append(it) }
                    delta.thought?.let { fullReasoning.append(it) }
                }
                val response = ChatCompletionResponse(
                    id = id,
                    created = created,
                    model = modelId,
                    choices = listOf(
                        ChatCompletionChoice(
                            index = 0,
                            message = ChatCompletionMessage(
                                role = "assistant", 
                                content = fullResponse.toString(),
                                reasoningContent = fullReasoning.toString().takeIf { it.isNotEmpty() }
                            )
                        )
                    )
                )
                call.respond(response)
                Log.i("InferenceServer", "Responded successfully")
            } catch (e: Exception) {
                Log.e("InferenceServer", "Error during non-streaming generation: ${e.message}", e)
                call.respond(io.ktor.http.HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
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
