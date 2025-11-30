package coolio.zoewong.traverse.ai

/**
 * Truncates the prompt to fit within the model's token limit.
 */
fun InferenceModel.truncatePromptToFit(prompt: String, maxTokens: Int = this.maxTokens): String {
    if (countTokens(prompt) < maxTokens) {
        return prompt
    }

    // Binary search to find the last word that fits within the limit.
    val words = prompt.split(" ")
    var iMin = 0
    var iMax = words.size

    while (true) {
        val mid = (iMin + iMax) / 2
        if (mid == iMin) { // Division truncates, eventually mid == iMin.
            return words.subList(0, iMin).joinToString(" ")
        }

        val newPrompt = words.subList(0, mid).joinToString(" ")
        if (countTokens(newPrompt) >= maxTokens) {
            iMax = mid
        } else {
            iMin = mid
        }
    }
}
