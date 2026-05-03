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

package org.eu.nl.syu.hearth.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eu.nl.syu.hearth.data.AuthRepository
import org.eu.nl.syu.hearth.data.ModelRepository
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val TAG = "DownloadWorker"
private const val CHANNEL_ID = "model_download_channel"
private var channelCreated = false

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val modelRepository: ModelRepository
) : CoroutineWorker(context, params) {

    init {
        if (!channelCreated) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Downloading",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
            channelCreated = true
        }
    }

    override suspend fun doWork(): Result {
        val urlStr = inputData.getString("url") ?: return Result.failure()
        val fileName = inputData.getString("fileName") ?: return Result.failure()
        val modelMetadataJson = inputData.getString("modelMetadataJson")
        val modelsDir = File(applicationContext.filesDir, "models")
        val tmpFile = File(modelsDir, "$fileName.tmp")
        val outputFile = File(modelsDir, fileName)

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting download: $fileName from $urlStr")
                setForeground(createForegroundInfo(0, fileName, 0, -1))
                // Also set initial progress with fileName
                setProgress(Data.Builder().putFloat("progress", 0f).putString("fileName", fileName).build())

                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                
                if (urlStr.contains("huggingface.co")) {
                    val token = authRepository.getAccessToken()
                    if (token != null) {
                        connection.setRequestProperty("Authorization", "Bearer $token")
                        Log.d(TAG, "Applied HuggingFace Auth Token for $fileName")
                    } else {
                        Log.w(TAG, "HuggingFace URL detected but no Auth Token found. Attempting public access.")
                    }
                }

                Log.d(TAG, "Connecting to ${url.host}...")
                connection.connect()

                val responseCode = connection.responseCode
                Log.d(TAG, "Server responded with HTTP $responseCode")

                if (responseCode == HttpURLConnection.HTTP_FORBIDDEN || responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Log.e(TAG, "Authentication failed (HTTP $responseCode) for $fileName. Check if the model is gated.")
                    return@withContext Result.failure(
                        Data.Builder()
                            .putInt("error_code", responseCode)
                            .putString("fileName", fileName)
                            .build()
                    )
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Download failed for $fileName: Unexpected HTTP $responseCode")
                    return@withContext Result.failure(
                        Data.Builder()
                            .putString("fileName", fileName)
                            .build()
                    )
                }

                val totalBytes = connection.contentLengthLong
                Log.d(TAG, "File size: ${totalBytes / (1024 * 1024)} MB")
                
                if (!modelsDir.exists()) {
                    Log.d(TAG, "Creating models directory: ${modelsDir.absolutePath}")
                    modelsDir.mkdirs()
                }

                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(tmpFile)

                val buffer = ByteArray(16384)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastProgressUpdate = System.currentTimeMillis()
                var lastDownloadedBytes = 0L

                Log.d(TAG, "Downloading $fileName to ${tmpFile.name}...")
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) {
                        Log.i(TAG, "Download cancelled: $fileName")
                        break
                    }
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - lastProgressUpdate
                    if (timeDiff >= 1000) { // Update every 1000ms to avoid spamming notifications
                        val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes * 100f) else 0f
                        val progressInt = progress.toInt()
                        
                        val bytesSinceLast = downloadedBytes - lastDownloadedBytes
                        val speed = (bytesSinceLast * 1000 / timeDiff) // bytes per second
                        
                        val eta = if (totalBytes > 0 && speed > 0) {
                            (totalBytes - downloadedBytes) / speed
                        } else {
                            -1L
                        }

                        setProgress(Data.Builder()
                            .putFloat("progress", progress)
                            .putString("fileName", fileName)
                            .putLong("downloadedBytes", downloadedBytes)
                            .putLong("totalBytes", totalBytes)
                            .putLong("speed", speed)
                            .putLong("eta", eta)
                            .build())
                        
                        setForeground(createForegroundInfo(progressInt, fileName, speed, eta))
                        
                        lastProgressUpdate = currentTime
                        lastDownloadedBytes = downloadedBytes
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                if (isStopped) {
                    if (tmpFile.exists()) {
                        tmpFile.delete()
                        Log.d(TAG, "Cleaned up temporary file after cancellation: ${tmpFile.name}")
                    }
                    return@withContext Result.failure()
                }

                Log.d(TAG, "Download completed. Verifying and moving to final destination...")
                if (outputFile.exists()) {
                    Log.d(TAG, "Overwriting existing model file: $fileName")
                    outputFile.delete()
                }
                
                if (tmpFile.renameTo(outputFile)) {
                    Log.i(TAG, "Successfully downloaded and saved: $fileName")
                    modelMetadataJson?.let { json ->
                        val model = modelRepository.deserializeModel(json)?.copy(localFileName = fileName)
                        if (model != null) {
                            val hash = modelRepository.hashOf(outputFile)
                            val modelWithHash = if (hash.isNotEmpty()) model.copy(fileHash = hash) else model
                            modelRepository.cacheDownloadedModel(modelWithHash)
                            Log.d(TAG, "Cached metadata for $fileName with hash: $hash")
                        } else {
                            Log.w(TAG, "Downloaded model metadata could not be cached for $fileName")
                        }
                    }
                    return@withContext Result.success(
                        Data.Builder()
                            .putFloat("progress", 100f)
                            .putString("fileName", fileName)
                            .build()
                    )
                } else {
                    Log.e(TAG, "Failed to rename temporary file to $fileName")
                    return@withContext Result.failure(
                        Data.Builder()
                            .putString("fileName", fileName)
                            .putString("error", "Rename failed")
                            .build()
                    )
                }

            } catch (e: Exception) {
                if (tmpFile.exists()) {
                    tmpFile.delete()
                    Log.d(TAG, "Cleaned up temporary file after exception: ${tmpFile.name}")
                }
                Log.e(TAG, "Exception during download of $fileName", e)
                return@withContext Result.failure(
                    Data.Builder()
                        .putString("fileName", fileName)
                        .putString("error", e.message)
                        .build()
                )
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val fileName = inputData.getString("fileName") ?: "Model"
        return createForegroundInfo(0, fileName, 0, -1)
    }

    private fun createForegroundInfo(progress: Int, fileName: String, speed: Long, eta: Long): ForegroundInfo {
        val speedStr = if (speed > 0) {
            val kb = speed / 1024
            if (kb > 1024) "${String.format(Locale.US, "%.1f", kb / 1024f)} MB/s" else "$kb KB/s"
        } else ""

        val etaStr = if (eta > 0) {
            if (eta > 60) "${eta / 60}m ${eta % 60}s left" else "${eta}s left"
        } else ""

        val contentText = if (speedStr.isNotEmpty() || etaStr.isNotEmpty()) {
            "Progress: $progress% • $speedStr • $etaStr"
        } else {
            "Progress: $progress%"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading $fileName")
            .setContentText(contentText)
            //.setSmallIcon(org.eu.nl.syu.hearth.R.mipmap.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress <= 0)
            .build()

        return ForegroundInfo(
            id.hashCode(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }
}
