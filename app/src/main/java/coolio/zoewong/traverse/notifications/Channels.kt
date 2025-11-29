package coolio.zoewong.traverse.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * An enum of notification channels used by this app.
 */
enum class NotificationChannels(
    val id: String,
    val channelName: String,
    val importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
) {
    SERVICES(
        id = "Services",
        channelName = "Services",
        importance = NotificationManager.IMPORTANCE_LOW
    );

    fun toNotificationChannel(): NotificationChannel {
        return NotificationChannel(id, channelName, importance)
    }

    fun prepare(context: Context) {
        if (Build.VERSION.SDK_INT > 26) {
            val channel = toNotificationChannel()
            NotificationManagerCompat.from(context).apply {
                createNotificationChannel(channel)
            }
        }
    }
}
