package coolio.zoewong.traverse.service.storyanalysis

/**
 * Events emitted by the StoryAnalysisService to inform the service manager of changes.
 */
sealed interface StoryAnalysisEvent {

    /**
     * The model failed to initialize.
     */
    data class ModelInitializationFailed(
        val reason: Throwable
    ) : StoryAnalysisEvent

    /**
     * The service has started analyzing a story.
     */
    data class StoryAnalysisStarted(
        val storyId: Long
    ) : StoryAnalysisEvent

    /**
     * The service has finished analyzing a story.
     */
    data class StoryAnalysisFinished(
        val storyId: Long
    ) : StoryAnalysisEvent

    /**
     * The service failed to analyze a story.
     */
    data class StoryAnalysisFailed(
        val storyId: Long,
        var reason: Throwable,
    ) : StoryAnalysisEvent

    /**
     * The service has cancelled analyzing a story.
     */
    data class StoryAnalysisCancelled(
        val storyId: Long,
    ) : StoryAnalysisEvent

}
