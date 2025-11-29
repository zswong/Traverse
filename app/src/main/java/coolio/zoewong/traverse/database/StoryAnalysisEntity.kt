package coolio.zoewong.traverse.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.android.gms.maps.model.LatLng
import java.util.Date

/**
 * Database entity containing information for AI analysis of a story.
 */
@Entity(
    tableName = "story_analysis",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["story_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@TypeConverters(Converters::class)
data class StoryAnalysisEntity(

    /**
     * The primary key of the story.
     */
    @PrimaryKey()
    @ColumnInfo(name = "story_id")
    val storyId: Long = 0L,

    /**
     * The ID of the last analyzed memory.
     */
    @ColumnInfo(name = "last_analyzed_memory_id")
    val lastAnalyzedMemoryId: Long?,

    /**
     * The ID of the latest memory in the story.
     */
    @ColumnInfo(name = "latest_memory_id")
    val latestMemoryId: Long?,

    /**
     * The human-readable summary of the story.
     */
    @ColumnInfo(name = "summary")
    val summary: String?,

    /**
     * The summary to feed back to the AI model as context, used when re-generating the summary
     * after new memories are added.
     */
    @ColumnInfo(name = "model_summary")
    val modelSummary: String,

)