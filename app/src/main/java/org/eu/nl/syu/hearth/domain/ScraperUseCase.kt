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

package org.eu.nl.syu.hearth.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eu.nl.syu.hearth.runtime.LiteRtEngineWrapper
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
