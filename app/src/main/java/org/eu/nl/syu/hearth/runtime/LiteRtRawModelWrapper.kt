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
import android.util.Log
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.TensorBuffer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtRawModelWrapper @Inject constructor() {
    private var model: CompiledModel? = null
    private var modelPath: String? = null

    fun isInitialized(): Boolean = model != null
    fun getLoadedModelPath(): String? = modelPath

    fun loadModel(path: String, accelerator: Accelerator = Accelerator.CPU) {
        close()
        try {
            val options = CompiledModel.Options(accelerator)
            model = CompiledModel.create(path, options)
            modelPath = path
            Log.i("LiteRtRawModelWrapper", "Raw model loaded successfully: $path")
        } catch (e: Exception) {
            Log.e("LiteRtRawModelWrapper", "Failed to load raw model: $path", e)
            close()
            throw e
        }
    }

    fun runInference(inputData: FloatArray): FloatArray? {
        val m = model ?: return null
        return try {
            val inputBuffers = m.createInputBuffers()
            val outputBuffers = m.createOutputBuffers()

            // For simplicity in this "adapter", we assume the first input matches inputData size
            // Real usage would need more complex buffer management
            val inputBuffer = inputBuffers[0]
            // We'll just pass the data and let LiteRT handle it
            val dataToUse = inputData

            inputBuffer.writeFloat(dataToUse)
            m.run(inputBuffers, outputBuffers)

            val outputBuffer = outputBuffers[0]
            val result = outputBuffer.readFloat()
            result
        } catch (e: Exception) {
            Log.e("LiteRtRawModelWrapper", "Inference failed", e)
            null
        }
    }

    fun close() {
        model?.close()
        model = null
        modelPath = null
    }
}
