package com.takwa.lapwalker.ui.calisthenics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takwa.lapwalker.data.catalog.CalisthenicsCatalog
import com.takwa.lapwalker.domain.model.calisthenics.CalisthenicsStep
import com.takwa.lapwalker.domain.model.calisthenics.ExerciseMetricType
import com.takwa.lapwalker.domain.usecase.RecordExerciseAttemptUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PracticeUiState(
    val step: CalisthenicsStep? = null,
    val currentSet: Int = 1,
    val currentReps: Int = 0,
    val holdSecondsRemaining: Int = 0,
    val isHoldRunning: Boolean = false,
    val isResting: Boolean = false,
    val restSecondsRemaining: Int = 0,
    val isFinished: Boolean = false,
    val wasBenchmarkPassed: Boolean = false
)

class ExercisePracticeViewModel(
    private val recordAttemptUseCase: RecordExerciseAttemptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var restJob: Job? = null

    fun loadStep(stepId: String) {
        val step = CalisthenicsCatalog.getStep(stepId) ?: return
        _uiState.update {
            it.copy(
                step = step,
                holdSecondsRemaining = if (step.metricType == ExerciseMetricType.TIME_HOLD) step.targetRepsOrSeconds else 0,
                restSecondsRemaining = step.restDurationSeconds
            )
        }
    }

    fun incrementRep() {
        _uiState.update { it.copy(currentReps = it.currentReps + 1) }
    }

    fun completeCurrentSet() {
        val state = _uiState.value
        val step = state.step ?: return

        if (state.currentSet >= step.targetSets) {
            finishSession(passed = true)
        } else {
            if (step.restDurationSeconds > 0) {
                startRestTimer()
            } else {
                advanceToNextSet()
            }
        }
    }

    private fun startRestTimer() {
        val step = _uiState.value.step ?: return
        restJob?.cancel()
        _uiState.update { it.copy(isResting = true, restSecondsRemaining = step.restDurationSeconds) }
        restJob = viewModelScope.launch {
            while (_uiState.value.restSecondsRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(restSecondsRemaining = it.restSecondsRemaining - 1) }
            }
            skipRest()
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _uiState.update { it.copy(isResting = false) }
        advanceToNextSet()
    }

    private fun advanceToNextSet() {
        val step = _uiState.value.step ?: return
        _uiState.update {
            it.copy(
                currentSet = it.currentSet + 1,
                currentReps = 0,
                holdSecondsRemaining = if (step.metricType == ExerciseMetricType.TIME_HOLD) step.targetRepsOrSeconds else 0,
                isHoldRunning = false
            )
        }
    }

    fun toggleHoldTimer() {
        val isRunning = _uiState.value.isHoldRunning
        if (isRunning) {
            timerJob?.cancel()
            _uiState.update { it.copy(isHoldRunning = false) }
        } else {
            _uiState.update { it.copy(isHoldRunning = true) }
            timerJob = viewModelScope.launch {
                while (_uiState.value.holdSecondsRemaining > 0 && _uiState.value.isHoldRunning) {
                    delay(1000)
                    _uiState.update { it.copy(holdSecondsRemaining = it.holdSecondsRemaining - 1) }
                }
                if (_uiState.value.holdSecondsRemaining == 0) {
                    _uiState.update { it.copy(isHoldRunning = false) }
                    completeCurrentSet()
                }
            }
        }
    }

    fun finishSession(passed: Boolean) {
        val state = _uiState.value
        val step = state.step ?: return
        viewModelScope.launch {
            recordAttemptUseCase(
                stepId = step.id,
                setsCompleted = if (passed) step.targetSets else state.currentSet - 1,
                targetSets = step.targetSets,
                repsOrSecondsAchieved = if (step.metricType == ExerciseMetricType.REPS) state.currentReps else step.targetRepsOrSeconds,
                wasBenchmarkPassed = passed
            )
            _uiState.update { it.copy(isFinished = true, wasBenchmarkPassed = passed) }
        }
    }
}
