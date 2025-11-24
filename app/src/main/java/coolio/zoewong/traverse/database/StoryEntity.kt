package coolio.zoewong.traverse.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.android.gms.maps.model.LatLng

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
     * The cover image for the story.
     */
    @ColumnInfo(name = "cover_uri")
    val coverUri: Uri?,

    /**
     * The location where the story takes place.
     */
    @ColumnInfo(name = "loccation")
    val location: LatLng?

)
