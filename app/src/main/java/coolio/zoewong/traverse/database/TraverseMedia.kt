package coolio.zoewong.traverse.database

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import coolio.zoewong.traverse.util.isFile
import coolio.zoewong.traverse.util.pathFileExtension
import coolio.zoewong.traverse.util.toFilePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.moveTo
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * Media file storage and management.
 */
class TraverseMedia private constructor(private val dir: Path) {
    private val imagesDir = dir.resolve("images")
    private val tempDir = dir.resolve("temp")

    /**
     * Returns a content URI that an external activity can write into.
     */
    fun uriForIntentResult(context: Context, prefix: String, extension: String = ""): Uri {
        val path = let {
            val randomName = UUID.randomUUID().toString()
            when (extension) {
                "" -> tempDir.resolve("${prefix}-${randomName}")
                else -> tempDir.resolve("${prefix}-${randomName}.${extension}")
            }
        }

        return FileProvider.getUriForFile(
            context,
            AUTHORITY,
            path.toFile()
        )
    }

    /**
     * Saves an image to the images directory.
     */
    suspend fun saveImage(context: Context, source: Uri): Uri {
        val (saved, type) = storeMedia(context, source, imagesDir)
        return Uri.fromFile(saved.toFile())
    }

    /**
     * Downloads or copies a media file to the target directory, storing it with a random name.
     * When complete, the path to the downloaded file and its MediaType is returned.
     *
     * If the media type is unknown or unsupported, throws IllegalArgumentException.
     */
    private suspend fun storeMedia(
        context: Context,
        source: Uri,
        destDir: Path
    ): Pair<Path, MediaType> {
        return withContext(Dispatchers.IO) {
            val (downloaded, type) = downloadMediaTemporarily(context, source)
            if (type == MediaType.UNKNOWN) {
                downloaded.deleteIfExists()
                throw IllegalArgumentException("Unsupported media type for $source")
            }

            // If the source is a URI result from this app, delete it.
            // Source: https://stackoverflow.com/a/53722802
            if (source.authority == AUTHORITY) {
                Log.d("TraverseMedia", "Media source is from uriForIntentResult()")
                withContext(Dispatchers.IO) {
                    DocumentFile.fromSingleUri(context, source)?.apply {
                        Log.d("TraverseMedia", "Deleting media source")
                        delete()
                    }
                }
            }

            // Move the downloaded file to the destination directory.
            val dest = destDir.resolve(downloaded.fileName)
            downloaded.moveTo(dest, true)
            return@withContext dest to type

        }
    }

    /**
     * Downloads a media file to the temp directory under a random name and detects its MediaType.
     *
     * If the URL is a file URL, it is copied instead of downloaded.
     */
    private suspend fun downloadMediaTemporarily(
        context: Context,
        source: Uri
    ): Pair<Path, MediaType> {
        val randomName = UUID.randomUUID().toString()
        val downloadName = when (source.pathFileExtension) {
            "" -> "${randomName}.download"
            else -> "${randomName}.download.${source.pathFileExtension}"
        }

        val downloadPath = tempDir.resolve(downloadName)
        return withContext(Dispatchers.IO) {
            try {
                // Download the media and detect its file type.
                val type = let {
                    when {
                        // file: URL
                        source.isFile -> Files.copy(
                            source.toFilePath(),
                            downloadPath,
                        )

                        // content: URL
                        source.scheme == "content" -> {
                            val contentResolver = context.contentResolver
                            Files.newOutputStream(downloadPath).use { destStream ->
                                contentResolver.openInputStream(source)!!.use { srcStream ->
                                    srcStream.copyTo(destStream)
                                }
                            }
                        }

                        // Unsupported URL type.
                        else -> throw IllegalArgumentException("Unsupported URL type: $source")
                    }

                    MediaType.byFileContents(downloadPath)
                }

                // Rename the file based on the media type.
                val renamedPath = downloadPath.resolveSibling("${randomName}.${type.extension}")
                downloadPath.moveTo(renamedPath, true)
                return@withContext renamedPath to type
            } catch (e: Exception) {
                // If the download failed, delete the temporary file.
                Files.deleteIfExists(downloadPath)
                throw e
            }
        }
    }

    companion object {
        // From manifest:
        private const val AUTHORITY = "coolio.zoewong.traverse.database.MediaStoreInternal";

        /**
         * Creates a new instance of the MediaStore, creating required directories.
         */
        internal fun createInstance(dir: Path): TraverseMedia {
            return TraverseMedia(dir).apply {
                imagesDir.createDirectories()
                tempDir.createDirectories()

                // Create a `.nomedia` file in the tempDir so Android doesn't index it.
                val tempDirNoMediaFile = tempDir.resolve(".nomedia")
                if (!Files.exists(tempDirNoMediaFile)) {
                    tempDirNoMediaFile.createFile()
                }
            }
        }
    }
}