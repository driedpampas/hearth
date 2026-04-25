package org.eu.nl.syu.charchat.runtime

import android.content.Context
import com.google.ai.edge.litertlm.*
import com.google.ai.edge.litertlm.Contents
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.eu.nl.syu.charchat.data.Character
import org.eu.nl.syu.charchat.data.ChatMessage
import org.eu.nl.syu.charchat.data.MessageRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtEngineWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    suspend fun initialize(modelPath: String, backend: Backend = Backend.GPU()) {
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            cacheDir = context.cacheDir.path
        )
        engine = Engine(config).apply { initialize() }
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
    }
}
