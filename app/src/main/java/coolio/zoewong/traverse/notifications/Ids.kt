package coolio.zoewong.traverse.notifications

import java.util.concurrent.atomic.AtomicInteger

private val nextIdCounter = AtomicInteger(1)

/**
 * Returns a unique ID for the next notification.
 */
fun NextNotificationId(): Int = nextIdCounter.getAndIncrement()
