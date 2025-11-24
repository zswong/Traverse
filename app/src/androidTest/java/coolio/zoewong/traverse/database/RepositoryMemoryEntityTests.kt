package coolio.zoewong.traverse.database

import androidx.test.ext.junit.runners.AndroidJUnit4
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
class RepositoryMemoryEntityTests {
    /** Fixed time to use for timestamp instead of actual time. */
    val now = 1762239623283

    @Rule
    @JvmField
    var tempdir: TemporaryFolder = TemporaryFolder()

    /**
     * Tests: memories.insert(MemoryEntity)
     */
    @Test
    fun insertingMemories() = runTest {
        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.memories.count())

            repo.memories.insert(
                MemoryEntity(
                    type = MemoryType.TEXT,
                    timestamp = now,
                    contents = "message 1"
                )
            )
            assertEquals(1, db.memories.count())

            repo.memories.insert(
                MemoryEntity(
                    type = MemoryType.TEXT,
                    timestamp = now + 1000,
                    contents = "message 2"
                )
            )
            assertEquals(2, db.memories.count())
        }
    }

    /**
     * Tests: memories.delete(Long)
     */
    @Test
    fun deletingMemoriesByID() = runTest {
        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.memories.count())

            val memory = repo.memories.insert(
                MemoryEntity(
                    type = MemoryType.TEXT,
                    timestamp = now,
                    contents = "message 1"
                )
            )

            repo.memories.delete(memory.id)
            assertEquals(0, db.memories.count())
        }
    }

    /**
     * Tests: memories.delete(MemoryEntity)
     */
    @Test
    fun deletingMemoriesByObject() = runTest {
        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.memories.count())

            val memory = repo.memories.insert(
                MemoryEntity(
                    type = MemoryType.TEXT,
                    timestamp = now,
                    contents = "message 1"
                )
            )

            repo.memories.delete(memory)
            assertEquals(0, db.memories.count())
        }
    }

    /**
     * Tests: memories.getBetween(Long, Long)
     */
    @Test
    fun gettingMemoriesBetween() = runTest {
        val memories = listOf(
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 1000, contents = "message 1"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 2000, contents = "message 2"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 3000, contents = "message 3"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 4000, contents = "message 4"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 5000, contents = "message 5"),
        )

        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.memories.count())

            memories.forEach { repo.memories.insert(it) }
            val actual = repo.memories.getBetween(now + 2000, now + 4000)
            val expected = memories.slice(1..3)
            assertEquals(expected.messages(), actual.messages())
        }
    }

    /**
     * Tests: memories.getPrevious(MemoryEntity, Long)
     */
    @Test
    fun gettingPreviousMemories() = runTest {
        val memories = listOf(
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 1000, contents = "message 1"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 2000, contents = "message 2"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 3000, contents = "message 3"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 4000, contents = "message 4"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 5000, contents = "message 5"),
        )

        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.memories.count())

            memories.forEach { repo.memories.insert(it) }
            val relativeTo = repo.memories.get(3)!! // <-- not an index; starts at 1

            val actual = repo.memories.getPrevious(relativeTo, 1)
            val expected = memories.slice(1..1)
            assertEquals(expected.messages(), actual.messages())
        }
    }

    /**
     * Tests: memories.getNext(MemoryEntity, Long)
     */
    @Test
    fun gettingNextMemories() = runTest {
        val memories = listOf(
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 1000, contents = "message 1"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 2000, contents = "message 2"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 3000, contents = "message 3"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 4000, contents = "message 4"),
            MemoryEntity(type = MemoryType.TEXT, timestamp = now + 5000, contents = "message 5"),
        )

        withTemporaryDatabase(tempdir) { repo, db ->
            assertEquals("sanity check", 0, db.memories.count())

            memories.forEach { repo.memories.insert(it) }
            val relativeTo = repo.memories.get(3)!! // <-- not an index; starts at 1

            val actual = repo.memories.getNext(relativeTo, 1)
            val expected = memories.slice(3..3)
            assertEquals(expected.messages(), actual.messages())
        }
    }
}