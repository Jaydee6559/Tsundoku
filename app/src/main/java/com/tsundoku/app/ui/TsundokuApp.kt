package com.tsundoku.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tsundoku.app.data.settings.AppPreferences
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.library.TrackingRepository
import com.tsundoku.app.library.TrashRepository
import com.tsundoku.app.library.SeriesRepository
import com.tsundoku.app.library.UpNextRepository
import com.tsundoku.app.ui.home.HomeScreen
import com.tsundoku.app.ui.home.UpNextScreen
import com.tsundoku.app.ui.series.SeriesDetailScreen
import com.tsundoku.app.ui.series.SeriesListScreen
import android.net.Uri
import com.tsundoku.app.AppContainer
import com.tsundoku.app.ui.settings.DeletedForeverScreen
import com.tsundoku.app.ui.settings.DuplicatesScreen
import com.tsundoku.app.ui.settings.ExcludedFoldersScreen
import com.tsundoku.app.ui.stats.StatsScreen
import com.tsundoku.app.ui.settings.RestoreScreen
import com.tsundoku.app.ui.settings.ScannerStatusScreen
import com.tsundoku.app.ui.settings.SettingsScreen
import com.tsundoku.app.ui.trash.TrashScreen
import com.tsundoku.app.ui.book.BookDetailScreen
import com.tsundoku.app.ui.library.LibraryScreen
import com.tsundoku.app.ui.navigation.TopLevelDestination

@Composable
fun TsundokuApp(
    library: LibraryRepository,
    tracking: TrackingRepository,
    trash: TrashRepository,
    series: SeriesRepository,
    upNext: UpNextRepository,
    preferences: AppPreferences,
    container: AppContainer,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // Full-height screens: no bottom bar there.
    val showBottomBar = currentDestination?.route !in setOf(BOOK_ROUTE, TRASH_ROUTE, SERIES_ROUTE, UP_NEXT_ROUTE, EXCLUDED_ROUTE, RESTORE_ROUTE, STATUS_ROUTE, DUPLICATES_ROUTE, STATS_ROUTE, DELETED_ROUTE)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                TopLevelDestination.entries.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                // Standard bottom-nav behavior: one copy of each tab, state kept.
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(dest.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    library = library,
                    tracking = tracking,
                    upNext = upNext,
                    onOpenBook = { id -> navController.navigate("book/$id") },
                    onEditUpNext = { navController.navigate(UP_NEXT_ROUTE) },
                    onOpenLibrary = {
                        navController.navigate(TopLevelDestination.LIBRARY.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(UP_NEXT_ROUTE) {
                UpNextScreen(
                    upNext = upNext,
                    onBack = { navController.popBackStack() },
                    onOpenBook = { id -> navController.navigate("book/$id") },
                )
            }
            composable(TopLevelDestination.LIBRARY.route) {
                LibraryScreen(
                    library = library,
                    tracking = tracking,
                    trash = trash,
                    upNext = upNext,
                    preferences = preferences,
                    onBookClick = { id -> navController.navigate("book/$id") },
                )
            }
            composable(BOOK_ROUTE, arguments = listOf(navArgument("bookId") { type = NavType.LongType })) { entry ->
                BookDetailScreen(
                    bookId = entry.arguments?.getLong("bookId") ?: 0L,
                    tracking = tracking,
                    trash = trash,
                    upNext = upNext,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(TRASH_ROUTE) {
                TrashScreen(trash = trash, onBack = { navController.popBackStack() })
            }
            composable(TopLevelDestination.SERIES.route) {
                SeriesListScreen(
                    seriesRepo = series,
                    onOpenSeries = { key -> navController.navigate("series/${Uri.encode(key)}") },
                )
            }
            composable(SERIES_ROUTE, arguments = listOf(navArgument("key") { type = NavType.StringType })) { entry ->
                SeriesDetailScreen(
                    seriesKey = entry.arguments?.getString("key").orEmpty(),
                    seriesRepo = series,
                    tracking = tracking,
                    onBack = { navController.popBackStack() },
                    onOpenBook = { id -> navController.navigate("book/$id") },
                )
            }
            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen(
                    container = container,
                    onOpenTrash = { navController.navigate(TRASH_ROUTE) },
                    onOpenExcluded = { navController.navigate(EXCLUDED_ROUTE) },
                    onOpenRestore = { navController.navigate(RESTORE_ROUTE) },
                    onOpenStatus = { navController.navigate(STATUS_ROUTE) },
                    onOpenDuplicates = { navController.navigate(DUPLICATES_ROUTE) },
                    onOpenStats = { navController.navigate(STATS_ROUTE) },
                    onOpenDeleted = { navController.navigate(DELETED_ROUTE) },
                )
            }
            composable(EXCLUDED_ROUTE) {
                ExcludedFoldersScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(RESTORE_ROUTE) {
                RestoreScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(DUPLICATES_ROUTE) {
                DuplicatesScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onOpenBook = { id -> navController.navigate("book/$id") },
                )
            }
            composable(DELETED_ROUTE) {
                DeletedForeverScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(STATS_ROUTE) {
                StatsScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(STATUS_ROUTE) {
                ScannerStatusScreen(container = container, onBack = { navController.popBackStack() })
            }
        }
    }
}

private const val BOOK_ROUTE = "book/{bookId}"
private const val TRASH_ROUTE = "trash"
private const val SERIES_ROUTE = "series/{key}"
private const val UP_NEXT_ROUTE = "up-next"
private const val EXCLUDED_ROUTE = "settings/excluded"
private const val RESTORE_ROUTE = "settings/restore"
private const val STATUS_ROUTE = "settings/status"
private const val DUPLICATES_ROUTE = "settings/duplicates"
private const val STATS_ROUTE = "stats"
private const val DELETED_ROUTE = "settings/deleted"
