package com.tsundoku.app.epub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.random.Random

class FileFingerprintTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(name: String, bytes: ByteArray) = tmp.newFile(name).apply { writeBytes(bytes) }

    @Test
    fun `same content gives same fingerprint even after rename`() {
        val data = Random(1).nextBytes(300_000)
        val a = file("a.epub", data)
        val b = file("renamed.epub", data)
        assertEquals(FileFingerprint.compute(a), FileFingerprint.compute(b))
    }

    @Test
    fun `changing the end of a big file changes the fingerprint`() {
        val data = Random(2).nextBytes(300_000)
        val changed = data.copyOf().also { it[it.size - 10] = (it[it.size - 10] + 1).toByte() }
        assertNotEquals(
            FileFingerprint.compute(file("x.epub", data)),
            FileFingerprint.compute(file("y.epub", changed)),
        )
    }

    @Test
    fun `small and empty files work and include the size`() {
        val small = FileFingerprint.compute(file("s.epub", byteArrayOf(1, 2, 3)))
        val empty = FileFingerprint.compute(file("e.epub", ByteArray(0)))
        assertTrue(small.startsWith("3-"))
        assertTrue(empty.startsWith("0-"))
        assertNotEquals(small, empty)
    }
}
