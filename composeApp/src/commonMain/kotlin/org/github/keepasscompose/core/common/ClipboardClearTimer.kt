package org.github.keepasscompose.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClipboardClearTimer(
    private val scope: CoroutineScope,
    private val clipboardManager: ClipboardManager,
) {
    private var timerJob: Job? = null
    private var copiedText: String? = null

    private val _secondsRemaining = MutableStateFlow<Int?>(null)
    val secondsRemaining: StateFlow<Int?> = _secondsRemaining.asStateFlow()

    fun copyAndScheduleClear(text: String, timeoutSeconds: Int) {
        cancel()
        clipboardManager.copyToClipboard(text)
        copiedText = text

        if (timeoutSeconds <= 0) return

        timerJob = scope.launch {
            for (remaining in timeoutSeconds downTo 1) {
                _secondsRemaining.value = remaining
                delay(1000L)
            }
            _secondsRemaining.value = 0
            clipboardManager.clearClipboard()
            copiedText = null
            _secondsRemaining.value = null
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        copiedText = null
        _secondsRemaining.value = null
    }
}
