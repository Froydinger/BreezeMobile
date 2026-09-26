package com.froydinger.breeze.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PwaManifestPreflightTest {
    @Test
    fun acceptsJsonManifestMimeTypesAndRejectsBinaryMime() {
        assertTrue(hasSupportedManifestMimeType("application/manifest+json; charset=utf-8"))
        assertTrue(hasSupportedManifestMimeType("application/json"))
        assertFalse(hasSupportedManifestMimeType("application/octet-stream"))
        assertFalse(hasSupportedManifestMimeType("text/html; charset=utf-8"))
    }

    @Test
    fun onlyStandaloneAndFullscreenDisplayModesPass() {
        assertEquals("standalone", standaloneDisplayMode("standalone", emptyList()))
        assertEquals("fullscreen", standaloneDisplayMode("fullscreen", emptyList()))
        assertEquals(null, standaloneDisplayMode("minimal-ui", emptyList()))
        assertEquals(null, standaloneDisplayMode("browser", emptyList()))
        assertEquals(null, standaloneDisplayMode("", emptyList()))
    }

    @Test
    fun displayOverridesUseTheFirstAndroidSupportedMode() {
        assertEquals("standalone", standaloneDisplayMode("browser", listOf("window-controls-overlay", "standalone")))
        assertEquals(null, standaloneDisplayMode("standalone", listOf("minimal-ui", "browser")))
        assertEquals("fullscreen", standaloneDisplayMode("browser", listOf("fullscreen", "standalone")))
    }

    @Test
    fun iconMustDeclare192PixelsAndLauncherPurpose() {
        assertTrue(declares192Icon("48x48 192x192 512x512", "any"))
        assertTrue(declares192Icon("192x192", "maskable"))
        assertTrue(declares192Icon("192x192", ""))
        assertFalse(declares192Icon("512x512", "any"))
        assertFalse(declares192Icon("192x192", "monochrome"))
    }

    @Test
    fun startUrlMustStayWithinPathBoundariesOfScope() {
        assertTrue(isWithinWebAppScope("/app", "/app"))
        assertTrue(isWithinWebAppScope("/app/home", "/app/"))
        assertFalse(isWithinWebAppScope("/apple/home", "/app"))
        assertFalse(isWithinWebAppScope("/outside", "/app/"))
    }
}
