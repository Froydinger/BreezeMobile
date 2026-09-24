package com.froydinger.breeze.data

import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserImportTest {
    @Test
    fun emptyBookmarkHrefIsSkippedWithoutAbortingImport() {
        val bookmarks = BrowserImport.bookmarksHtml("<A HREF=\"\">Empty</A>")
        assertTrue(bookmarks.isEmpty())
    }
}
