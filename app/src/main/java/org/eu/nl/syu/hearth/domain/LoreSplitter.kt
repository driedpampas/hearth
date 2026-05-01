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

/**
 * Utility to chunk large character bios into searchable pieces.
 */
object LoreSplitter {

    /**
     * Splits a large text into chunks of approximately [chunkSize] characters,
     * with an overlap of [overlap] characters.
     */
    fun splitLore(text: String, chunkSize: Int = 500, overlap: Int = 50): List<String> {
        if (text.isEmpty()) return emptyList()
        if (text.length <= chunkSize) return listOf(text)

        val chunks = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val end = (i + chunkSize).coerceAtMost(text.length)
            
            // Try to find a good breaking point (e.g., period or newline) near the end
            var breakPoint = end
            if (end < text.length) {
                var found = false
                for (j in end downTo (end - 100).coerceAtLeast(i)) {
                    if (text[j] == '\n' || text[j] == '.' || text[j] == '?' || text[j] == '!') {
                        breakPoint = j + 1
                        found = true
                        break
                    }
                }
                // If no good break point, just cut at chunkSize
                if (!found) {
                    for (j in end downTo (end - 100).coerceAtLeast(i)) {
                        if (text[j] == ' ') {
                            breakPoint = j + 1
                            break
                        }
                    }
                }
            }

            chunks.add(text.substring(i, breakPoint).trim())
            
            if (breakPoint == text.length) {
                break
            }
            
            // Move forward, ensuring overlap
            i = breakPoint - overlap
        }

        return chunks
    }
}
