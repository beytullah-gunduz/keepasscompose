package org.github.keepasscompose.core.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InactivityTimerTest {

    @Test
    fun timerExpiresAfterTimeout() = runTest {
        val timer = InactivityTimer(this)
        timer.start(5)
        assertFalse(timer.isExpired.value)
        advanceTimeBy(5001)
        assertTrue(timer.isExpired.value)
    }

    @Test
    fun userActivityResetsTimer() = runTest {
        val timer = InactivityTimer(this)
        timer.start(5)
        advanceTimeBy(3000)
        assertFalse(timer.isExpired.value)
        timer.onUserActivity()
        advanceTimeBy(3000)
        assertFalse(timer.isExpired.value) // Reset, so not expired yet
        advanceTimeBy(2001)
        assertTrue(timer.isExpired.value) // Now expired (5s after reset)
    }

    @Test
    fun stopPreventsExpiration() = runTest {
        val timer = InactivityTimer(this)
        timer.start(1)
        timer.stop()
        advanceTimeBy(2000)
        assertFalse(timer.isExpired.value)
    }

    @Test
    fun zeroTimeoutNeverExpires() = runTest {
        val timer = InactivityTimer(this)
        timer.start(0)
        advanceTimeBy(100_000)
        assertFalse(timer.isExpired.value)
    }
}
