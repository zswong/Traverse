package coolio.zoewong.traverse.service.storyanalysis

import android.util.Log
import coolio.zoewong.traverse.ai.InferenceModel
import coolio.zoewong.traverse.ai.truncatePromptToFit
import coolio.zoewong.traverse.database.MemoryEntity
import coolio.zoewong.traverse.database.MemoryType
import coolio.zoewong.traverse.database.StoryAnalysisEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/**
 * Analyzes a Story using a device-local large language model.
 *
 * Feeds a summary and some memories into the LLM to generate a new summary.
 * When the token limit is reached, repeats the process until all memories are analyzed.
 */
class StoryAnalyzer(
    val inferenceModel: InferenceModel,
    val lastAnalysis: StoryAnalysisEntity,
    val memories: List<MemoryEntity>,
) {
    private var summary = lastAnalysis.modelSummary
    private val start = when (lastAnalysis.lastAnalyzedMemoryId) {
        null -> 0
        else -> memories.indexOfFirst { it.id == lastAnalysis.lastAnalyzedMemoryId + 1 }
    }

    private var currentPrompt = DEFAULT_PROMPT
    private var memoriesInPrompt = 0

    /**
     * Analyzes the Story memories in chunks.
     */
    suspend fun run(): StoryAnalysisEntity {
        val defaultPromptTokens = inferenceModel.countTokens(DEFAULT_PROMPT)
        if (defaultPromptTokens > inferenceModel.maxTokens) {
            throw RuntimeException("Default prompt too long at ${defaultPromptTokens} tokens")
        }

        Log.d(LOG_TAG, "Analyzing story with ${memories.size} memories.")

        // Add the last summary to the prompt.
        addSummaryToPrompt()
        if (isCurrentPromptTooLong()) {
            Log.w(LOG_TAG, "Prompt with summary is too long. Must truncate.")
            currentPrompt = inferenceModel.truncatePromptToFit(currentPrompt)
            analyzeChunk()
        }

        // Add remaining memories to the prompt.
        for (i in start until memories.size) {
            if (!currentCoroutineContext().isActive) {
                throw CancellationException()
            }

            val memory = memories[i]
            val addedChunk = addMemoryToPrompt(memory)
            if (addedChunk) {
                continue
            }

            // Add it again with a fresh prompt. If it's still too long, just truncate it.
            analyzeChunk()
            addMemoryToPrompt(memory, truncate = true)
        }

        if (memoriesInPrompt > 0) {
            analyzeChunk()
        }

        // Condense the summary.
        Log.i(LOG_TAG, "Condensing summary:\n\n$summary")
        val condenserPrompt = "$CONDENSER_PROMPT\nThe summary:\n\n$summary"
        val condensedSummary = inferenceModel.generate(
            inferenceModel.truncatePromptToFit(condenserPrompt)
        )

        return lastAnalysis.copy(
            summary = condensedSummary,
            modelSummary = summary,
            lastAnalyzedMemoryId = memories.last().id,
        )
    }

    /**
     * Analyzes the current chunk of memories.
     */
    suspend fun analyzeChunk() {
        val tokens = inferenceModel.countTokens(currentPrompt)
        Log.d(LOG_TAG, "Analyzing chunk. Prompt ($tokens tokens):\n\n$currentPrompt")
        summary = inferenceModel.generate(currentPrompt)
        currentPrompt = DEFAULT_PROMPT
        memoriesInPrompt = 0
        addSummaryToPrompt()
    }

    /**
     * Adds the current summary to the prompt.
     */
    private fun addSummaryToPrompt() {
        if (summary != "") {
            currentPrompt += "Previously, you created this summary for earlier entries. " +
                    "Pretend it's a previous journal entry, and weigh it with more importance than the upcoming ones. " +
                    "The Summary:\n${summary}"
        }
    }

    /**
     * Adds a memory to the prompt.
     *
     * Returns true if the memory was added, false if it was not.
     */
    private fun addMemoryToPrompt(memory: MemoryEntity, truncate: Boolean = false): Boolean {
        if (memory.type != MemoryType.TEXT) {
            return true // We only support text memories for now.
        }

        val delimiter = when (memoriesInPrompt > 0) {
            false -> "\n\nJournal Entries:\n\n"
            true -> "\n\n"
        }

        val newPrompt = currentPrompt + delimiter + memory.contents
        if (inferenceModel.isPromptTooLarge(newPrompt)) {
            if (!truncate) {
                Log.d(LOG_TAG, "Prompt reached limit. Must analyze chunk before adding more memories.")
                return false
            }

            Log.w(LOG_TAG, "Memory content is too long. Must truncate.")
            currentPrompt = inferenceModel.truncatePromptToFit(newPrompt)
            memoriesInPrompt++
            return true
        }

        currentPrompt = newPrompt
        memoriesInPrompt++
        return true
    }

    /**
     * Returns true if the current prompt is too long for the model.
     */
    private fun isCurrentPromptTooLong(): Boolean {
        return inferenceModel.countTokens(currentPrompt) > inferenceModel.maxTokens
    }

    companion object {
        private const val LOG_TAG = "StoryAnalyzer"
        private const val DEFAULT_PROMPT = "" +
                "You are a helpful assistant that accurately summarizes my journals." +
                "\n" +
                "Task: Summarize the journal entries. " +
                "Unless explicitly written, keep your thoughts to yourself. " +
                "If the journal entries are written in first-person, write the summary in first-person." +
                "\n" +
                "Constraints: Do NOT print anything other than the summary. " +
                "DO NOT print any introductory text. " +
                "This is NOT a creative writing story. You are not Shakespeare. " +
                "\n"

        private const val CONDENSER_PROMPT = "" +
                "You are a helpful assistant that simplifies my writing." +
                "\n" +
                "Task: You should condense my writing into a few sentences." +
                "If it's written in first-person, keep it in first-person." +
                "\n" +
                "Constraints: Do NOT print anything other than the simplified paragraph. " +
                "DO NOT print any introductory text. DO NOT tell me what you will do or what you have done. " +
                "This is NOT a creative writing story. You are not Shakespeare. " +
                "\n"
    }

}