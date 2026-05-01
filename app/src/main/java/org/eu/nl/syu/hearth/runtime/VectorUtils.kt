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

import org.eu.nl.syu.hearth.runtime.VectorUtils.TARGET_DIMS
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility for vector processing, specifically for Matryoshka Representation Learning (MRL)
 * and SQLite serialization.
 */
object VectorUtils {
    const val TARGET_DIMS = 256

    /**
     * Truncates a high-dimensional embedding (e.g., 768) to [TARGET_DIMS] (256)
     * and converts it to a Little-Endian ByteArray for sqlite-vec storage.
     */
    fun processEmbedding(raw: FloatArray): ByteArray {
        // Matryoshka Truncation: Only take the first 256 dimensions.
        // This retains ~95%+ retrieval accuracy for Gemma models while saving 3x space.
        val truncated = if (raw.size > TARGET_DIMS) {
            raw.copyOfRange(0, TARGET_DIMS)
        } else {
            raw
        }
        
        val buffer = ByteBuffer.allocate(truncated.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        truncated.forEach { buffer.putFloat(it) }
        return buffer.array()
    }
}
