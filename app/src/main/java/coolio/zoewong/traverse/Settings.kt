package coolio.zoewong.traverse

import android.content.SharedPreferences

/**
 * App settings.
 *
 * For dark/light theme settings, see .ui.theme.ThemeManager.
 */
data class Settings(
    val enableStoryAnalysis: Boolean,
    val showStorySummaryByDefault: Boolean,
) {
    companion object {
        private const val KEY_ENABLE_STORY_ANALYSIS = "enable_story_analysis"
        private const val KEY_SHOW_STORY_SUMMARY_BY_DEFAULT = "show_story_summary_by_default"

        /**
         * Loads the app settings from SharedPreferences.
         */
        fun fromSharedPreferences(prefs: SharedPreferences): Settings {
            return Settings(
                enableStoryAnalysis = prefs.getBoolean(KEY_ENABLE_STORY_ANALYSIS, false),
                showStorySummaryByDefault = prefs.getBoolean(KEY_SHOW_STORY_SUMMARY_BY_DEFAULT, false),
            )
        }

        /**
         * Saves the app settings to SharedPreferences.
         */
        fun toSharedPreferences(prefs: SharedPreferences, settings: Settings) {
            prefs.edit().apply {
                putBoolean(KEY_ENABLE_STORY_ANALYSIS, settings.enableStoryAnalysis)
                putBoolean(KEY_SHOW_STORY_SUMMARY_BY_DEFAULT, settings.showStorySummaryByDefault)
            }.apply()
        }
    }
}
