package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookScanRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanPlannerTest {

    private fun row(id: Long, path: String, fp: String, size: Long = 100, mod: Long = 1, missing: Boolean = false) =
        BookScanRow(id, path, size, mod, fp, missing)

    private fun file(path: String, size: Long = 100, mod: Long = 1) = FoundFile(path, size, mod)

    /** Fingerprints for tests: taken from a lookup table keyed by path. */
    private fun fps(vararg pairs: Pair<String, String>): (FoundFile) -> String {
        val map = pairs.toMap()
        return { map[it.path] ?: "fp-${it.path}" }
    }

    @Test
    fun `first scan - everything is new`() {
        val plan = ScanPlanner.plan(emptyList(), listOf(file("/a.epub"), file("/b.epub")), fps())
        assertEquals(2, plan.newFiles.size)
        assertTrue(plan.changed.isEmpty() && plan.moved.isEmpty() && plan.missing.isEmpty())
    }

    @Test
    fun `nothing changed - nothing to do`() {
        val plan = ScanPlanner.plan(listOf(row(1, "/a.epub", "x")), listOf(file("/a.epub")), fps())
        assertEquals(1, plan.unchangedCount)
        assertTrue(plan.newFiles.isEmpty() && plan.changed.isEmpty() && plan.missing.isEmpty())
    }

    @Test
    fun `size or date change means re-read`() {
        val plan = ScanPlanner.plan(listOf(row(1, "/a.epub", "x")), listOf(file("/a.epub", size = 200)), fps())
        assertEquals(listOf(1L), plan.changed.map { it.first })
    }

    @Test
    fun `moved file keeps its id`() {
        val plan = ScanPlanner.plan(
            existing = listOf(row(7, "/Download/a.epub", "same")),
            found = listOf(file("/Books/a-renamed.epub")),
            fingerprintOf = fps("/Books/a-renamed.epub" to "same"),
        )
        assertEquals(listOf(7L to "/Books/a-renamed.epub"), plan.moved.map { it.first to it.second.path })
        assertTrue(plan.newFiles.isEmpty())
        assertTrue(plan.missing.isEmpty())
    }

    @Test
    fun `deleted file is marked missing, not removed`() {
        val plan = ScanPlanner.plan(listOf(row(3, "/gone.epub", "g")), emptyList(), fps())
        assertEquals(listOf(3L), plan.missing)
    }

    @Test
    fun `already-missing file is not marked again`() {
        val plan = ScanPlanner.plan(listOf(row(3, "/gone.epub", "g", missing = true)), emptyList(), fps())
        assertTrue(plan.missing.isEmpty())
    }

    @Test
    fun `missing file that comes back at the same path reappears`() {
        val plan = ScanPlanner.plan(listOf(row(4, "/a.epub", "x", missing = true)), listOf(file("/a.epub")), fps())
        assertEquals(listOf(4L), plan.reappeared)
    }

    @Test
    fun `missing file that comes back at a new path is a move`() {
        val plan = ScanPlanner.plan(
            existing = listOf(row(5, "/old.epub", "fp5", missing = true)),
            found = listOf(file("/new.epub")),
            fingerprintOf = fps("/new.epub" to "fp5"),
        )
        assertEquals(listOf(5L), plan.moved.map { it.first })
    }

    @Test
    fun `two copies of one book - one is a move, the other is new`() {
        val plan = ScanPlanner.plan(
            existing = listOf(row(1, "/old.epub", "dup")),
            found = listOf(file("/copy1.epub"), file("/copy2.epub")),
            fingerprintOf = fps("/copy1.epub" to "dup", "/copy2.epub" to "dup"),
        )
        assertEquals(1, plan.moved.size)
        assertEquals(1, plan.newFiles.size)
        assertTrue(plan.missing.isEmpty())
    }

    @Test
    fun `mixed scan`() {
        val plan = ScanPlanner.plan(
            existing = listOf(
                row(1, "/keep.epub", "k"),
                row(2, "/edit.epub", "e"),
                row(3, "/move-from.epub", "m"),
                row(4, "/delete.epub", "d"),
            ),
            found = listOf(
                file("/keep.epub"),
                file("/edit.epub", mod = 99),
                file("/move-to.epub"),
                file("/brand-new.epub"),
            ),
            fingerprintOf = fps("/move-to.epub" to "m", "/brand-new.epub" to "n"),
        )
        assertEquals(1, plan.unchangedCount)
        assertEquals(listOf(2L), plan.changed.map { it.first })
        assertEquals(listOf(3L), plan.moved.map { it.first })
        assertEquals(listOf("/brand-new.epub"), plan.newFiles.map { it.first.path })
        assertEquals(listOf(4L), plan.missing)
    }
}
