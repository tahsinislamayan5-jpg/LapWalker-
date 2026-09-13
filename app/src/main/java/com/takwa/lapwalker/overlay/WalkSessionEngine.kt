package com.takwa.lapwalker.overlay

import com.takwa.lapwalker.domain.model.WalkSessionState
import com.takwa.lapwalker.domain.usecase.RecordLapUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WalkSessionEngine(
    private val scope: CoroutineScope,
    private val recordLapUseCase: RecordLapUseCase = RecordLapUseCase()
) {
    private val _sessionState = MutableStateFlow(WalkSessionState())
    val sessionState: StateFlow<WalkSessionState> = _sessionState.asStateFlow()

    private var timerJob: Job? = null

    fun dispatch(intent: OverlayIntent) {
        when (intent) {
            is OverlayIntent.Initialize -> {
                _sessionState.value = WalkSessionState(
                    isRunning = true,
                    laps = 0,
                    elapsedSeconds = 0L,
                    lapLengthFeet = intent.lapLengthFeet,
                    targetKm = intent.targetKm,
                    weightKg = intent.weightKg,
                    vibrateEnabled = intent.vibrateEnabled
                )
                startTimer()
            }
            is OverlayIntent.RecordLap -> {
                _sessionState.update { recordLapUseCase(it) }
            }
            is OverlayIntent.StopSession -> {
                timerJob?.cancel()
                _sessionState.update { it.copy(isRunning = false) }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                _sessionState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    fun stop() {
        timerJob?.cancel()
    }
}
