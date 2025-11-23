package coolio.zoewong.traverse.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    var isDarkMode by mutableStateOf(false)

    fun toggleTheme(context: Context) {
        isDarkMode = !isDarkMode
        // Save to SharedPreferences
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dark_mode", isDarkMode).apply()
    }

    fun loadTheme(context: Context) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        isDarkMode = prefs.getBoolean("dark_mode", false)
    }
}