package coolio.zoewong.traverse.model

import android.net.Uri
import coolio.zoewong.traverse.database.StoryEntity
import java.util.Date

/**
 * Model representing a story.
 */
data class Story(
    val id: Long,
    val title: String,
    val dateMillis: Long,
    val location: String?,
    val coverUri: String? = null,
    val memories: List<Memory>? = null,
) {
    val timestampDate get() = Date(dateMillis)
}

/**
 * Converts the database representation of a story into the model representation.
 */
fun StoryEntity.toModel(
    memories: List<Memory>? = null,
): Story {
    return Story(
        id = this.id,
        title = this.title,
        dateMillis = this.timestamp,
        location = this.locationName,
        coverUri = this.coverUri?.toString(),
        memories = memories,
    )
}

/**
 * Converts the model representation of a story into the database representation.
 */
fun Story.toDatabase(): StoryEntity {
    return StoryEntity(
        id = this.id,
        title = this.title,
        timestamp = this.dateMillis,
        location = null,
        locationName = this.location,
        coverUri = this.coverUri?.let {
            Uri.parse(it)
        },
    )
}
