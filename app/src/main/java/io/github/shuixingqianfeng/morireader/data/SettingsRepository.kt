package io.github.shuixingqianfeng.morireader.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.moriDataStore by preferencesDataStore("reader_preferences")

enum class ReaderTheme { WHITE, SEPIA, GRAY, DARK }
enum class ReaderMode { PAGED, SCROLLED }

data class ReaderPreferences(
    val dailyGoalMinutes: Int = 30,
    val fontSizeSp: Float = 19f,
    val lineHeight: Float = 1.75f,
    val paragraphSpacingEm: Float = 0.8f,
    val horizontalMarginDp: Int = 24,
    val theme: ReaderTheme = ReaderTheme.WHITE,
    val mode: ReaderMode = ReaderMode.PAGED,
    val swipeEnabled: Boolean = true,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val goal = intPreferencesKey("daily_goal_minutes")
        val fontSize = floatPreferencesKey("font_size_sp")
        val lineHeight = floatPreferencesKey("line_height")
        val paragraphSpacing = floatPreferencesKey("paragraph_spacing_em")
        val margin = intPreferencesKey("horizontal_margin_dp")
        val theme = stringPreferencesKey("reader_theme")
        val mode = stringPreferencesKey("reader_mode")
        val swipe = booleanPreferencesKey("swipe_enabled")
    }

    val preferences: Flow<ReaderPreferences> = context.moriDataStore.data.map { values ->
        ReaderPreferences(
            dailyGoalMinutes = values[Keys.goal] ?: 30,
            fontSizeSp = values[Keys.fontSize] ?: 19f,
            lineHeight = values[Keys.lineHeight] ?: 1.75f,
            paragraphSpacingEm = values[Keys.paragraphSpacing] ?: 0.8f,
            horizontalMarginDp = values[Keys.margin] ?: 24,
            theme = values[Keys.theme]?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() } ?: ReaderTheme.WHITE,
            mode = values[Keys.mode]?.let { runCatching { ReaderMode.valueOf(it) }.getOrNull() } ?: ReaderMode.PAGED,
            swipeEnabled = values[Keys.swipe] ?: true,
        )
    }

    suspend fun update(transform: (ReaderPreferences) -> ReaderPreferences) {
        context.moriDataStore.edit { values ->
            val current = ReaderPreferences(
                dailyGoalMinutes = values[Keys.goal] ?: 30,
                fontSizeSp = values[Keys.fontSize] ?: 19f,
                lineHeight = values[Keys.lineHeight] ?: 1.75f,
                paragraphSpacingEm = values[Keys.paragraphSpacing] ?: 0.8f,
                horizontalMarginDp = values[Keys.margin] ?: 24,
                theme = values[Keys.theme]?.let { runCatching { ReaderTheme.valueOf(it) }.getOrNull() } ?: ReaderTheme.WHITE,
                mode = values[Keys.mode]?.let { runCatching { ReaderMode.valueOf(it) }.getOrNull() } ?: ReaderMode.PAGED,
                swipeEnabled = values[Keys.swipe] ?: true,
            )
            val next = transform(current)
            values[Keys.goal] = next.dailyGoalMinutes.coerceIn(1, 1440)
            values[Keys.fontSize] = next.fontSizeSp.coerceIn(12f, 40f)
            values[Keys.lineHeight] = next.lineHeight.coerceIn(1.1f, 2.5f)
            values[Keys.paragraphSpacing] = next.paragraphSpacingEm.coerceIn(0f, 3f)
            values[Keys.margin] = next.horizontalMarginDp.coerceIn(8, 64)
            values[Keys.theme] = next.theme.name
            values[Keys.mode] = next.mode.name
            values[Keys.swipe] = next.swipeEnabled
        }
    }
}
