package com.tsundoku.app.library

/** Rules for user-chosen folders (paths relative to internal storage). Pure, unit-tested. */
object FolderRules {

    /** True if [folder] is an excluded folder or inside one. Android storage ignores letter case. */
    fun isExcluded(folder: String, excluded: Set<String>): Boolean {
        val f = folder.trim('/')
        return excluded.any { ex ->
            val e = ex.trim('/')
            e.isNotEmpty() && (f.equals(e, ignoreCase = true) || f.startsWith("$e/", ignoreCase = true))
        }
    }

    /**
     * Turns the id Android's folder picker returns ("primary:Download/Novels") into a relative
     * path ("Download/Novels"). Other storages (SD cards, cloud) aren't supported: null.
     */
    fun relativeFromTreeDocId(docId: String): String? =
        if (docId.startsWith("primary:")) docId.removePrefix("primary:").trim('/') else null
}
