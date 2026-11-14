package com.tsundoku.app.diagnostics

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves the details of a crash to a private file, so the next launch can show it
 * (with a Copy button) instead of the crash vanishing silently.
 *
 * Privacy: the report stays on the phone. Nothing is sent anywhere (the app has no
 * internet permission); you decide whether to copy and share it.
 */
object CrashLog {

    private const val FILE_NAME = "last_crash.txt"
    private const val MAX_CHARS = 12_000

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(appContext, thread, error) }
            // Let Android show its normal "keeps stopping" dialog and close the app.
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? =
        file(context).takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }

    fun clear(context: Context) {
        file(context).delete()
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
        val report = buildString {
            appendLine("Tsundoku crash report")
            appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("App: $version")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(trace)
        }.take(MAX_CHARS)
        file(context).writeText(report)
    }

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)
}
