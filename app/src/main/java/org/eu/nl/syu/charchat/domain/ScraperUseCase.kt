package org.eu.nl.syu.charchat.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eu.nl.syu.charchat.runtime.LiteRtEngineWrapper
import java.net.URL
import javax.inject.Inject

class ScraperUseCase @Inject constructor(
    private val engineWrapper: LiteRtEngineWrapper
) {
    suspend fun scrapeCharacterFromUrl(url: String): Map<String, String>? = withContext(Dispatchers.IO) {
        try {
            val html = URL(url).readText()
            val cleanText = stripHtml(html)
            
            // Pass to LLM for parsing
            val prompt = """
                Extract character name, tagline, and system prompt lore from the following text into JSON format.
                
                Text:
                $cleanText
                
                JSON:
                {
                  "name": "...",
                  "tagline": "...",
                  "systemPromptLore": "..."
                }
            """.trimIndent()
            
            if (!engineWrapper.isInitialized()) {
                throw IllegalStateException("LLM Engine is not initialized. Please load a model first.")
            }
            
            var result: String? = null
            engineWrapper.sendMessage(prompt).collect { partial ->
                result = (result ?: "") + partial
            }
            
            parseJsonToMap(result)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseJsonToMap(json: String?): Map<String, String>? {
        if (json == null) return null
        return try {
            // Very basic manual parsing for demonstration
            val name = json.substringAfter("\"name\": \"").substringBefore("\"")
            val tagline = json.substringAfter("\"tagline\": \"").substringBefore("\"")
            val lore = json.substringAfter("\"systemPromptLore\": \"").substringBefore("\"")
            mapOf("name" to name, "tagline" to tagline, "systemPromptLore" to lore)
        } catch (e: Exception) {
            null
        }
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
    }
}
