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

package org.eu.nl.syu.hearth.runtime

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.eu.nl.syu.hearth.data.Character
import org.eu.nl.syu.hearth.data.ChatMessage
import org.eu.nl.syu.hearth.data.MessageRole
import org.eu.nl.syu.hearth.data.ModelRepository
import org.eu.nl.syu.hearth.data.stripThinking
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
data class GenerationDelta(
    val thought: String? = null,
    val content: String? = null
)

@Singleton
class LiteRtEngineWrapper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val modelRepository: ModelRepository
) {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private val _loadedModelPath = MutableStateFlow<String?>(null)

    val loadedModelPath: StateFlow<String?> = _loadedModelPath.asStateFlow()
    private val _fallbackReason = MutableStateFlow<String?>(null)
    val fallbackReason: StateFlow<String?> = _fallbackReason.asStateFlow()

    fun isInitialized(): Boolean = engine != null

    fun getLoadedModelPath(): String? = _loadedModelPath.value

    private fun calculateHash(filePath: String): String {
        val file = File(filePath)
        if (!file.exists()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        val fis = FileInputStream(file)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        fis.close()
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun initialize(
        modelPath: String, 
        preferredBackend: Backend? = null,
        maxTokens: Int = 4096,
        onFallback: ((String) -> Unit)? = null
    ) {
        val hash = calculateHash(modelPath)
        val backendToUse = if (preferredBackend != null) {
            preferredBackend
        } else {
            // Automatic mode
            val cachedBackendLabel = modelRepository.getCachedBackend(hash)
            when (cachedBackendLabel) {
                "GPU" -> Backend.GPU()
                "CPU" -> Backend.CPU()
                "NPU" -> Backend.NPU(context.applicationInfo.nativeLibraryDir)
                else -> null // Try GPU first
            }
        }

        if (backendToUse != null) {
            try {
                loadModel(modelPath, backendToUse, maxTokens)
                return
            } catch (e: Exception) {
                onFallback?.invoke("Cached/Preferred backend failed, falling back to CPU.")
            }
        }

        // Automatic fallback chain: GPU -> CPU
        try {
            loadModel(modelPath, Backend.GPU(), maxTokens)
            modelRepository.setCachedBackend(hash, "GPU")
        } catch (e: Exception) {
            onFallback?.invoke("GPU not supported for this model. Falling back to CPU.")
            loadModel(modelPath, Backend.CPU(), maxTokens)
            modelRepository.setCachedBackend(hash, "CPU")
        }
    }


    private suspend fun loadModel(modelPath: String, backend: Backend, maxTokens: Int) {
        _fallbackReason.value = null
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            maxNumTokens = maxTokens,
            cacheDir = context.cacheDir.path
        )
        val nextEngine = Engine(config)
        try {
            nextEngine.initialize()
            engine?.close()
            engine = nextEngine
            _loadedModelPath.value = modelPath
            modelRepository.setLastLoadedModelPath(modelPath)
        } catch (e: Exception) {
            nextEngine.close()
            throw e
        }
    }
    
    fun ensureConversation() {
        if (conversation == null && engine != null) {
            val config = ConversationConfig(
                systemInstruction = Contents.of("You are a helpful assistant."),
                samplerConfig = SamplerConfig(
                    temperature = 0.7,
                    topK = 40,
                    topP = 0.95
                )
            )
            conversation = engine?.createConversation(config)
        }
    }

    fun createConversation(character: Character) {
        val systemInstruction = if (character.enableThinking && character.enableThinkingCompatibility && character.thinkingCompatibilityToken.isNotEmpty()) {
            character.thinkingCompatibilityToken + "\n" + character.roleInstruction
        } else {
            character.roleInstruction
        }

        val config = ConversationConfig(
            systemInstruction = Contents.of(systemInstruction),
            samplerConfig = SamplerConfig(
                temperature = character.temp.toDouble(),
                topK = character.topK,
                topP = character.topP.toDouble()
            ),
            extraContext = mapOf("enable_thinking" to character.enableThinking)
        )
        
        conversation?.close()
        conversation = engine?.createConversation(config)
    }

    fun sendMessage(
        text: String,
        reminder: String = "",
        history: List<ChatMessage> = emptyList(),
        includeThinking: Boolean = false,
        enableThinking: Boolean = false
    ): Flow<String> = callbackFlow {
        val fullPrompt = StringBuilder()
        
        if (history.isNotEmpty()) {
            history.filter { !it.isHiddenFromAi }.forEach { msg ->
                val roleName = if (msg.role == MessageRole.USER) "User" else "Assistant"
                // Clean content from thinking tags if not requested to keep them
                var content = msg.content
                if (!includeThinking) {
                    content = content.stripThinking()
                }
                if (content.isNotEmpty()) {
                    fullPrompt.append("$roleName: $content\n")
                }
            }
            fullPrompt.append("User: ")
        }
        
        fullPrompt.append(text)
        
        if (reminder.isNotEmpty()) {
            fullPrompt.append("\n\n[Reminder: $reminder]")
        }

        // Trigger for Assistant response
        fullPrompt.append("\nAssistant: ")

        sendRawPrompt(fullPrompt.toString(), enableThinking).collect { trySend(it) }
        awaitClose { conversation?.cancelProcess() }
    }

    fun sendRawPrompt(
        prompt: String,
        enableThinking: Boolean = false
    ): Flow<String> = callbackFlow {
        sendRawPromptStructured(prompt, enableThinking).collect { delta ->
            val response = StringBuilder()
            delta.thought?.let { 
                // We don't know if it's the start or middle, so we rely on sendRawPromptStructured 
                // to handle the tags if we want them here, but structured delta is cleaner.
                // Actually, for backward compatibility, we'll format it here.
                response.append(it) 
            }
            delta.content?.let { response.append(it) }
            if (response.isNotEmpty()) trySend(response.toString())
        }
        awaitClose { /* handled in sendRawPromptStructured */ }
    }

    fun sendRawPromptStructured(
        prompt: String,
        enableThinking: Boolean = false
    ): Flow<GenerationDelta> = callbackFlow {
        ensureConversation()
        
        var inThinkingMode = false
        var lastThought = ""
        var lastMain = ""

        val callback = object : MessageCallback {
            override fun onMessage(message: Message) {
                val currentThought = message.channels["thought"] ?: ""
                val currentMain = message.toString()
                
                val thoughtDelta = if (currentThought.startsWith(lastThought)) {
                    currentThought.substring(lastThought.length)
                } else {
                    currentThought
                }
                
                val mainDelta = if (currentMain.startsWith(lastMain)) {
                    currentMain.substring(lastMain.length)
                } else {
                    currentMain
                }
                
                if (thoughtDelta.isNotEmpty()) {
                    if (!inThinkingMode) {
                        trySend(GenerationDelta(thought = "<think>\n$thoughtDelta"))
                        inThinkingMode = true
                    } else {
                        trySend(GenerationDelta(thought = thoughtDelta))
                    }
                }
                
                if (mainDelta.isNotEmpty()) {
                    if (inThinkingMode) {
                        trySend(GenerationDelta(content = "\n</think>\n$mainDelta"))
                        inThinkingMode = false
                    } else {
                        trySend(GenerationDelta(content = mainDelta))
                    }
                }
                
                lastThought = currentThought
                lastMain = currentMain
            }

            override fun onDone() {
                if (inThinkingMode) {
                    trySend(GenerationDelta(content = "\n</think>"))
                }
                close()
            }

            override fun onError(throwable: Throwable) {
                close(throwable)
            }
        }

        conversation?.sendMessageAsync(
            Contents.of(prompt), 
            callback,
            extraContext = mapOf("enable_thinking" to enableThinking)
        ) ?: run {
            close(IllegalStateException("Conversation not initialized. Ensure a character is selected or model is ready."))
        }

        awaitClose {
            conversation?.cancelProcess()
        }
    }

    suspend fun close() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
        _loadedModelPath.value = null
        _fallbackReason.value = null
        modelRepository.setLastLoadedModelPath(null)
    }
}
