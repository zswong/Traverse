package coolio.zoewong.traverse.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

/**
 * Database access object for the traverse database.
 */
@Dao
interface StorySegmentAccess {

    @Insert
    suspend fun insertStorySegment(segment: StorySegmentEntity): Long

    @Query(
        "SELECT * FROM story_segments " +
                "WHERE story_id = :storyId " +
                "ORDER BY created_at ASC"
    )
    fun watchStorySegmentsForStory(storyId: Long): Flow<List<StorySegmentEntity>>
}