package com.tsundoku.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.tsundoku.app.R

/** The four bottom-bar tabs. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("home", R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    LIBRARY(
        "library",
        R.string.tab_library,
        Icons.AutoMirrored.Filled.LibraryBooks,
        Icons.AutoMirrored.Outlined.LibraryBooks,
    ),
    SERIES("series", R.string.tab_series, Icons.Filled.Layers, Icons.Outlined.Layers),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Tune, Icons.Outlined.Tune),
}
