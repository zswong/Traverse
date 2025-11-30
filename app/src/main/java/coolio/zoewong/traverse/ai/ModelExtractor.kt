package coolio.zoewong.traverse.ai

import android.content.Context
import android.util.Log
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import java.nio.file.Path

/**
 * Extracts models from the APK and saves them to the device filesystem.
 */
class ModelExtractor(private val context: Context) {
    private val ASSETS_VERSION = 1 // Increase if LLM model is changed

    private val modelsStoreDir = context.noBackupFilesDir.toPath()
        .resolve("models")

    val modelsDir = modelsStoreDir.resolve("v$ASSETS_VERSION")

    /**
     * Deletes outdated models.
     */
    @OptIn(ExperimentalPathApi::class)
    suspend fun removeOutdatedModels() {
        if (!modelsStoreDir.exists()) {
            return
        }

        modelsStoreDir.listDirectoryEntries().forEach {
            if (it != modelsDir) {
                Log.i(LOG_TAG, "Deleting outdated models: $it")
                it.deleteRecursively()
            }
        }
    }

    private suspend fun createDirs() {
        modelsDir.createDirectories()
        modelsDir.resolve(".nomedia").apply {
            if (!exists()) {
                createFile()
            }
        }
    }

    /**
     * Extracts an asset from the APK and saves it to the models directory.
     */
    suspend fun extract(assetName: String): Path {
        createDirs()

        val outputFile = modelsDir.resolve(assetName)
        if (outputFile.exists()) {
            return outputFile
        }

        try {
            Log.i(LOG_TAG, "Extracting asset: $assetName")
            context.assets.open(assetName).use { inputStream ->
                outputFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            return outputFile
        } catch (t: Throwable) {
            outputFile.deleteIfExists()
            throw t
        }
    }

    /**
     * Available models.
     */
    class Models private constructor() {
        companion object {
            const val GEMMA_1B = "gemma-3n-E1B-it-int4.task"
        }
    }

    companion object {
        private const val LOG_TAG = "ModelExtractor"
    }
}