package coolio.zoewong.traverse.service.storyanalysis

import android.app.Notification
import android.app.NotificationChannel
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import coolio.zoewong.traverse.R
import coolio.zoewong.traverse.ai.InferenceModel
import coolio.zoewong.traverse.ai.ModelExtractor
import coolio.zoewong.traverse.database.TraverseRepository
import coolio.zoewong.traverse.notifications.NextNotificationId
import coolio.zoewong.traverse.notifications.NotificationChannels
import coolio.zoewong.traverse.service.storyanalysis.StoryAnalysisEvent.StoryAnalysisCancelled
import coolio.zoewong.traverse.service.storyanalysis.StoryAnalysisEvent.StoryAnalysisFailed
import coolio.zoewong.traverse.service.storyanalysis.StoryAnalysisEvent.StoryAnalysisFinished
import coolio.zoewong.traverse.service.storyanalysis.StoryAnalysisEvent.StoryAnalysisStarted
import coolio.zoewong.traverse.util.MutableWaitFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException


/**
 * Background service for analyzing stories with AI.
 */
class StoryAnalysisService : Service() {
    private val binder = Binder()
    private var serviceMainJob: Job? = null

    private val notificationChannelConfig = NotificationChannels.SERVICES
    private val notificationId = NextNotificationId()
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notification: Notification

    private lateinit var queue: StoryAnalysisServiceQueue
    private var events: MutableSharedFlow<StoryAnalysisEvent>? = null
    private lateinit var inferenceModel: InferenceModel
    private val waitForQueueSetup = MutableWaitFor<Unit>()
    private val waitForEventsSetup = MutableWaitFor<Unit>()


    /**
     * The main entrypoint of the service.
     */
    private suspend fun serviceMain() {
        Log.d(LOG_TAG, "Loading database")
        val db = TraverseRepository.getInstance(this)

        Log.d(LOG_TAG, "Loading model")
        try {
            inferenceModel = InferenceModel(this, ModelExtractor.Models.GEMMA_1B)
            inferenceModel.initialize()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to initialize model", e)
            emitEvent(StoryAnalysisEvent.ModelInitializationFailed(e))
            stopSelf()
            return
        }

        waitForQueueSetup()
        serviceMainLoop(db)
    }

    /**
     * Emits an event to the service manager instance.
     */
    private suspend fun emitEvent(event: StoryAnalysisEvent) {
        waitForEventsSetup()
        events?.emit(event)
    }

    /**
     * The main loop of the service.
     *
     * The coroutine must be explicitly cancelled to exit the function.
     */
    private suspend fun serviceMainLoop(
        db: TraverseRepository,
    ) {
        while (currentCoroutineContext().isActive) {
            updateNotification {
                setContentText("Everything is up-to-date!")
            }

            Log.d(LOG_TAG, "Waiting for next story in queue")
            queue.waitUntilNotEmpty()

            // Launch a coroutine in the Default executor to analyze the story.
            // It will need to wait for the story to be taken from the queue first.
            Log.d(LOG_TAG, "Launching background job to analyze story")
            val waitForStoryId = MutableWaitFor<Long>()
            val processingJob = CoroutineScope(Dispatchers.Default).launch {
                delay(1000) // Debounce
                val storyId = waitForStoryId()
                var exception: Throwable? = null
                try {
                    emitEvent(StoryAnalysisStarted(storyId))
                    analyzeStory(db, storyId)
                } catch (e: Throwable) {
                    exception = e
                } finally {
                    val cancelled = !currentCoroutineContext().isActive
                    emitEvent(
                        when {
                            cancelled -> StoryAnalysisCancelled(storyId)
                            exception != null -> StoryAnalysisFailed(storyId, exception)
                            else -> StoryAnalysisFinished(storyId)
                        }
                    )
                }
            }

            // Get the next story from the queue.
            // Provide an onCancel function to cancel the previously-launched processing job.
            val next = queue.next(onCancel = {
                Log.d(LOG_TAG, "Canceling analysis of current story.")
                processingJob.cancel()
            })

            // Wait for the story to be analyzed by the processing job.
            try {
                next?.use {
                    Log.d(LOG_TAG, "Waiting for background job to complete.")
                    waitForStoryId.done(it.storyId)
                    processingJob.join()
                }
            } catch (e: CancellationException) {
                processingJob.cancel()
            }
        }
    }

