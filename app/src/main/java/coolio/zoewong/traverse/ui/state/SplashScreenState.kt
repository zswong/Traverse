package coolio.zoewong.traverse.ui.state


import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import coolio.zoewong.traverse.util.MutableWaitFor
import coolio.zoewong.traverse.util.WaitFor
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides access to memories that can be displayed in the journal page.
 */
@Composable
fun SplashScreenStateProvider(
    children: @Composable () -> Unit,
) {
    val tag = "SplashScreen"

    val waitingFor = remember { ConcurrentHashMap.newKeySet<Pair<String, WaitFor<Unit>>>() }
    var ready by remember { mutableStateOf(false) }

    val waitForFirstComposition = remember { MutableWaitFor<Unit>() }
    SideEffect {
        waitForFirstComposition.done(Unit)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            waitForFirstComposition()
            while (!waitingFor.isEmpty()) {
                val pair = waitingFor.first()
                waitingFor.remove(pair)

                val (name, wait) = pair
                Log.i(tag, "Waiting for ${name}...")
                wait()
                Log.i(tag, "Done waiting for ${name}.")
            }

            Log.i(tag, "Splash screen is no longer needed.")
            ready = true
        }
    }

    // Provide access to children.
    CompositionLocalProvider(SplashScreenVisible provides !ready) {
        CompositionLocalProvider(SplashScreenWaitingFor provides waitingFor) {
            children()
        }
    }
}

@Composable
fun shouldSplashScreenBeVisible(): Boolean {
    return SplashScreenVisible.current
}

@Composable
fun splashScreenWaitsFor(tag: String, waitFor: WaitFor<Unit>) {
    if (!shouldSplashScreenBeVisible()) {
        return
    }

    Log.i("SplashScreen", "Want to wait for ${tag}.")

    val splashWaitingFor = SplashScreenWaitingFor.current
    DisposableEffect(waitFor, splashWaitingFor) {
        splashWaitingFor.add(tag to waitFor)
        onDispose {
            splashWaitingFor.remove(tag to waitFor)
        }
    }
}

@Composable
fun splashScreenWaitsForThis(tag: String): () -> Unit {
    val waitFor = remember { MutableWaitFor<Unit>() }
    splashScreenWaitsFor(tag, waitFor)
    return {
        waitFor.done(Unit)
    }
}

internal val SplashScreenVisible = compositionLocalOf { false }
internal val SplashScreenWaitingFor = compositionLocalOf<MutableSet<Pair<String, WaitFor<Unit>>>> {
    throw IllegalStateException("SplashScreenStateProvider not in UI hierarchy")
}
