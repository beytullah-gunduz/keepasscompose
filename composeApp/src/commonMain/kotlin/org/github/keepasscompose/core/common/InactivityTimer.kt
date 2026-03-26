package org.github.keepasscompose.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InactivityTimer(private val scope: CoroutineScope) {
    private var timerJob: Job? = null

    private val _isExpired = MutableStateFlow(false)
    val isExpired: StateFlow<Boolean> = _isExpired.asStateFlow()

    private var timeoutMillis: Long = 0L

    fun start(timeoutSeconds: Int) {
        timeoutMillis = timeoutSeconds * 1000L
        resetTimer()
    }

    fun resetTimer() {
        _isExpired.value = false
        timerJob?.cancel()
        if (timeoutMillis <= 0) return

        timerJob =
            scope.launch {
                delay(timeoutMillis)
                _isExpired.value = true
            }
    }

    fun onUserActivity() {
        if (!_isExpired.value && timerJob?.isActive == true) {
            resetTimer()
        }
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
        _isExpired.value = false
    }
}
