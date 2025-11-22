package coolio.zoewong.traverse.ui.state

/**
 * Represents the current loading status of a state provider composable.
 */
enum class LoadStatus {

    /**
     * The state is still being loaded/built.
     *
     * If a fragment is meant to show data that depends on the state being loaded,
     * a *skeleton screen* should be displayed instead of the actual data.
     */
    WORKING,

    /**
     * The state is done loading.
     */
    LOADED,

}