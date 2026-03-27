package org.github.keepasscompose.core.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NativeMessagingInstallerTest {
    @Test
    fun hostNameFollowsConvention() {
        assertEquals("org.keepasscompose.browser", NativeMessagingInstaller.HOST_NAME)
    }

    @Test
    fun allBrowsersHaveManifestDirectory() {
        for (browser in NativeMessagingInstaller.Browser.entries) {
            val dir = NativeMessagingInstaller.getManifestDirectory(browser)
            assertNotNull(dir)
            assertTrue(dir.path.isNotEmpty())
        }
    }

    @Test
    fun linuxChromeManifestDirectoryIsCorrect() {
        val dir = NativeMessagingInstaller.getManifestDirectory(NativeMessagingInstaller.Browser.CHROME)
        assertTrue(dir.path.contains("NativeMessagingHosts"))
    }

    @Test
    fun firefoxManifestDirectoryDiffersFromChrome() {
        val chromeDir = NativeMessagingInstaller.getManifestDirectory(NativeMessagingInstaller.Browser.CHROME)
        val firefoxDir = NativeMessagingInstaller.getManifestDirectory(NativeMessagingInstaller.Browser.FIREFOX)
        assertTrue(chromeDir.path != firefoxDir.path)
    }

    @Test
    fun getInstalledBrowsersReturnsEmptySetWhenNoneInstalled() {
        // This test just verifies the method doesn't crash
        val installed = NativeMessagingInstaller.getInstalledBrowsers()
        assertNotNull(installed)
    }

    @Test
    fun allBrowserEntriesHaveDisplayName() {
        for (browser in NativeMessagingInstaller.Browser.entries) {
            assertTrue(browser.displayName.isNotBlank())
        }
    }
}
