package coolio.zoewong.traverse.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.android.gms.maps.model.LatLng
import java.util.Date

/**
 * Database entity representing a story.
 */
@Entity(tableName = "stories")
@TypeConverters(Converters::class)
data class StoryEntity(

    /**
     * The primary key of the memory.
     * This is auto-generated starting from 1.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /**
     * The story title.
     */
    @ColumnInfo(name = "title")
    val title: String,

    /**
     * The timestamp of the story.
     */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /**
     * The cover image for the story.
     */
    @ColumnInfo(name = "cover_uri")
    val coverUri: Uri?,

    /**
     * The location where the story takes place.
     */
    @ColumnInfo(name = "location")
    val location: LatLng?,

    /**
     * The name of the location where the story takes place.
     */
    @ColumnInfo(name = "location_name")
    val locationName: String?

) {

    /**
     * The timestamp of the story.
     */
    val timestampDate: Date
        get() = Date(timestamp)

}

