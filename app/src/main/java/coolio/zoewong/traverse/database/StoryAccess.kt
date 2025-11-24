package coolio.zoewong.traverse.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

import kotlinx.coroutines.flow.Flow

/**
 * Database access object for the traverse database.
 */
@Dao
interface StoryAccess {

    /**
     * Inserts a StoryEntity into the database, returning its new ID.
     *
     * The ID of the provided memory MUST be 0.
     */
    @Insert
    suspend fun insert(story: StoryEntity): Long

    /**
     * Updates a StoryEntity.
     */
    @Update
    suspend fun update(story: StoryEntity)

    /**
     * Deletes the StoryEntity with the given ID from the database.
     *
     * The deletion cascades and removes the StoryMemoryAssociation entities as well.
     */
    @Query("DELETE FROM stories WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Returns a flow emitting all StoryEntity instances from the database.
     */
    @Query("SELECT * FROM stories")
    fun watchAll(): Flow<List<StoryEntity>>

    /**
     * Returns the number of StoryEntity instances in the database.
     */
    @Query("SELECT COUNT(*) FROM stories")
    suspend fun count(): Long

}