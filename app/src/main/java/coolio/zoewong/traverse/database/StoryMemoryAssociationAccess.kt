package coolio.zoewong.traverse.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

/**
 * Database access object for the traverse database.
 */
@Dao
interface StoryMemoryAssociationAccess {

    /**
     * Inserts a StoryMemoryAssociation into the database.
     */
    @Insert
    suspend fun insert(story: StoryMemoryAssociation)

    /**
     * Deletes a StoryMemoryAssociation from the database.
     */
    @Query("DELETE FROM story_memory_associations WHERE story_id = :storyId AND memory_id = :memoryId")
    suspend fun delete(storyId: Long, memoryId: Long)

    /**
     * Returns a flow emitting all MemoryEntity instances from the database that
     * are associated with the specified StoryEntity ID.
     */
    @Query(
        "" +
                "SELECT * FROM story_memory_associations " +
                "LEFT JOIN memories " +
                "ON memories.id = story_memory_associations.memory_id " +
                "WHERE story_memory_associations.story_id = :storyId"
    )
    fun watchMemoriesByStory(storyId: Long): Flow<List<MemoryEntity>>

    /**
     * Returns the number of StoryMemoryAssociation in the database.
     */
    @Query("SELECT COUNT(*) FROM story_memory_associations")
    suspend fun count(): Long

}