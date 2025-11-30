package coolio.zoewong.traverse.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

import kotlinx.coroutines.flow.Flow

/**
 * Database access object for the traverse database.
 */
@Dao
interface StoryAnalysisAccess {

    /**
     * Gets the StoryAnalysisEntity for the given StoryEntity ID.
     */
    @Query("SELECT * FROM story_analysis WHERE story_id = :id")
    suspend fun get(id: Long): StoryAnalysisEntity?

    /**
     * Watches the StoryAnalysisEntity for changes.
     */
    @Query("SELECT * FROM story_analysis WHERE story_id = :id")
    fun watch(id: Long): Flow<StoryAnalysisEntity>

    /**
     * Inserts a StoryAnalysisEntity into the database, returning its ID.
     */
    @Insert
    suspend fun insert(analysis: StoryAnalysisEntity): Long

    /**
     * Updates a StoryAnalysisEntity.
     */
    @Update
    suspend fun update(analysis: StoryAnalysisEntity)

    /**
     * Deletes the StoryAnalysisEntity with the given ID from the database.
     */
    @Query("DELETE FROM story_analysis WHERE story_id = :id")
    suspend fun delete(id: Long)

    /**
     * Clears the data of the StoryAnalysisEntity for the given story.
     */
    @Query("UPDATE story_analysis SET " +
            "last_analyzed_memory_id = NULL, " +
            "summary = NULL, " +
            "model_summary = '' " +
            "WHERE story_id = :id")
    fun clearAnalysisData(id: Long)

    /**
     * Updates the StoryAnalysis to consider new memories being added to the story.
     */
    @Transaction
    suspend fun updateToIncludeMemories(storyId: Long, memoryIds: List<Long>) {
        val analysis = get(storyId) ?: return
        val lowestMemoryId = memoryIds.minOrNull() ?: return
        val highestMemoryId = memoryIds.maxOrNull() ?: return

        // Are any of the memories part of what was already analyzed?
        // If so, the summary will needs to be fully re-generated.
        if (lowestMemoryId < (analysis.lastAnalyzedMemoryId ?: 0)) {
            update(analysis.copy(
                latestMemoryId = null,
                lastAnalyzedMemoryId = null,
            ))
            return
        }

        // Added a memory after the last one. Progressive analysis is possible.
        update(analysis.copy(latestMemoryId = highestMemoryId))
    }

}
