package coolio.zoewong.traverse

import android.content.SharedPreferences

/**
 * App settings.
 *
 * For dark/light theme settings, see .ui.theme.ThemeManager.
 */
data class Settings(
    val enableStoryAnalysis: Boolean
) {
    companion object {
        private const val KEY_ENABLE_STORY_ANALYSIS = "enable_story_analysis"

        /**
         * Loads the app settings from SharedPreferences.
         */
        fun fromSharedPreferences(prefs: SharedPreferences): Settings {
            return Settings(
                enableStoryAnalysis = prefs.getBoolean(KEY_ENABLE_STORY_ANALYSIS, false)
            )
        }

        /**
         * Saves the app settings to SharedPreferences.
         */
        fun toSharedPreferences(prefs: SharedPreferences, settings: Settings) {
            prefs.edit().apply {
                putBoolean(KEY_ENABLE_STORY_ANALYSIS, settings.enableStoryAnalysis)
            }.apply()
        }
    }
}
