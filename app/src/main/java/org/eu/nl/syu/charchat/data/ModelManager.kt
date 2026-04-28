package org.eu.nl.syu.charchat.data

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eu.nl.syu.charchat.worker.DownloadWorker
import java.io.File
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
    @param:ApplicationContext private val context: Context
) {
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    fun getModelsDir(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun downloadModel(url: String, fileName: String, modelMetadataJson: String? = null) {
        val workManager = WorkManager.getInstance(context)
        val dataBuilder = Data.Builder()
            .putString("url", url)
            .putString("fileName", fileName)

        if (modelMetadataJson != null) {
            dataBuilder.putString("modelMetadataJson", modelMetadataJson)
        }

        val data = dataBuilder.build()
            
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .addTag("model_download")
            .build()
            
        workManager.enqueueUniqueWork("model_download_$fileName", ExistingWorkPolicy.REPLACE, request)
        
        // We observe the work info directly if needed, but for now we just start it.
        // The foreground service notification will handle the UI for the global app.
    }

    fun getLocalModels(): List<File> {
        return getModelsDir().listFiles()?.toList() ?: emptyList()
    }

    fun deleteModel(fileName: String): Boolean {
        val file = File(getModelsDir(), fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
