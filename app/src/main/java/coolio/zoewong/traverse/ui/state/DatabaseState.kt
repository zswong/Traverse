package coolio.zoewong.traverse.ui.state

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coolio.zoewong.traverse.database.TraverseRepository
import coolio.zoewong.traverse.util.MutableWaitFor
import coolio.zoewong.traverse.util.WaitFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides access to the current DatabaseStateAccessor.
 *
 * Can only be used in a composable that has a DatabaseStateProvider as one of its
 * UI hierarchy ancestors.
 *
 * Usage:
 *
 *     @Composable
 *     fun MyComponent() {
 *         val memories = rememberWhenDatabaseReady(
 *             initial = { listOf<MemoryEntity>() },
 *             calculation = { db ->
 *                 return getAllMemories(db)
 *             }
 *         )
 *
 *         if (memories == null) {
 *             // Show a loading indicator
 *             return
 *         }
 *
 *         // Show memories
 *     }
 *
 * See: https://developer.android.com/develop/ui/compose/compositionlocal
 */
val DatabaseState = compositionLocalOf<DatabaseStateAccessor> {
    throw IllegalStateException("DatabaseStateProvider not in UI hierarchy")
}

/**
 * Provides access to the TraverseRepository and other global app state.
 * Must wrap all app content composables.
 *
 * Usage:
 *
 *     @Composable
 *     fun AppRoot() {
 *         DatabaseStateProvider {
 *             // Anything under here has access to DatabaseState.current
 *             MyComponent()
 *         }
 *     }
 */
@Composable
fun DatabaseStateProvider(children: @Composable () -> Unit) {
    val tag = "DatabaseStateProvider"
    val context = LocalContext.current
    val waitForReady = remember { MutableWaitFor<TraverseRepository>() }
    val (state, setState) = remember {
        mutableStateOf(
            DatabaseStateAccessor(
                status = LoadStatus.WORKING,
                _waitForReady = waitForReady,
                _database = null,
            )
        )
    }

    // Load database in the background.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            Log.i(tag, "Loading database...")
            val database = TraverseRepository.getInstance(context)
            setState(
                DatabaseStateAccessor(
                    status = LoadStatus.LOADED,
                    _waitForReady = waitForReady,
                    _database = database
                )
            )
            Log.i(tag, "Database loaded.")
            waitForReady.done(database)
        }
    }

    // Provide access to children.
    CompositionLocalProvider(
        DatabaseState provides state,
    ) {
        children()
    }

}

data class DatabaseStateAccessor(
    val status: LoadStatus,
    private val _waitForReady: WaitFor<TraverseRepository>,
    private val _database: TraverseRepository?,
) {

    /**
     * The database repository.
     *
     * If the status is not LOADED, throws an exception.
     */
    val database: TraverseRepository
        get() = when (status) {
            LoadStatus.LOADED -> _database!!
            else -> throw IllegalStateException("Database not loaded. Check DatabaseState.status first.")
        }

    /**
     * Runs the given callback only if the database is loaded.
     * Acts as a convenience method to make the usage more like a DSL.
     */
    inline fun whenReady(callback: (db: TraverseRepository) -> Unit) {
        if (status == LoadStatus.LOADED) {
            callback(database)
        }
    }

    /**
     * Suspends until the database is loaded.
     *
     * If the database is already loaded, returns immediately.
     */
    suspend fun waitForReady(): TraverseRepository {
        return _waitForReady()
    }
}

/**
 * Performs a calculation based on whether the database is ready, changing it when the
 * database is done loading.
 *
 * The calculation will persist through recompositions.
 */
@Composable
inline fun <T> rememberDatabaseStateChanges(
    vararg keys: Any?,
    crossinline calculation: @DisallowComposableCalls (state: DatabaseStateAccessor) -> T,
): T {
    val dbstate = DatabaseState.current // Causes recomposition when when DatabaseState changes.

    val (remembered, setRemembered) = remember(keys = keys) {
        mutableStateOf(
            dbstate.status to calculation(
                dbstate
            )
        )
    }

    var (lastStatus, result) = remembered
    if (lastStatus != dbstate.status) {
        result = calculation(dbstate)
        setRemembered(dbstate.status to result)
    }

    return result
}

/**
 * Returns null if the database is not ready, otherwise runs the given calculation.
 *
 * Usage:
 *
 *     @Composable
 *     fun MyComponent() {
 *         val memories = rememberWhenDatabaseReady { db ->
 *             return getAllMemories(db)
 *         }
 *
 *         memories?.let {
 *             // Show data from the database
 *         }
 *     }
 *
 * The calculation will persist through recompositions.
 */
@Composable
inline fun <T> rememberWhenDatabaseReady(
    vararg keys: Any?,
    crossinline calculation: @DisallowComposableCalls (db: TraverseRepository) -> T,
): T? {
    return rememberDatabaseStateChanges(keys = keys) {
        when (it.status) {
            LoadStatus.LOADED -> calculation(it.database)
            else -> null
        }
    }
}

/**
 * Returns the initial value if the database is not ready, otherwise runs the given calculation.
 *
 * Usage:
 *
 *     @Composable
 *     fun MyComponent() {
 *         val memories = rememberWhenDatabaseReady(
 *             initial = { listOf<MemoryEntity>() },
 *             calculation = { db ->
 *                 return getAllMemories(db)
 *             }
 *         )
 *
 *         // Show memories
 *     }
 *
 * The calculation will persist through recompositions.
 */
@Composable
inline fun <T> rememberWhenDatabaseReady(
    vararg keys: Any?,
    crossinline initial: @DisallowComposableCalls () -> T,
    crossinline calculation: @DisallowComposableCalls (db: TraverseRepository) -> T,
): T {
    return rememberDatabaseStateChanges(keys = keys) {
        when (it.status) {
            LoadStatus.LOADED -> calculation(it.database)
            else -> initial()
        }
    }
}