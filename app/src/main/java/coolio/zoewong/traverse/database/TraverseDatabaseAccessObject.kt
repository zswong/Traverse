package coolio.zoewong.traverse.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

/**
 * Database access object for the traverse database.
 */
@Dao
interface TraverseDatabaseAccessObject {

    /**
     * Gets the MemoryEntity for the given ID.
     */
    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemory(id: Long): MemoryEntity?

    /**
     * Gets all MemoryEntity instances between the provided timestamp range (inclusive).
     */
    @Query("SELECT * FROM memories WHERE timestamp >= :from AND timestamp <= :to")
    suspend fun getMemoriesBetween(from: Long, to: Long): List<MemoryEntity>
    
    /**
     * Gets the previous N (count) MemoryEntity instances to the given id.
     */
    @Query("SELECT * FROM memories WHERE id < :id ORDER BY id DESC LIMIT :count")
    suspend fun getPreviousMemories(id: Long, count: Long): List<MemoryEntity>

    /**
     * Gets the previous N (count) MemoryEntity instances to the given id.
     */
    @Query("SELECT * FROM memories WHERE id > :id ORDER BY id ASC LIMIT :count")
    suspend fun getNextMemories(id: Long, count: Long): List<MemoryEntity>

    /**
     * Inserts a MemoryEntity into the database, returning its new ID.
     *
     * The ID of the provided memory MUST be 0.
     */
    @Insert
    suspend fun insertMemory(memory: MemoryEntity): Long

    /**
     * Deletes the MemoryEntity entry with the given ID from the database.
     */
    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    /**
     * Returns the number of MemoryEntities in the database.
     */
    @Query("SELECT COUNT(*) FROM memories")
    suspend fun countMemories(): Long

    /**
     * Returns a flow emitting the timestamp of the oldest MemoryEntity in the database.
     * This can be used to get the oldest memory in the database.
     */
    @Query("SELECT timestamp FROM memories ORDER BY timestamp ASC LIMIT 1")
    fun watchOldestMemoryTimestamp(): Flow<Long>

    /**
     * Returns a flow emitting the timestamp of the newest memory in the database.
     * This can be used to track if a new memory was added.
     */
    @Query("SELECT timestamp FROM memories ORDER BY timestamp DESC LIMIT 1")
    fun watchNewestMemoryTimestamp(): Flow<Long>

    /**
     * Returns a flow emitting MemoryEntity instances from the database that were created
     * since the provided timestamp (inclusive).
     */
    @Query("SELECT * FROM memories WHERE timestamp >= :timestamp")
    fun watchMemoriesSince(timestamp: Long): Flow<List<MemoryEntity>>

    /**
     * Returns a flow emitting all MemoryEntity instances from the database.
     *
     * Note: This will return EVERYTHING.
     *
     * Prefer only fetching what is needed by using getMemoriesBetween or watchMemoriesSince.
     */
    @Query("SELECT * FROM memories")
    fun watchMemories(): Flow<List<MemoryEntity>>

}