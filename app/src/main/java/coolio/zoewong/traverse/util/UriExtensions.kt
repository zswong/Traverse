package coolio.zoewong.traverse.util

import android.net.Uri
import androidx.core.net.toFile
import kotlin.io.path.extension
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Returns the file extension of the Uri's path.
 */
val Uri.pathFileExtension
    get(): String {
        val path = when (isFile) {
            true -> this.toFilePath()
            false -> Paths.get(this.path)
        }

        return path.extension
    }

/**
 * Returns true if this URL is a file URL, false otherwise.
 */
val Uri.isFile
    get(): Boolean {
        return this.scheme == "file"
    }

/**
 * When the URL is a file URL, returns the file path.
 * Throws IllegalArgumentException if not a file URL.
 */
fun Uri.toFilePath(): Path {
    if (!isFile) {
        throw IllegalArgumentException("URL is not a file URL")
    }

    return Paths.get(
        when {
            host == null -> path
            host == "" -> path
            host != "" -> "${host}/${path}"

            // ???
            else -> throw IllegalArgumentException("Not able to determine file path of $this")
        }
    )
}
