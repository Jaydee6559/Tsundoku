package com.tsundoku.app.storage

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings

/** Helpers for Android's "All files access" (MANAGE_EXTERNAL_STORAGE). minSdk is 30, so it always exists. */
object StoragePermission {

    /** True when the user has turned on All files access for Tsundoku. */
    fun isGranted(): Boolean = Environment.isExternalStorageManager()

    /**
     * Opens the system page where the user turns the permission on.
     * Tries Tsundoku's own page first, then the general list (some phones lack the first).
     */
    fun openSettings(context: Context) {
        val appPage = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        val generalPage = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        try {
            context.startActivity(appPage)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(generalPage)
        }
    }
}
