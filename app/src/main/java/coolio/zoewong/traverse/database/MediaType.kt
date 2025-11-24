package coolio.zoewong.traverse.database

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.URL
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Path

/**
 * An enum of supported media types.
 */
enum class MediaType(
    val extension: String,
    val mime: String,
    val alternateMimes: List<String> = listOf(),
) {

    // Images:
    IMAGE_PNG("png", "image/png"),
    IMAGE_JPEG("jpg", "image/jpeg", alternateMimes = listOf("image/jpg")),
    IMAGE_GIF("gif", "image/gif"),
    IMAGE_WEBP("webp", "image/webp"),
    IMAGE_HEIF("heif", "image/heif", alternateMimes = listOf("image/heic")),

    // Unknown:
    UNKNOWN("", "");

    companion object {

        /**
         * Guess the media type of a file by its name.
         */
        fun byFileName(path: Path): MediaType {
            return byFileName(path.toUri().toURL())
        }

        /**
         * Guess the media type of a file by its name.
         */
        fun byFileName(url: URL): MediaType {
            // https://stackoverflow.com/a/847849
            val guess = URLConnection.guessContentTypeFromName(url.file)
            if (guess != null) {
                return MediaType[guess]
            }

            Log.w("MediaType", "Could not detect media type for $url")
            return UNKNOWN
        }

        /**
         * Guess the media type of a file by its contents.
         */
        suspend fun byFileContents(path: Path): MediaType {
            return withContext(Dispatchers.IO) {
                // Source: https://stackoverflow.com/a/8973468
                val guess = Files.probeContentType(path)
                if (guess != null) {
                    return@withContext MediaType[guess]
                }

                // Source: https://stackoverflow.com/a/847849
                val guess2 = BufferedInputStream(Files.newInputStream(path)).use { stream ->
                    URLConnection.guessContentTypeFromStream(stream)
                }
                if (guess2 != null) {
                    return@withContext MediaType[guess2]
                }

                // Try by name.
                byFileName(path)
            }
        }

        operator fun get(mimeType: String): MediaType {
            val mediaType = MediaType.entries.find { it.mime == mimeType }
            if (mediaType != null) {
                return mediaType
            }

            // Maybe it's an alternate mime type.
            val mediaType2 = MediaType.entries.find { it.alternateMimes.contains(mimeType) }
            if (mediaType2 != null) {
                return mediaType2
            }

            // Unknown or unsupported mime type.
            Log.w("MediaType", "No enum constant for mime type $mimeType")
            return UNKNOWN
        }
    }

}