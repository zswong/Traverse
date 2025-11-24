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

    /**
     * Create, read, and modify memories.
     */
    val memories = MemoriesRepo(
        database.memories
    )

    /**
     * Create, read, and modify stories.
     */
    val stories = StoriesRepo(
        database.stories,
        database.assocStoriesMemories,
    )

    /**
     * The media store for Traverse.
     */
    val media = mediaStore

    /**
     * Provides access to memories.
     */
    class MemoriesRepo(
        private val memories: MemoryAccess,
    ) {

        /**
         * Gets the MemoryEntity for the given ID.
         */
        suspend fun get(id: Long): MemoryEntity? {
            return withContext(IO) {
                memories.get(id)
            }
        }

        /**
         * Gets all MemoryEntity instances between the provided timestamp range (inclusive).
         */
        suspend fun getBetween(from: Long, to: Long): List<MemoryEntity> {
            return withContext(IO) {
                memories.getBetween(from, to)
            }
        }

        /**
         * Gets the previous N (count) MemoryEntity instances to the given id.
         */
        suspend fun getPrevious(relativeTo: MemoryEntity, count: Long): List<MemoryEntity> {
            return withContext(IO) {
                memories.getPrevious(relativeTo.id, count)
            }
        }

        /**
         * Gets the next N (count) MemoryEntity instances to the given id.
         */
        suspend fun getNext(relativeTo: MemoryEntity, count: Long): List<MemoryEntity> {
            return withContext(IO) {
                memories.getNext(relativeTo.id, count)
            }
        }

        /**
         * Inserts a MemoryEntity into the database, returning a copy of the
         * MemoryEntity with its new ID.
         */
        suspend fun insert(memory: MemoryEntity): MemoryEntity {
            return withContext(IO) {
                val id = memories.insert(memory)
                memory.copy(id = id)
            }
        }

        /**
         * Deletes the MemoryEntity entry with the given ID from the database.
         *
         * It will also be removed from any stories it was a part of.
         */
        suspend fun delete(id: Long) {
            return withContext(IO) {
                memories.delete(id)
            }
        }

        /**
         * Deletes the given MemoryEntity from the database.
         *
         * It will also be removed from any stories it was a part of.
         */
        suspend fun delete(memory: MemoryEntity) {
            delete(memory.id)
        }

        /**
         * Returns a flow emitting MemoryEntity instances from the database that were created
         * after the provided timestamp (inclusive).
         */
        suspend fun watchSince(timestamp: Long): Flow<List<MemoryEntity>> {
            return memories.watchSince(timestamp)
                .flowOn(IO)
        }

        /**
         * Returns a flow emitting all MemoryEntity instances from the database.
         *
         * Note: This will return EVERYTHING.
         *
         * Prefer only fetching what is needed by using getMemoriesBetween or watchMemoriesSince.
         */
        suspend fun watchAll(): Flow<List<MemoryEntity>> {
            return memories.watchAll()
                .flowOn(IO)
        }

        /**
         * Returns a flow emitting the timestamp of the oldest MemoryEntity in the database.
         * This can be used to get the oldest memory in the database.
         */
        suspend fun watchOldestTimestamp(): Flow<Long> {
            return memories.watchOldestTimestamp()
                .flowOn(IO)
        }

        /**
         * Returns a flow emitting the timestamp of the newest memory in the database.
         * This can be used to track if a new memory was added.
         */
        suspend fun watchNewestTimestamp(): Flow<Long> {
            return memories.watchNewestTimestamp()
                .flowOn(IO)
        }
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

    /**
     * Provides access to stories.
     */
    class StoriesRepo(
        private val stories: StoryAccess,
        private val assocWithMemories: StoryMemoryAssociationAccess,
    ) {

        /**
         * Inserts a StoryEntity into the database, returning a copy of the
         * StoryEntity with its new ID.
         */
        suspend fun insert(story: StoryEntity): StoryEntity {
            return withContext(IO) {
                val id = stories.insert(story)
                story.copy(id = id)
            }
        }

        /**
         * Updates a StoryEntity in the database.
         */
        suspend fun update(story: StoryEntity) {
            return withContext(IO) {
                stories.update(story)
            }
        }

        /**
         * Deletes the StoryEntity entry with the given ID from the database.
         */
        suspend fun delete(id: Long) {
            return withContext(IO) {
                stories.delete(id)
            }
        }

        /**
         * Deletes the given MemoryEntity from the database.
         */
        suspend fun delete(story: StoryEntity) {
            delete(story.id)
        }

        /**
         * Returns a flow emitting a list of all MemoryEntity instances that
         * are part of the specified story.
         */
        suspend fun watchMemoriesOf(story: StoryEntity): Flow<List<MemoryEntity>> {
            return assocWithMemories.watchMemoriesByStory(story.id)
        }

        /**
         * Associates a MemoryEntity with a StoryEntity, "adding" the
         * memory to the story.
         *
         * Throws if:
         *  - The memory is already in the story.
         *  - The story doesn't exist.
         *  - The memory doesn't exist.
         */
        suspend fun addMemory(story: StoryEntity, memory: MemoryEntity) {
            return withContext(IO) {
                assocWithMemories.insert(
                    StoryMemoryAssociation(
                        storyId = story.id,
                        memoryId = memory.id,
                    )
                )
            }
        }

        /**
         * Removes an association between a MemoryEntity and StoryEntity, "removing" the
         * memory from the story.
         *
         * Does nothing if the memory is not already in the story.
         */
        suspend fun removeMemory(story: StoryEntity, memory: MemoryEntity) {
            return withContext(IO) {
                assocWithMemories.delete(
                    storyId = story.id,
                    memoryId = memory.id,
                )
            }
        }
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
