package com.mafatih.reader

import android.content.Context

enum class AppFont { SYSTEM, ESTEDAD }
enum class AppTheme { LIGHT, SEPIA, DARK, NIGHT }

object PrefsManager {
    private const val PREFS_NAME = "mafatih_prefs"
    private const val KEY_FONT = "font"
    private const val KEY_THEME = "theme"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_SHOW_TRANSLATION = "show_translation"
    private const val KEY_LAST_READ = "last_read_index"
    private const val KEY_BOOKMARKS = "bookmarks"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFont(context: Context): AppFont {
        val v = prefs(context).getString(KEY_FONT, AppFont.ESTEDAD.name)
        return try { AppFont.valueOf(v ?: AppFont.ESTEDAD.name) } catch (e: Exception) { AppFont.ESTEDAD }
    }

    fun setFont(context: Context, font: AppFont) {
        prefs(context).edit().putString(KEY_FONT, font.name).apply()
    }

    fun getTheme(context: Context): AppTheme {
        val v = prefs(context).getString(KEY_THEME, AppTheme.LIGHT.name)
        return try { AppTheme.valueOf(v ?: AppTheme.LIGHT.name) } catch (e: Exception) { AppTheme.LIGHT }
    }

    fun setTheme(context: Context, theme: AppTheme) {
        prefs(context).edit().putString(KEY_THEME, theme.name).apply()
    }

    fun getFontScale(context: Context): Float =
        prefs(context).getFloat(KEY_FONT_SCALE, 1.0f)

    fun setFontScale(context: Context, scale: Float) {
        prefs(context).edit().putFloat(KEY_FONT_SCALE, scale).apply()
    }

    fun getShowTranslation(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_TRANSLATION, true)

    fun setShowTranslation(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_TRANSLATION, show).apply()
    }

    // ---- Last read position ----

    fun getLastReadIndex(context: Context): Int =
        prefs(context).getInt(KEY_LAST_READ, -1)

    fun setLastReadIndex(context: Context, recordIndex: Int) {
        prefs(context).edit().putInt(KEY_LAST_READ, recordIndex).apply()
    }

    // ---- Bookmarks (starred headings) ----
    // Stored as a String set of record-index strings in SharedPreferences.

    fun getBookmarks(context: Context): Set<Int> {
        val raw = prefs(context).getStringSet(KEY_BOOKMARKS, emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun isBookmarked(context: Context, recordIndex: Int): Boolean =
        getBookmarks(context).contains(recordIndex)

    fun toggleBookmark(context: Context, recordIndex: Int): Boolean {
        val current = getBookmarks(context).toMutableSet()
        val nowBookmarked: Boolean
        if (current.contains(recordIndex)) {
            current.remove(recordIndex)
            nowBookmarked = false
        } else {
            current.add(recordIndex)
            nowBookmarked = true
        }
        prefs(context).edit()
            .putStringSet(KEY_BOOKMARKS, current.map { it.toString() }.toSet())
            .apply()
        return nowBookmarked
    }
}
