package coolio.zoewong.traverse.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coolio.zoewong.traverse.util.MutableWaitFor

/**
 * Provides access to global app state.
 */
@Composable
fun AppState(children: @Composable () -> Unit) {
    val databaseReady = remember { MutableWaitFor<Unit>() }

    SplashScreenStateProvider {
        DatabaseStateProvider(onReady = splashScreenWaitsForThis("database ready")) {
            children()
        }
    }
}
