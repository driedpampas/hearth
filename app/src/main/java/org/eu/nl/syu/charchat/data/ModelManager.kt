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
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.map
import androidx.lifecycle.asFlow
import org.eu.nl.syu.charchat.worker.DownloadWorker

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
        val workManager = WorkManager.getInstance(context)
        
        val data = Data.Builder()
            .putString("url", url)
            .putString("fileName", fileName)
            .build()
            
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .addTag("model_download")
            .build()
            
        workManager.enqueue(request)
        
        // We observe the work info directly if needed, but for now we just start it.
        // The foreground service notification will handle the UI for the global app.
    }

    fun getLocalModels(): List<File> {
        return getModelsDir().listFiles()?.toList() ?: emptyList()
    }
}
