package coolio.zoewong.traverse.ui.state

import androidx.compose.runtime.Composable
import coolio.zoewong.traverse.database.TraverseRepository

/**
 * Provides access to global app state.
 */
@Composable
fun AppState(children: @Composable () -> Unit) {
    DatabaseStateProvider {
        children()
    }
}
