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
import com.tsundoku.app.R
import com.tsundoku.app.data.settings.AppPreferences
import com.tsundoku.app.library.LibraryRepository
import com.tsundoku.app.library.TrackingRepository
import com.tsundoku.app.library.TrashRepository
import com.tsundoku.app.ui.settings.SettingsScreen
import com.tsundoku.app.ui.trash.TrashScreen
import com.tsundoku.app.ui.book.BookDetailScreen
import com.tsundoku.app.ui.library.LibraryScreen
import com.tsundoku.app.ui.navigation.TopLevelDestination
import com.tsundoku.app.ui.screens.HomeScreen
import com.tsundoku.app.ui.screens.PlaceholderScreen

@Composable
fun TsundokuApp(
    library: LibraryRepository,
    tracking: TrackingRepository,
    trash: TrashRepository,
    preferences: AppPreferences,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // Full-height screens: no bottom bar there.
    val showBottomBar = currentDestination?.route !in setOf(BOOK_ROUTE, TRASH_ROUTE)

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
            composable(TopLevelDestination.HOME.route) { HomeScreen() }
            composable(TopLevelDestination.LIBRARY.route) {
                LibraryScreen(
                    library = library,
                    tracking = tracking,
                    trash = trash,
                    preferences = preferences,
                    onBookClick = { id -> navController.navigate("book/$id") },
                )
            }
            composable(BOOK_ROUTE, arguments = listOf(navArgument("bookId") { type = NavType.LongType })) { entry ->
                BookDetailScreen(
                    bookId = entry.arguments?.getLong("bookId") ?: 0L,
                    tracking = tracking,
                    trash = trash,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(TRASH_ROUTE) {
                TrashScreen(trash = trash, onBack = { navController.popBackStack() })
            }
            composable(TopLevelDestination.SERIES.route) {
                PlaceholderScreen(R.string.tab_series, R.string.placeholder_series)
            }
            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen(library = library, trash = trash, onOpenTrash = { navController.navigate(TRASH_ROUTE) })
            }
        }
    }
}

private const val BOOK_ROUTE = "book/{bookId}"
private const val TRASH_ROUTE = "trash"
