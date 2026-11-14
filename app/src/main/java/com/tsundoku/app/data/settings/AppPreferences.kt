package com.tsundoku.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tsundoku.app.library.LibraryFilter
import com.tsundoku.app.library.LibrarySort
import com.tsundoku.app.library.LibraryViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tsundoku_prefs")

/** Small app settings stored on the phone (DataStore). Library data lives in Room. */
class AppPreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LIBRARY_VIEW = stringPreferencesKey("library_view")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val LIBRARY_FILTER = stringPreferencesKey("library_filter")
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
