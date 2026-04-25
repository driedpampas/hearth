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
                Extract character name, tagline, and system prompt lore from the following text into JSON format:
                {
                  "name": "...",
                  "tagline": "...",
                  "systemPromptLore": "..."
                }
                
                Text:
                $cleanText
            """.trimIndent()
            
            // Note: This assumes a model is already loaded or we use a default "scraper" model.
            // For now, I'll return a placeholder or implement the call if engine is ready.
            null // Placeholder for actual LLM parsing logic
        } catch (e: Exception) {
            null
        }
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
    }
}
