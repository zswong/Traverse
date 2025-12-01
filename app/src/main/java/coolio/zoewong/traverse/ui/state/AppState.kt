package coolio.zoewong.traverse.ui.state

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coolio.zoewong.traverse.Settings
import coolio.zoewong.traverse.service.storyanalysis.StoryAnalysisEvent
import coolio.zoewong.traverse.service.storyanalysis.StoryAnalysisServiceManager
import coolio.zoewong.traverse.ui.provider.MemoryListProvider
import coolio.zoewong.traverse.ui.provider.StoryListProvider
import coolio.zoewong.traverse.ui.provider.throwWhenNotInHierarchy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.getValue

/**
 * Provides access to global app state.
 */
@Composable
fun AppState(
    activity: ComponentActivity,
    children: @Composable () -> Unit,
) {
    val persistentState by remember(activity) {
        activity.viewModels<PersistentAppStateViewModel>()
    }

    SettingsStateProvider(activity) {
        val settings = getSettings()
        val settingsManager = getSettingsManager()
        val storyAnalysisService = persistentState.storyAnalysisService
        var storyAnalysisSupported by remember { mutableStateOf(true) }

        // Ensure the StoryAnalysisService is started/stopped based on settings.
        if (settings.enableStoryAnalysis) {
            storyAnalysisService.start(activity)
        } else {
            storyAnalysisService.shutdown()
        }

        LaunchedEffect(storyAnalysisService,settings.enableStoryAnalysis) {
            if (!settings.enableStoryAnalysis) {
                return@LaunchedEffect
            }

            storyAnalysisService.getEvents().collect { event ->
                if (event is StoryAnalysisEvent.ModelInitializationFailed) {
                    Log.d("AppState", "Story analysis is not supported.")
                    storyAnalysisSupported = false
                    settingsManager.changeSettings(settings.copy(enableStoryAnalysis = false))
                }
            }
        }

        // Provide access to state.
        CompositionLocalProvider(values=arrayOf(
            localPersistentAppState provides persistentState,
            localStoryAnalysisSupported provides storyAnalysisSupported,
        )) {
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
    }
}

/**
 * Returns the StoryAnalysisServiceManager instance.
 */
@Composable
fun getStoryAnalysisService(): StoryAnalysisServiceManager {
    return localPersistentAppState.current.storyAnalysisService
}

/**
 * Returns true unless the story analysis model initialization failed.
 */
@Composable
fun isStoryAnalysisSupported(): Boolean {
    return localStoryAnalysisSupported.current
}

/**
 * State bound to the Android activity's lifecycle. Will persist across configuration
 * changes such as screen rotations.
 */
internal class PersistentAppStateViewModel : androidx.lifecycle.ViewModel() {
    val storyAnalysisService = StoryAnalysisServiceManager()

    init {
        Log.d("PersistentAppStateViewModel", "Created new ViewModel instance")
        this.addCloseable {
            storyAnalysisService.shutdown()
        }
    }
}

internal val localPersistentAppState = compositionLocalOf<PersistentAppStateViewModel>(
    policy = referentialEqualityPolicy(),
    defaultFactory = throwWhenNotInHierarchy("AppState"),
)

internal val localStoryAnalysisSupported = compositionLocalOf<Boolean>(
    policy = referentialEqualityPolicy(),
    defaultFactory = throwWhenNotInHierarchy("AppState"),
)
