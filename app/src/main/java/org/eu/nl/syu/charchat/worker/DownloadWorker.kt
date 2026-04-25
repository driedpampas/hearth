package org.eu.nl.syu.charchat.worker

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
import org.eu.nl.syu.charchat.data.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "DownloadWorker"
private const val CHANNEL_ID = "model_download_channel"
private var channelCreated = false

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    init {
        if (!channelCreated) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Downloading",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
            channelCreated = true
        }
    }

    override suspend fun doWork(): Result {
        val urlStr = inputData.getString("url") ?: return Result.failure()
        val fileName = inputData.getString("fileName") ?: return Result.failure()

        return withContext(Dispatchers.IO) {
            try {
                setForeground(createForegroundInfo(0, fileName))

                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                
                // Add Authorization header if it's a HuggingFace URL
                if (urlStr.contains("huggingface.co")) {
                    val token = authRepository.getAccessToken()
                    if (token != null) {
                        connection.setRequestProperty("Authorization", "Bearer $token")
                        Log.d(TAG, "Using HuggingFace Auth Token")
                    }
                }

                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_FORBIDDEN || connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Log.e(TAG, "Auth failed: ${connection.responseCode}")
                    return@withContext Result.failure(
                        Data.Builder()
                            .putInt("error_code", connection.responseCode)
                            .putString("fileName", fileName)
                            .build()
                    )
                }

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP error: ${connection.responseCode}")
                    return@withContext Result.failure(
                        Data.Builder()
                            .putString("fileName", fileName)
                            .build()
                    )
                }

                val totalBytes = connection.contentLengthLong
                val modelsDir = File(applicationContext.filesDir, "models")
                if (!modelsDir.exists()) modelsDir.mkdirs()

                val outputFile = File(modelsDir, fileName)
                val tmpFile = File(modelsDir, "$fileName.tmp")

                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(tmpFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastProgressUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastProgressUpdate > 500 && totalBytes > 0) {
                        val progress = (downloadedBytes * 100 / totalBytes).toInt()
                        setProgress(Data.Builder().putInt("progress", progress).build())
                        setForeground(createForegroundInfo(progress, fileName))
                        lastProgressUpdate = currentTime
                    }
                }

                outputStream.close()
                inputStream.close()

                if (outputFile.exists()) {
                    outputFile.delete()
                }
                tmpFile.renameTo(outputFile)

                setProgress(Data.Builder().putInt("progress", 100).build())
                Result.success()

            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                Result.failure(Data.Builder().putString("error", e.message).build())
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0, "Model")
    }

    private fun createForegroundInfo(progress: Int, fileName: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading $fileName")
            .setContentText("Progress: $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()

        return ForegroundInfo(
            id.hashCode(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }
}
