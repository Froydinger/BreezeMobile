package com.froydinger.breeze.updates

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidUpdatePolicyTest {
    @Test fun offersOnlyHigherVersionCodes() {
        assertTrue(AndroidUpdatePolicy.shouldOffer(9, 8))
        assertFalse(AndroidUpdatePolicy.shouldOffer(8, 8))
        assertFalse(AndroidUpdatePolicy.shouldOffer(7, 8))
    }

    @Test fun acceptsOnlyTheExpectedSignedReleaseAssetUrl() {
        val tag = "android-v0.1.8-beta.1"
        val name = "Breeze-Android-0.1.8-beta.1-arm64.apk"
        val url = "https://github.com/Froydinger/BreezeMobile/releases/download/$tag/$name"
        assertTrue(AndroidUpdatePolicy.isTrustedReleaseAsset(tag, "0.1.8", name, url))
        assertFalse(AndroidUpdatePolicy.isTrustedReleaseAsset(tag, "0.1.7", name, url))
        assertFalse(AndroidUpdatePolicy.isTrustedReleaseAsset(tag, "0.1.8", name, url.replace("github.com", "github.example.com")))
        assertFalse(AndroidUpdatePolicy.isTrustedReleaseAsset(tag, "0.1.8", "other.apk", url))
        assertFalse(AndroidUpdatePolicy.isTrustedReleaseAsset("android-v0.1.8-rc.1", "0.1.8", name, url))
    }
}
