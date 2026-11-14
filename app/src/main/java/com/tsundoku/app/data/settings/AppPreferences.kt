package com.tsundoku.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tsundoku.app.library.LibraryFilter
import com.tsundoku.app.library.LibrarySort
import com.tsundoku.app.library.LibraryViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** App theme choice. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Default folder for books added with "Share → Tsundoku". */
const val DEFAULT_SHARE_FOLDER = "Books/Tsundoku"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tsundoku_prefs")

/** Small app settings stored on the phone (DataStore). Library data lives in Room. */
class AppPreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LIBRARY_VIEW = stringPreferencesKey("library_view")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val LIBRARY_FILTER = stringPreferencesKey("library_filter")
        val LIBRARY_LOCATION = stringPreferencesKey("library_location")
        val THEME = stringPreferencesKey("theme")
        val AMOLED = booleanPreferencesKey("amoled")
        val EXCLUDED = stringSetPreferencesKey("excluded_folders")
        val SHARE_FOLDER = stringPreferencesKey("share_folder")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val LAST_BACKUP = longPreferencesKey("last_backup_at")
    }

    /** True once the user has finished the first-launch flow. */
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    // ---- Library view choices (remembered between launches)

    val libraryView: Flow<LibraryViewMode> = enumFlow(Keys.LIBRARY_VIEW, LibraryViewMode.GRID)
    val librarySort: Flow<LibrarySort> = enumFlow(Keys.LIBRARY_SORT, LibrarySort.RECENT)
    val libraryFilter: Flow<LibraryFilter> = enumFlow(Keys.LIBRARY_FILTER, LibraryFilter.ALL)

    /** Chosen location ("Download", "ReadEra"...), or null for everywhere. */
    val libraryLocation: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[Keys.LIBRARY_LOCATION]?.takeIf { it.isNotEmpty() } }
        .distinctUntilChanged()

    suspend fun setLibraryLocation(value: String?) {
        context.dataStore.edit { it[Keys.LIBRARY_LOCATION] = value.orEmpty() }
    }

    // ---- Appearance

    val themeMode: Flow<ThemeMode> = enumFlow(Keys.THEME, ThemeMode.SYSTEM)
    val amoled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AMOLED] ?: false }.distinctUntilChanged()

    suspend fun setThemeMode(value: ThemeMode) = setEnum(Keys.THEME, value)
    suspend fun setAmoled(value: Boolean) {
        context.dataStore.edit { it[Keys.AMOLED] = value }
    }

    // ---- Folders (paths relative to internal storage, e.g. "Download/Manga")

    /** Folders (and everything inside) the scanner and watcher ignore. */
    val excludedFolders: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.EXCLUDED] ?: emptySet() }.distinctUntilChanged()

    suspend fun excludedFoldersNow(): Set<String> = excludedFolders.first()

    suspend fun addExcludedFolder(folder: String) {
        val clean = folder.trim('/')
        if (clean.isEmpty()) return // never exclude the whole storage
        context.dataStore.edit { it[Keys.EXCLUDED] = (it[Keys.EXCLUDED] ?: emptySet()) + clean }
    }

    suspend fun removeExcludedFolder(folder: String) {
        context.dataStore.edit { it[Keys.EXCLUDED] = (it[Keys.EXCLUDED] ?: emptySet()) - folder }
    }

    val shareFolder: Flow<String> =
        context.dataStore.data.map { it[Keys.SHARE_FOLDER] ?: DEFAULT_SHARE_FOLDER }.distinctUntilChanged()

    suspend fun setShareFolder(folder: String) {
        context.dataStore.edit { it[Keys.SHARE_FOLDER] = folder.trim('/').ifEmpty { DEFAULT_SHARE_FOLDER } }
    }

    // ---- Backups

    val autoBackup: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_BACKUP] ?: true }.distinctUntilChanged()
    val lastBackupAt: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_BACKUP] ?: 0L }.distinctUntilChanged()

    suspend fun setAutoBackup(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP] = value }
    }

    suspend fun setLastBackupAt(value: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP] = value }
    }

    suspend fun setLibraryView(value: LibraryViewMode) = setEnum(Keys.LIBRARY_VIEW, value)
    suspend fun setLibrarySort(value: LibrarySort) = setEnum(Keys.LIBRARY_SORT, value)
    suspend fun setLibraryFilter(value: LibraryFilter) = setEnum(Keys.LIBRARY_FILTER, value)

    private inline fun <reified E : Enum<E>> enumFlow(key: Preferences.Key<String>, default: E): Flow<E> =
        context.dataStore.data
            .map { prefs -> prefs[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default }
            .distinctUntilChanged()

    private suspend fun <E : Enum<E>> setEnum(key: Preferences.Key<String>, value: E) {
        context.dataStore.edit { it[key] = value.name }
    }
}
