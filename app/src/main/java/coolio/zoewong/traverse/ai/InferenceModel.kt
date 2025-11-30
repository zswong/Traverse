package coolio.zoewong.traverse.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.path.absolutePathString

/**
 * Inference model for story analysis.
 */
class InferenceModel(
    private val context: Context,
    val model: String = "gemma-3n-E1B-it-int4.task",
    val maxTokens: Int = 1000,
) : Model<String, String> {
    private val extractor = ModelExtractor(context)
    private lateinit var inference: LlmInference

    /**
     * Counts the number of tokens in a string.
     */
    fun countTokens(text: String): Int {
        // Seems to crash when close to but still within the token limit.
        // Pretend there are 25% more tokens than there actually are.
        return (inference.sizeInTokens(text).toDouble() * 1.25).toInt()
    }

    /**
     * Returns true if the prompt is too large for the model.
     */
    fun isPromptTooLarge(text: String): Boolean {
        return countTokens(text) >= maxTokens
    }

    override suspend fun initialize() {
        val modelFile = extractor.extract(model)
        val modelOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePathString())
            .setMaxTokens(maxTokens)
            .build()

        inference = LlmInference.createFromOptions(context, modelOptions)
    }

    override suspend fun generate(context: String): String {
        return withContext(Dispatchers.IO) {
            inference.generateResponse(context)
        }
    }
}