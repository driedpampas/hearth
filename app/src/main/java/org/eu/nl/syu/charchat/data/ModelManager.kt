package org.eu.nl.syu.charchat.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Progress(val progress: Float) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    fun getModelsDir(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun downloadModel(url: String, fileName: String) {
        withContext(Dispatchers.IO) {
            _downloadState.value = DownloadState.Progress(0f)
            val file = File(getModelsDir(), fileName)
            try {
                val connection = URL(url).openConnection()
                val totalSize = connection.contentLengthLong
                connection.getInputStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (totalSize > 0) {
                                _downloadState.value = DownloadState.Progress(totalBytesRead.toFloat() / totalSize)
                            }
                        }
                    }
                }
                _downloadState.value = DownloadState.Success(file)
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun getLocalModels(): List<File> {
        return getModelsDir().listFiles()?.toList() ?: emptyList()
    }
}