    /**
     * Analyzes a Story using AI.
     */
    private suspend fun analyzeStory(
        db: TraverseRepository,
        storyId: Long,
    ) {
        val story = db.stories.get(storyId) ?: return
        val analysis = db.stories.getAnalysis(story) ?: return
        val memories = db.stories.getMemoriesOf(story)

        if (analysis.lastAnalyzedMemoryId == analysis.latestMemoryId) {
            Log.i(
                LOG_TAG,
                "Story #${story.id} already analyzed: memory=${analysis.latestMemoryId}, analyzed=${analysis.lastAnalyzedMemoryId}"
            )
            return
        }

        Log.i(LOG_TAG, "Analyzing story: $story $analysis")
        updateNotification {
            setContentText(story.title)
        }

        // Analyze the story in chunks.
        val analyzer = StoryAnalyzer(
            inferenceModel,
            analysis,
            memories,
            onProgressChange = {
                events?.tryEmit(StoryAnalysisEvent.StoryAnalysisProgressUpdate(storyId, it))
            })

        val updatedAnalysis = analyzer.run()

        db.stories.updateAnalysis(story, updatedAnalysis)
        Log.d(LOG_TAG, "Done analyzing.")
    }


    override fun onCreate() {
        Log.i(LOG_TAG, "Created Service instance")

        notificationChannel = notificationChannelConfig.toNotificationChannel()
        notificationManager = NotificationManagerCompat.from(this)
        prepareNotification()
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "Destroying Service instance")
        serviceMainJob?.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(LOG_TAG, "App closed, stopping Service")
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Promote to foreground service.
        // This is needed to receive location updates when the app is not visible.
        // https://developer.android.com/develop/background-work/services/fgs/launch#promote-service
        Log.i(LOG_TAG, "Promoting Service to foreground")

        try {
            ServiceCompat.startForeground(
                this,
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                // MEDIA_PROCESSING didn't work:
                // "android.app.InvalidForegroundServiceTypeException: Starting FGS with type none"
            )
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "Failed to promote Service to foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }

        serviceMainJob = CoroutineScope(Dispatchers.IO).launch {
            Log.i(LOG_TAG, "Starting service main loop")
            serviceMain()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(LOG_TAG, "Bound: ${intent?.identifier}")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(LOG_TAG, "Unbound: ${intent?.identifier}")
        return super.onUnbind(intent)
    }

    /**
     * Creates the service Notification and the corresponding NotificationChannel.
     */
    private fun prepareNotification() {
        notificationChannelConfig.prepare(this)
        notification = NotificationCompat.Builder(this, notificationChannel.id)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Story Analysis")
            .setContentText("Starting service...")
            .setSilent(true)
            .build()
    }

    private fun updateNotification(updateFun: NotificationCompat.Builder.() -> NotificationCompat.Builder) {
        val context = this
        CoroutineScope(Dispatchers.Main).launch {
            notification = NotificationCompat.Builder(context, notificationChannel.id)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Story Analysis")
                .setSilent(true)
                .updateFun()
                .build()

            try {
                notificationManager.notify(notificationId, notification)
            } catch (e: SecurityException) {
                Log.e(LOG_TAG, "Failed to update notification", e)
            }
        }
    }

    inner class Binder : android.os.Binder() {

        /**
         * Stops the service.
         */
        fun stop() {
            stopSelf()
        }

        /**
         * Set the queue of stories to be analyzed.
         */
        fun setQueue(queue: StoryAnalysisServiceQueue) {
            this@StoryAnalysisService.queue = queue
            waitForQueueSetup.done(Unit)
        }

        /**
         * Returns a flow of StoryAnalysisEvents.
         */
        fun setEventEmitter(events: MutableSharedFlow<StoryAnalysisEvent>) {
            this@StoryAnalysisService.events = events
            waitForEventsSetup.done(Unit)
        }

    }

    companion object {
        private const val LOG_TAG = "StoryAnalysisService"
    }
}