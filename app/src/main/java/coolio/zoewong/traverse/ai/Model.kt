package coolio.zoewong.traverse.ai

interface Model<ContextType, ResultType> {

    /**
     * Initializes the model.
     */
    suspend fun initialize()

    /**
     * Generates a result from the model.
     */
    suspend fun generate(context: ContextType): ResultType

}