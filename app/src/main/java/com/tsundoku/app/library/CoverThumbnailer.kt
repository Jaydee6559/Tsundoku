package com.tsundoku.app.library

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Turns a full-size cover into a small WebP (about 300 px wide, ~15 KB) stored privately.
 * The grid then loads tiny files instead of decoding big images from inside EPUBs.
 */
object CoverThumbnailer {

    private const val TARGET_WIDTH = 300
    private const val QUALITY = 80

    /** Returns true when [out] was written. Never throws. */
    fun save(imageBytes: ByteArray, out: File): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

        // Decode at a reduced size straight away (saves memory on huge covers).
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= TARGET_WIDTH || bounds.outHeight / (sample * 2) >= TARGET_WIDTH * 4) {
            sample *= 2
        }
        val decoded = BitmapFactory.decodeByteArray(
            imageBytes, 0, imageBytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return false

        val scaled = if (decoded.width > TARGET_WIDTH) {
            val h = (decoded.height.toLong() * TARGET_WIDTH / decoded.width).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, TARGET_WIDTH, h, true)
        } else {
            decoded
        }

        out.parentFile?.mkdirs()
        val tmp = File(out.parentFile, out.name + ".tmp")
        tmp.outputStream().use { scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, QUALITY, it) }
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        tmp.renameTo(out)
    }.getOrDefault(false)
}
