package com.froydinger.breeze.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChromiumCredentialsOriginTest {
    @Test fun acceptsOnlyExactSecureOrigins() {
        assertEquals("https://example.com", ChromiumCredentials.httpsOrigin("https://Example.COM/login"))
        assertEquals("https://example.com:8443", ChromiumCredentials.httpsOrigin("https://example.com:8443/login"))
        assertNull(ChromiumCredentials.httpsOrigin("http://example.com/login"))
        assertNull(ChromiumCredentials.httpsOrigin("https://user@example.com/login"))
    }
}
