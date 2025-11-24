package coolio.zoewong.traverse.util

import android.net.Uri
import androidx.core.net.toFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Paths

/**
 * Tests for `util/UriExtensions.kt` file.
 */
@RunWith(AndroidJUnit4::class)
class UriExtensionsTest {
    @Test
    fun isFileURL() {
        Assert.assertTrue(Uri.parse("file:///path/to/file").isFile) // Absolute path
        Assert.assertTrue(Uri.parse("file://path/to/file").isFile) // Relative path
        Assert.assertTrue(Uri.parse("file:/path/to/file").isFile) // Malformed URL
    }

    @Test
    fun toFilePath() {
        Assert.assertEquals(
            // Absolute path
            Paths.get("/path/to/file"),
            Uri.parse("file:///path/to/file").toFilePath(),
        )

        Assert.assertEquals( // Relative path
            Paths.get("path/to/file"),
            Uri.parse("file://path/to/file").toFilePath()
        )

        Assert.assertEquals( // Malformed URL
            Paths.get("/path/to/file"),
            Uri.parse("file:/path/to/file").toFilePath()
        )
    }

    @Test
    fun pathFileExtension() {
        Assert.assertEquals(
            // File extension
            "txt",
            Uri.parse("file:///path/to/file.txt").pathFileExtension,
        )
        Assert.assertEquals(
            // No file extension
            "",
            Uri.parse("file:///path/to/file").pathFileExtension,
        )
        Assert.assertEquals(
            // No subdir
            "txt",
            Uri.parse("file://file.txt").pathFileExtension,
        )
    }
}