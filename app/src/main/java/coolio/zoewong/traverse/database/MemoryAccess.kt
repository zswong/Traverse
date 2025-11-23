package coolio.zoewong.traverse.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

/**
 * Database access object for the traverse database.
 */
@Dao
interface MemoryAccess {

    /**
     * Gets the MemoryEntity for the given ID.
     */
    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun get(id: Long): MemoryEntity?

    /**
     * Gets all MemoryEntity instances between the provided timestamp range (inclusive).
     */
    @Query("SELECT * FROM memories WHERE timestamp >= :from AND timestamp <= :to")
    suspend fun getBetween(from: Long, to: Long): List<MemoryEntity>

    /**
     * Gets the previous N (count) MemoryEntity instances to the given id.
     */
    @Query("SELECT * FROM memories WHERE id < :id ORDER BY id DESC LIMIT :count")
    suspend fun getPrevious(id: Long, count: Long): List<MemoryEntity>

    /**
     * Gets the previous N (count) MemoryEntity instances to the given id.
     */
    @Query("SELECT * FROM memories WHERE id > :id ORDER BY id ASC LIMIT :count")
    suspend fun getNext(id: Long, count: Long): List<MemoryEntity>

    /**
     * Inserts a MemoryEntity into the database, returning its new ID.
     *
     * The ID of the provided memory MUST be 0.
     */
    @Insert
    suspend fun insert(memory: MemoryEntity): Long

    /**
     * Deletes the MemoryEntity entry with the given ID from the database.
     *
     * The deletion cascades and removes its from the stories as well.
     */
    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Returns the number of MemoryEntities in the database.
     */
    @Query("SELECT COUNT(*) FROM memories")
    suspend fun count(): Long

    /**
     * Returns a flow emitting the timestamp of the oldest MemoryEntity in the database.
     * This can be used to get the oldest memory in the database.
     */
    @Query("SELECT timestamp FROM memories ORDER BY timestamp ASC LIMIT 1")
    fun watchOldestTimestamp(): Flow<Long>

    /**
     * Returns a flow emitting the timestamp of the newest memory in the database.
     * This can be used to track if a new memory was added.
     */
    @Query("SELECT timestamp FROM memories ORDER BY timestamp DESC LIMIT 1")
    fun watchNewestTimestamp(): Flow<Long>

    /**
     * Returns a flow emitting MemoryEntity instances from the database that were created
     * since the provided timestamp (inclusive).
     */
    @Query("SELECT * FROM memories WHERE timestamp >= :timestamp")
    fun watchSince(timestamp: Long): Flow<List<MemoryEntity>>

    /**
     * Returns a flow emitting all MemoryEntity instances from the database.
     *
     * Note: This will return EVERYTHING.
     *
     * Prefer only fetching what is needed by using getMemoriesBetween or watchMemoriesSince.
     */
    @Query("SELECT * FROM memories")
    fun watchAll(): Flow<List<MemoryEntity>>

}