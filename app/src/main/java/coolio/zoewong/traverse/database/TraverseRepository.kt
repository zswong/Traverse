package coolio.zoewong.traverse.database

import android.content.Context
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Provides high-level access to the database in a thread-safe manner.
 *
 * Whenever a function returns a Flow, it will watch for changes to the database and re-emit
 * the updated contents. See https://developer.android.com/kotlin/flow#jetpack for details.
 *
 * All suspend functions and flow producers will be executed on an IO thread.
 */
class TraverseRepository(
    private val database: TraverseDatabase,
    private val mediaStore: TraverseMedia,
) {
    private val storySegments = database.storySegments
    private val memories = database.memories

    /**
     * The media store for Traverse.
     */
    val media = mediaStore

    /**
     * Gets the MemoryEntity for the given ID.
     */
    suspend fun getMemory(id: Long): MemoryEntity? {
        return withContext(IO) {
            memories.get(id)
        }
    }

    /**
     * Gets all MemoryEntity instances between the provided timestamp range (inclusive).
     */
    suspend fun getMemoriesBetween(from: Long, to: Long): List<MemoryEntity> {
        return withContext(IO) {
            memories.getBetween(from, to)
        }
    }

    /**
     * Gets the previous N (count) MemoryEntity instances to the given id.
     */
    suspend fun getPreviousMemories(relativeTo: MemoryEntity, count: Long): List<MemoryEntity> {
        return withContext(IO) {
            memories.getPrevious(relativeTo.id, count)
        }
    }

    /**
     * Gets the next N (count) MemoryEntity instances to the given id.
     */
    suspend fun getNextMemories(relativeTo: MemoryEntity, count: Long): List<MemoryEntity> {
        return withContext(IO) {
            memories.getNext(relativeTo.id, count)
        }
    }

    /**
     * Inserts a MemoryEntity into the database, returning a copy of the
     * MemoryEntity with its new ID.
     */
    suspend fun insertMemory(memory: MemoryEntity): MemoryEntity {
        return withContext(IO) {
            val id = memories.insert(memory)
            memory.copy(id = id)
        }
    }

    /**
     * Deletes the MemoryEntity entry with the given ID from the database.
     */
    suspend fun deleteMemory(id: Long) {
        return withContext(IO) {
            memories.delete(id)
        }
    }

    /**
     * Deletes the given MemoryEntity from the database.
     */
    suspend fun deleteMemory(memory: MemoryEntity) {
        deleteMemory(memory.id)
    }

    /**
     * Returns a flow emitting MemoryEntity instances from the database that were created
     * after the provided timestamp (inclusive).
     */
    suspend fun watchMemoriesSince(timestamp: Long): Flow<List<MemoryEntity>> {
        return database.memories.watchSince(timestamp)
            .flowOn(IO)
    }

    /**
     * Returns a flow emitting all MemoryEntity instances from the database.
     *
     * Note: This will return EVERYTHING.
     *
     * Prefer only fetching what is needed by using getMemoriesBetween or watchMemoriesSince.
     */
    suspend fun watchMemories(): Flow<List<MemoryEntity>> {
        return database.memories.watchAll()
            .flowOn(IO)
    }

    /**
     * Returns a flow emitting the timestamp of the oldest MemoryEntity in the database.
     * This can be used to get the oldest memory in the database.
     */
    suspend fun watchOldestMemoryTimestamp(): Flow<Long> {
        return database.memories.watchOldestTimestamp()
            .flowOn(IO)
    }

    /**
     * Returns a flow emitting the timestamp of the newest memory in the database.
     * This can be used to track if a new memory was added.
     */
    suspend fun watchNewestMemoryTimestamp(): Flow<Long> {
        return database.memories.watchNewestTimestamp()
            .flowOn(IO)
    }
    // -------- Story segments --------

    /**
     * Inserts a StorySegmentEntity into the database, returning a copy with its new ID.
     */
    suspend fun insertStorySegment(segment: StorySegmentEntity): StorySegmentEntity {
        return withContext(IO) {
            val id = storySegments.insertStorySegment(segment)
            segment.copy(id = id)
        }
    }

    /**
     * Watches all segments for a given story.
     */
    suspend fun watchStorySegments(storyId: Long): Flow<List<StorySegmentEntity>> {
        return storySegments.watchStorySegmentsForStory(storyId)
            .flowOn(IO)
    }

    companion object {

        /**
         * Singleton instance of the repository.
         */
        @Volatile
        private var instance: TraverseRepository? = null

        /**
         * Returns the TraverseRepository instance, creating a new one if necessary.
         */
        fun getInstance(context: Context): TraverseRepository {
            synchronized(this) {
                var instance = instance
                if (instance == null) {
                    instance = createInstance(context)
                    Companion.instance = instance
                }
                return instance
            }
        }

        private fun createInstance(context: Context): TraverseRepository {
            return TraverseRepository(
                database = TraverseDatabase.getInstance(context),
                mediaStore = TraverseMedia.createInstance(context.filesDir.toPath())
            )
        }

    }

}
