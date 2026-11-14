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

/** Size of book tiles in the Library (grid width / list cover size). */
enum class TileSize { SMALL, MEDIUM, LARGE }

/** How books look in the Library (Library → Display options). */
data class LibraryDisplay(
    val tileSize: TileSize = TileSize.MEDIUM,
    /** Show the whole title instead of cutting it after 2 lines. */
    val fullTitles: Boolean = false,
    /** Author / series line under the title. */
    val showSubtitle: Boolean = true,
    /** Reading status (dot + label). */
    val showStatus: Boolean = true,
    /** Folder chip (Download, ReadEra...) in list view. */
    val showLocation: Boolean = true,
    /** The EPUB's own tags (genres) under each book. */
    val showEpubTags: Boolean = true,
)

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
        val TILE_SIZE = stringPreferencesKey("tile_size")
        val FULL_TITLES = booleanPreferencesKey("full_titles")
        val SHOW_SUBTITLE = booleanPreferencesKey("show_subtitle")
        val SHOW_STATUS = booleanPreferencesKey("show_status")
        val SHOW_LOCATION = booleanPreferencesKey("show_location")
        val SHOW_EPUB_TAGS = booleanPreferencesKey("show_epub_tags")
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

    // ---- Library display options

    val libraryDisplay: Flow<LibraryDisplay> = context.dataStore.data.map { p ->
        LibraryDisplay(
            tileSize = p[Keys.TILE_SIZE]?.let { n -> TileSize.entries.firstOrNull { it.name == n } } ?: TileSize.MEDIUM,
            fullTitles = p[Keys.FULL_TITLES] ?: false,
            showSubtitle = p[Keys.SHOW_SUBTITLE] ?: true,
            showStatus = p[Keys.SHOW_STATUS] ?: true,
            showLocation = p[Keys.SHOW_LOCATION] ?: true,
            showEpubTags = p[Keys.SHOW_EPUB_TAGS] ?: true,
        )
    }.distinctUntilChanged()

    suspend fun setLibraryDisplay(d: LibraryDisplay) {
        context.dataStore.edit {
            it[Keys.TILE_SIZE] = d.tileSize.name
            it[Keys.FULL_TITLES] = d.fullTitles
            it[Keys.SHOW_SUBTITLE] = d.showSubtitle
            it[Keys.SHOW_STATUS] = d.showStatus
            it[Keys.SHOW_LOCATION] = d.showLocation
            it[Keys.SHOW_EPUB_TAGS] = d.showEpubTags
        }
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
