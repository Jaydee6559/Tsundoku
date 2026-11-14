package com.tsundoku.app.library

import com.tsundoku.app.data.db.BookEntity

/** A group of books that are (probably) the same novel. */
data class DuplicateGroup(
    /** Exact = byte-for-byte the same file (e.g. a Download original and its ReadEra copy). */
    val exact: Boolean,
    val books: List<BookEntity>,
)

/** Finds duplicates in the library. Pure Kotlin, unit-tested. */
object DuplicateRules {

    /**
     * Exact copies first (same fingerprint), then "possibly the same" (same title and author but
     * different files: other editions or translations). Each book appears in at most one group.
     */
    fun find(books: List<BookEntity>): List<DuplicateGroup> {
        val exact = books
            .groupBy { it.fingerprint }
            .values
            .filter { it.size > 1 }
            .map { DuplicateGroup(exact = true, books = it.sortedBy { b -> b.addedAt }) }

        val inExact = exact.flatMap { g -> g.books.map { it.id } }.toSet()
        val similar = books
            .filter { it.id !in inExact }
            .groupBy { similarityKey(it) }
            .filterKeys { it.isNotBlank() }
            .values
            .filter { group -> group.size > 1 && group.map { it.fingerprint }.toSet().size > 1 }
            .map { DuplicateGroup(exact = false, books = it.sortedBy { b -> b.addedAt }) }

        return exact.sortedBy { it.books.first().title.lowercase() } +
            similar.sortedBy { it.books.first().title.lowercase() }
    }

    /** Same words in title + author, ignoring case, accents, punctuation and volume decorations. */
    internal fun similarityKey(book: BookEntity): String {
        val title = LibraryQuery.normalize(book.title)
        if (title.length < MIN_TITLE) return ""
        return title + "|" + LibraryQuery.normalize(book.authors.orEmpty())
    }

    private const val MIN_TITLE = 3
}
