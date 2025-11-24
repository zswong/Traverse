package coolio.zoewong.traverse.database

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
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

}