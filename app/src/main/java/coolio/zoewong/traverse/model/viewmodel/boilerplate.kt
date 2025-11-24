package coolio.zoewong.traverse.model.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coolio.zoewong.traverse.database.TraverseRepository
import coolio.zoewong.traverse.ui.state.DatabaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Creates a function that when called, runs the given handler using the IO dispatcher.
 *
 * If called and the database is not yet ready, the handler will wait for the database
 * to become ready before running.
 *
 * Usage:
 *
 *     val createStory = editDatabase(::toCreateStory)
 *     val story = Story(...)
 *     Button(onClick = { createStory(story) }) {
 *         Text("Create Story")
 *     }
 */
@Composable
fun editDatabase(
    handler: @DisallowComposableCalls suspend (TraverseRepository) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val dbstate = DatabaseState.current
    return remember(context, dbstate) {
        fun() {
            CoroutineScope(Dispatchers.IO).launch {
                val db = dbstate.waitForReady()
                handler(db)
            }
        }
    }
}

/**
 * Creates a function that when called, runs the given handler using the IO dispatcher.
 *
 * If called and the database is not yet ready, the handler will wait for the database
 * to become ready before running.
 *
 * Usage:
 *
 *     val createStory = editDatabase(::toCreateStory)
 *     val story = Story(...)
 *     Button(onClick = { createStory(story) }) {
 *         Text("Create Story")
 *     }
 */
@Composable
fun <P1> editDatabase(
    handler: @DisallowComposableCalls suspend (TraverseRepository, P1) -> Unit
): (P1) -> Unit {
    val context = LocalContext.current
    val dbstate = DatabaseState.current
    return remember(context, dbstate) {
        fun(p1: P1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = dbstate.waitForReady()
                handler(db, p1)
            }
        }
    }
}

/**
 * Creates a function that when called, runs the given handler using the IO dispatcher.
 *
 * If called and the database is not yet ready, the handler will wait for the database
 * to become ready before running.
 *
 * Usage:
 *
 *     val createStory = editDatabase(::toCreateStory)
 *     val story = Story(...)
 *     Button(onClick = { createStory(story) }) {
 *         Text("Create Story")
 *     }
 */
@Composable
fun <P1, P2> editDatabase(
    handler: @DisallowComposableCalls suspend (TraverseRepository, P1, P2) -> Unit
): (P1, P2) -> Unit {
    val context = LocalContext.current
    val dbstate = DatabaseState.current
    return remember(context, dbstate) {
        fun(p1: P1, p2: P2) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = dbstate.waitForReady()
                handler(db, p1, p2)
            }
        }
    }
}
