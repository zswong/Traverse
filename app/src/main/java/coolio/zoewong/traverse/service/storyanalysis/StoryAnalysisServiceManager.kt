package coolio.zoewong.traverse.service.storyanalysis

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import coolio.zoewong.traverse.model.Story

class StoryAnalysisServiceManager {
    private var mutex = object {}

    @Volatile
    private var service: StoryAnalysisService.Binder? = null

    @Volatile
    private var serviceStarted = false

    private var queue = StoryAnalysisServiceQueue()

    /**
     * Stops the service and any background processing.
     */
    fun shutdown() {
        Log.d(LOG_TAG, "Shutting down service")

        synchronized(mutex) {
            service?.stop()
        }
    }

    /**
     * Starts the service.
     */
    fun start(context: Context) {
        Log.d(LOG_TAG, "Starting service")

        synchronized(mutex) {
            if (serviceStarted) {
                return
            }

            serviceStarted = true
        }

        context.apply {
            val serviceIntent = Intent(this, StoryAnalysisService::class.java)
            serviceIntent.identifier = this.javaClass.name
            startForegroundService(serviceIntent)
            bindService(serviceIntent, connection, 0)
        }
    }

    /**
     * Queues a story for analysis.
     */
    fun queueForAnalysis(story: Story) {
        queue.enqueue(story.id)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            serviceBinder: IBinder?
        ) {
            Log.d(LOG_TAG, "Connected to Service")
            synchronized(mutex) {
                service = (serviceBinder as StoryAnalysisService.Binder).apply {
                    setQueue(queue)
                }
                serviceStarted = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(LOG_TAG, "Disconnected from Service")
            synchronized(mutex) {
                service = null
                serviceStarted = false
            }

        }
    }

    companion object {
        private const val LOG_TAG = "StoryAnalysisServiceManager"
    }
}
