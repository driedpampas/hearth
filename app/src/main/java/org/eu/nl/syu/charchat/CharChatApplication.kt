package org.eu.nl.syu.charchat

import android.app.Application
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import dagger.hilt.android.HiltAndroidApp
import org.eu.nl.syu.charchat.runtime.LiteRtEngineWrapper
import javax.inject.Inject

@HiltAndroidApp
class CharChatApplication : Application(), Configuration.Provider, ImageLoaderFactory {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var engineWrapper: LiteRtEngineWrapper

    private val closeHandler = Handler(Looper.getMainLooper())
    private val closeRunnable = Runnable { engineWrapper.close() }
    private var startedActivityCount = 0

    private val activityCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityStarted(activity: Activity) {
            startedActivityCount += 1
            closeHandler.removeCallbacks(closeRunnable)
        }

        override fun onActivityResumed(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) {
            startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
            if (startedActivityCount == 0) {
                closeHandler.removeCallbacks(closeRunnable)
                closeHandler.postDelayed(closeRunnable, 2000)
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityCallbacks)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
}
