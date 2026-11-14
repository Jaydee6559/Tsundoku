package com.tsundoku.app.epub

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

/**
 * A quick "identity" for a file that survives moves and renames.
 *
 * Hashing a whole 5 MB EPUB for hundreds of books would be slow, so we hash only the
 * first and last 64 KB plus the exact size. Two different EPUBs matching all three is
 * practically impossible, and it takes about a millisecond per file.
 */
object FileFingerprint {

    private const val CHUNK = 64 * 1024

    fun compute(file: File): String {
        val size = file.length()
        val digest = MessageDigest.getInstance("SHA-256")
        RandomAccessFile(file, "r").use { raf ->
            val head = ByteArray(minOf(CHUNK.toLong(), size).toInt())
            raf.readFully(head)
            digest.update(head)
            if (size > CHUNK) {
                val tailLen = minOf(CHUNK.toLong(), size - CHUNK).toInt()
                val tail = ByteArray(tailLen)
                raf.seek(size - tailLen)
                raf.readFully(tail)
                digest.update(tail)
            }
        }
        digest.update(size.toString().toByteArray())
        val hex = digest.digest().joinToString("") { "%02x".format(it) }
        return "$size-${hex.take(32)}"
    }
}
