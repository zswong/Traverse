package coolio.zoewong.traverse.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coolio.zoewong.traverse.ui.provider.MemoryListProvider
import coolio.zoewong.traverse.ui.provider.StoryListProvider
import coolio.zoewong.traverse.util.MutableWaitFor

/**
 * Provides access to global app state.
 */
@Composable
fun AppState(children: @Composable () -> Unit) {
    val databaseReady = remember { MutableWaitFor<Unit>() }

    SplashScreenStateProvider {
        DatabaseStateProvider(onReady = splashScreenWaitsForThis("database ready")) {

            // Always keep the list of memories and stories loaded.
            // Keep the splash screen visible until they're ready.
            MemoryListProvider(onReady = splashScreenWaitsForThis("memories loaded")) {
                StoryListProvider(onReady = splashScreenWaitsForThis("stories loaded")) {
                    children()
                }
            }
        }
    }
}
