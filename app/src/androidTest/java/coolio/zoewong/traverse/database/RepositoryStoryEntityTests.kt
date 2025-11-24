package coolio.zoewong.traverse.database

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class RepositoryStoryEntityTests {
    /** Fixed time to use for timestamp instead of actual time. */
    val now = 1762239623283

    @Rule
    @JvmField
    var tempdir: TemporaryFolder = TemporaryFolder()

    /**
     * Tests: stories.insert(StoryEntity)
     */
    @Test
    fun insertingStories() = runTest {
        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.stories.count())

            repo.stories.insert(
                StoryEntity(
                    title = "My Story",
                    coverUri = Uri.parse("https://example.com"),
                    location = LatLng(1.0, -1.0),
                )
            )
            assertEquals(1, db.stories.count())

            repo.stories.insert(
                StoryEntity(
                    title = "My Story 2",
                    coverUri = Uri.parse("https://example2.com"),
                    location = LatLng(2.0, -2.0),
                )
            )
            assertEquals(2, db.stories.count())
        }
    }

    /**
     * Tests: stories.delete(Long)
     */
    @Test
    fun deletingStoriesByID() = runTest {
        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.stories.count())

            val story = repo.stories.insert(
                StoryEntity(
                    title = "My Story",
                    coverUri = Uri.parse("https://example.com"),
                    location = LatLng(1.0, -1.0),
                )
            )

            repo.stories.delete(story.id)
            assertEquals(0, db.stories.count())
        }
    }

    /**
     * Tests: stories.delete(StoryEntity)
     */
    @Test
    fun deletingStoriesByObject() = runTest {
        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.stories.count())

            val story = repo.stories.insert(
                StoryEntity(
                    title = "My Story",
                    coverUri = Uri.parse("https://example.com"),
                    location = LatLng(1.0, -1.0),
                )
            )

            repo.stories.delete(story)
            assertEquals(0, db.stories.count())
        }
    }

    /**
     * Tests:
     *  - stories.addMemory(StoryEntity, MemoryEntity)
     *  - stories.watchMemoriesOf(StoryEntity)
     */
    @Test
    fun addingMemoriesToStories() = runTest {
        val memories = listOf(
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 1000, contents = "message 1"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 2000, contents = "message 2"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 3000, contents = "message 3"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 4000, contents = "message 4"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 5000, contents = "message 5"),
        )
        val stories = listOf(
            StoryEntity(title = "Story 1", coverUri = null, location = null),
            StoryEntity(title = "Story 2", coverUri = null, location = null),
        )
        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.stories.count())
            assertEquals("sanity check", 0, db.memories.count())
            assertEquals("sanity check", 0, db.assocStoriesMemories.count())

            // Insert memories and stories.
            val insertedMemories = memories.map { repo.memories.insert(it) }
            val insertedStories = stories.map { repo.stories.insert(it) }

            // Associate memories with stories.
            repo.stories.apply {
                // Story 1 -> Memory [1, 2, 3]
                addMemory(insertedStories[0], insertedMemories[0])
                addMemory(insertedStories[0], insertedMemories[1])
                addMemory(insertedStories[0], insertedMemories[2])

                // Story 2 -> Memory [3, 4]
                addMemory(insertedStories[1], insertedMemories[2])
                addMemory(insertedStories[1], insertedMemories[3])
            }

            // Check that the associations were inserted correctly.
            assertEquals(
                listOf(
                    insertedMemories[0],
                    insertedMemories[1],
                    insertedMemories[2],
                ),
                repo.stories
                    .watchMemoriesOf(insertedStories[0])
                    .first()
            )

            assertEquals(
                listOf(
                    insertedMemories[2],
                    insertedMemories[3],
                ),
                repo.stories
                    .watchMemoriesOf(insertedStories[1])
                    .first()
            )
        }
    }

    /**
     * Tests:
     *  - stories.removeMemories(StoryEntity, MemoryEntity)
     */
    @Test
    fun removingMemoriesFromStories() = runTest {
        val memories = listOf(
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 1000, contents = "message 1"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 2000, contents = "message 2"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 3000, contents = "message 3"),
        )
        val stories = listOf(
            StoryEntity(title = "Story 1", coverUri = null, location = null),
        )
        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.stories.count())
            assertEquals("sanity check", 0, db.memories.count())
            assertEquals("sanity check", 0, db.assocStoriesMemories.count())

            // Insert memories and stories.
            val insertedMemories = memories.map { repo.memories.insert(it) }
            val insertedStories = stories.map { repo.stories.insert(it) }

            // Associate memories with stories.
            repo.stories.apply {
                // Story 1 -> Memory [1, 2, 3]
                addMemory(insertedStories[0], insertedMemories[0])
                addMemory(insertedStories[0], insertedMemories[1])
                addMemory(insertedStories[0], insertedMemories[2])
            }

            // Remove Memory 1 from Story 1 and check that it worked.
            repo.stories.removeMemory(insertedStories[0], insertedMemories[0])
            assertEquals(
                listOf(
                    insertedMemories[1],
                    insertedMemories[2],
                ),
                repo.stories
                    .watchMemoriesOf(insertedStories[0])
                    .first()
            )
        }
    }

    /**
     * Tests:
     *  - memories.delete(MemoryEntity)
     */
    @Test
    fun removingMemoriesFromStoriesAsMemoryDeleteCascade() = runTest {
        val memories = listOf(
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 1000, contents = "message 1"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 2000, contents = "message 2"),
        )
        val stories = listOf(
            StoryEntity(title = "Story 1", coverUri = null, location = null),
        )
        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.stories.count())
            assertEquals("sanity check", 0, db.memories.count())
            assertEquals("sanity check", 0, db.assocStoriesMemories.count())

            // Insert memories and stories.
            val insertedMemories = memories.map { repo.memories.insert(it) }
            val insertedStories = stories.map { repo.stories.insert(it) }

            // Associate memories with stories.
            repo.stories.apply {
                // Story 1 -> Memory [1, 2, 3]
                addMemory(insertedStories[0], insertedMemories[0])
                addMemory(insertedStories[0], insertedMemories[1])
            }

            // Delete Memory 1 and check that Story 1 no longer contains it.
            repo.memories.delete(insertedMemories[0])
            assertEquals(
                listOf(
                    insertedMemories[1],
                ),
                repo.stories
                    .watchMemoriesOf(insertedStories[0])
                    .first()
            )
        }
    }

    /**
     * Tests:
     *  - stories.delete(StoryEntity)
     */
    @Test
    fun removingMemoriesFromStoriesAsStoryDeleteCascade() = runTest {
        val memories = listOf(
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 1000, contents = "message 1"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 2000, contents = "message 2"),
        )
        val stories = listOf(
            StoryEntity(title = "Story 1", coverUri = null, location = null),
        )
        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.stories.count())
            assertEquals("sanity check", 0, db.memories.count())
            assertEquals("sanity check", 0, db.assocStoriesMemories.count())

            // Insert memories and stories.
            val insertedMemories = memories.map { repo.memories.insert(it) }
            val insertedStories = stories.map { repo.stories.insert(it) }

            // Associate memories with stories.
            repo.stories.apply {
                // Story 1 -> Memory [1, 2, 3]
                addMemory(insertedStories[0], insertedMemories[0])
                addMemory(insertedStories[0], insertedMemories[1])
            }

            // Delete Story 1 and check that the database no longer has any associations.
            repo.stories.delete(insertedStories[0])
            assertEquals(0, db.assocStoriesMemories.count())
        }
    }
}