package coolio.zoewong.traverse.service.storyanalysis

import android.util.Log
import coolio.zoewong.traverse.util.MutableWaitFor
import coolio.zoewong.traverse.util.WaitFor
import java.io.Closeable
import java.util.LinkedList

/**
 * Manages a queue of stories to be analyzed by the service.
 */
class StoryAnalysisServiceQueue {
    private val mutex = Object()

    private var queue = LinkedList<Long>()
    private var queuedItems = mutableSetOf<Long>()
    private var heldItems = mutableMapOf<Long, HoldGuard>()
    private var waitForNotEmpty = MutableWaitFor<Unit>()

    /**
     * Returns the next story to be analyzed, or null if there are no more stories.
     *
     * Example:
     *
     *     var cancelled = false
     *     val item = queue.queue.next({ cancelled = true })
     *     item?.use {
     *         // Do steps to complete long-running task
     *         if (cancelled) {
     *             return@use
     *         }
     *
     *         // etc.
     *     }
     */
    fun next(onCancel: () -> Unit): HoldGuard? {
        synchronized(mutex) {
            if (queue.isEmpty()) {
                return null
            }

            val storyId = queue.removeFirst()
            queuedItems.remove(storyId)
            if (queuedItems.isEmpty()) {
                waitForNotEmpty.done(Unit) // ensure the old one doesn't block
                waitForNotEmpty = MutableWaitFor()
            }

            val guard = HoldGuard(storyId, cancel = onCancel)
            heldItems[storyId] = guard

            return guard
        }
    }

    /**
     * Waits until the queue has at least one item.
     */
    suspend fun waitUntilNotEmpty() {
        val waitFor = synchronized(mutex) { waitForNotEmpty }
        waitFor()
    }

    /**
     * Enqueues the given story for analysis.
     *
     * If the story is already queued, it will not be requeued.
     * If the story is already being analyzed, it will be canceled and requeued.
     */
    fun enqueue(storyId: Long) {
        synchronized(mutex) {
            // If the service has a HeldGuard for that story, it was in the middle of processing it.
            // Cancel the work and requeue it.
            heldItems[storyId]?.let {
                Log.d(LOG_TAG, "Story $storyId currently being processed. Cancelling.")
                it.cancel()
                heldItems.remove(it.storyId)
            }

            // If the story was already queued, don't queue it again.
            if (queuedItems.contains(storyId)) {
                Log.d(LOG_TAG, "Story $storyId already queued.")
                return
            }

            // Queue the story.
            queuedItems.add(storyId)
            queue.push(storyId)
            Log.d(LOG_TAG, "Story $storyId queued at position ${queue.size - 1}.")

            // Let the service know there's data ready.
            waitForNotEmpty.done(Unit)
        }
    }

    inner class HoldGuard(
        val storyId: Long,
        internal val cancel: () -> Unit,
    ) : Closeable {
        override fun close() {
            synchronized(mutex) {
                heldItems.remove(storyId)
            }
        }
    }

    companion object {
        private const val LOG_TAG = "StoryAnalysisServiceQueue"
    }
}
