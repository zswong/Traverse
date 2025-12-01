package coolio.zoewong.traverse.ui.state

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coolio.zoewong.traverse.Settings
import coolio.zoewong.traverse.database.TraverseRepository
import coolio.zoewong.traverse.ui.provider.throwWhenNotInHierarchy
import coolio.zoewong.traverse.util.MutableWaitFor
import coolio.zoewong.traverse.util.WaitFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Provides access to the app settings.
 * Must wrap all app content composables.
 */
@Composable
fun SettingsStateProvider(
    context: Context,
    children: @Composable () -> Unit,
) {
    val sharedPrefs = remember(context) {
        context.getSharedPreferences(
            "app_settings",
            Context.MODE_PRIVATE,
        )
    }

    var currentSettings by remember(sharedPrefs) {
        mutableStateOf(Settings.fromSharedPreferences(sharedPrefs))
    }

    val settingsManager = SettingsStateManager(
        sharedPrefs = sharedPrefs,
        onSettingsChanged = { newSettings ->
            currentSettings = newSettings
        }
    )

    // Provide access to children.
    CompositionLocalProvider(localSettings provides currentSettings) {
        CompositionLocalProvider(localSettingsManager provides settingsManager) {
            children()
        }
    }

}

data class SettingsStateManager(
    private val sharedPrefs: SharedPreferences,
    private val onSettingsChanged: (settings: Settings) -> Unit,
) {

    /**
     * Changes the app settings.
     *
     * Use the settings.copy function to create a new instance with the desired changes.
     */
    fun changeSettings(newSettings: Settings) {
        onSettingsChanged(newSettings)

        // Save the settings in the background.
        CoroutineScope(Dispatchers.IO).launch {
            withContext(NonCancellable) {
                Log.i("AppState", "Saving settings: $newSettings")
                Settings.toSharedPreferences(sharedPrefs, newSettings)
            }
        }
    }

}

/**
 * Returns the app settings.
 */
@Composable
fun getSettings(): Settings {
    return localSettings.current
}

/**
 * Returns the app settings manager.
 */
@Composable
fun getSettingsManager(): SettingsStateManager {
    return localSettingsManager.current
}

internal val localSettings = compositionLocalOf<Settings>(
    policy = referentialEqualityPolicy(),
    defaultFactory = throwWhenNotInHierarchy("SettingsStateProvider"),
)

internal val localSettingsManager = compositionLocalOf<SettingsStateManager>(
    policy = referentialEqualityPolicy(),
    defaultFactory = throwWhenNotInHierarchy("SettingsStateProvider"),
)
