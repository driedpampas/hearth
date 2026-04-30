package org.eu.nl.syu.charchat.runtime

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
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.ModelRepository
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtEngineWrapper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val modelRepository: ModelRepository
) {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private val _loadedModelPath = MutableStateFlow<String?>(null)

    val loadedModelPath: StateFlow<String?> = _loadedModelPath.asStateFlow()

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

    private fun loadModel(modelPath: String, backend: Backend, maxTokens: Int) {
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
        } catch (e: Exception) {
            nextEngine.close()
            throw e
        }
    }

    fun createConversation(character: Character): Flow<String> = callbackFlow {
        val config = ConversationConfig(
            systemInstruction = Contents.of(character.systemPromptLore),
            samplerConfig = SamplerConfig(
                temperature = character.temp.toDouble(),
                topK = character.topK,
                topP = character.topP.toDouble()
            )
        )
        
        conversation?.close()
        conversation = engine?.createConversation(config)

        awaitClose { 
            // We don't necessarily want to close the conversation here if it's reused,
            // but for a one-off stream it might be appropriate.
            // However, the wrapper manages the conversation lifecycle.
        }
    }

    fun sendMessage(text: String, reminder: String = ""): Flow<String> = callbackFlow {
        val messageContent = if (reminder.isNotEmpty()) {
            "$text\n\n[Reminder: $reminder]"
        } else {
            text
        }

        val callback = object : MessageCallback {
            override fun onMessage(message: Message) {
                trySend(message.toString())
            }

            override fun onDone() {
                close()
            }

            override fun onError(throwable: Throwable) {
                close(throwable)
            }
        }

        conversation?.sendMessageAsync(Contents.of(messageContent), callback)

        awaitClose {
            // Cancel process if the flow is cancelled
            conversation?.cancelProcess()
        }
    }

    fun close() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
        _loadedModelPath.value = null
    }
}
