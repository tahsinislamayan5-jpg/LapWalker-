package com.takwa.lapwalker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takwa.lapwalker.data.repository.UpdateRepository
import com.takwa.lapwalker.domain.model.UserSettings
import com.takwa.lapwalker.domain.usecase.ClearAllWorkoutsUseCase
import com.takwa.lapwalker.domain.usecase.DeleteWorkoutUseCase
import com.takwa.lapwalker.domain.usecase.GetSettingsUseCase
import com.takwa.lapwalker.domain.usecase.GetWorkoutsUseCase
import com.takwa.lapwalker.domain.usecase.SaveSettingsUseCase
import com.takwa.lapwalker.core.sensors.StepSensorManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    val updateRepository: UpdateRepository,
    private val getWorkoutsUseCase: GetWorkoutsUseCase,
    private val deleteWorkoutUseCase: DeleteWorkoutUseCase,
    private val clearAllWorkoutsUseCase: ClearAllWorkoutsUseCase,
    private val stepSensorManager: StepSensorManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainViewState())
    val uiState: StateFlow<MainViewState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            stepSensorManager.todaySteps.collect { steps ->
                _uiState.update { current ->
                    current.copy(
                        stepState = current.stepState.copy(
                            todaySteps = steps,
                            stepGoal = stepSensorManager.stepGoal.value,
                            weightKg = current.weightKg,
                            isSensorAvailable = stepSensorManager.isSensorAvailable
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            stepSensorManager.stepGoal.collect { goal ->
                _uiState.update { current ->
                    current.copy(
                        stepState = current.stepState.copy(stepGoal = goal)
                    )
                }
            }
        }
        viewModelScope.launch {
            stepSensorManager.sessionOnlyMode.collect { enabled ->
                _uiState.update { current ->
                    current.copy(
                        stepState = current.stepState.copy(isSessionOnlyMode = enabled)
                    )
                }
            }
        }
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                stepSensorManager.setSessionOnlyMode(settings.sessionOnlySteps)
                _uiState.update { current ->
                    current.copy(
                        lapFeet = settings.lapFeet,
                        targetKm = settings.targetKm,
                        weightKg = settings.weightKg,
                        vibrateEnabled = settings.vibrateEnabled,
                        isDarkTheme = settings.isDarkTheme
                    )
                }
            }
        }

        viewModelScope.launch {
            getWorkoutsUseCase().collect { workoutList ->
                _uiState.update { current ->
                    current.copy(workouts = workoutList)
                }
            }
        }
    }

    fun dispatch(intent: MainIntent) {
        when (intent) {
            is MainIntent.LoadInitialPreferences -> {
                _uiState.update {
                    it.copy(
                        lapFeet = intent.feet,
                        targetKm = intent.targetKm,
                        weightKg = intent.weight,
                        vibrateEnabled = intent.vibrate,
                        isDarkTheme = intent.darkTheme
                    )
                }
            }
            is MainIntent.UpdateLapFeet -> {
                _uiState.update { it.copy(lapFeet = intent.feet) }
                persistCurrentSettings()
            }
            is MainIntent.UpdateTargetKm -> {
                _uiState.update { it.copy(targetKm = intent.km) }
                persistCurrentSettings()
            }
            is MainIntent.UpdateWeight -> {
                _uiState.update { it.copy(weightKg = intent.weight) }
                persistCurrentSettings()
            }
            is MainIntent.ToggleVibration -> {
                _uiState.update { it.copy(vibrateEnabled = intent.enabled) }
                persistCurrentSettings()
            }
            is MainIntent.ToggleTheme -> {
                _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
                persistCurrentSettings()
            }
            is MainIntent.UpdatePermissionStatus -> {
                _uiState.update { it.copy(hasOverlayPermission = intent.granted) }
            }
            is MainIntent.CheckForUpdate -> {
                performUpdateCheck(intent.isManual)
            }
            is MainIntent.StartDownloadUpdate -> {
                startDownload(intent.downloadUrl)
            }
            is MainIntent.DismissUpdateDialog -> {
                _uiState.update { it.copy(updateStatus = UpdateStatus.Idle) }
            }
            is MainIntent.SelectTab -> {
                _uiState.update { it.copy(selectedTab = intent.tab) }
            }
            is MainIntent.DeleteWorkout -> {
                viewModelScope.launch {
                    deleteWorkoutUseCase(intent.id)
                }
            }
            is MainIntent.ClearAllWorkouts -> {
                viewModelScope.launch {
                    clearAllWorkoutsUseCase()
                }
            }
            is MainIntent.UpdateStepPermission -> {
                _uiState.update { current ->
                    current.copy(stepState = current.stepState.copy(hasPermission = intent.granted))
                }
                if (intent.granted) {
                    stepSensorManager.startListening()
                } else {
                    stepSensorManager.stopListening()
                }
            }
            is MainIntent.UpdateStepGoal -> {
                stepSensorManager.updateGoal(intent.goal)
            }
            is MainIntent.ResetTodaySteps -> {
                stepSensorManager.resetTodaySteps()
            }
            is MainIntent.ToggleSessionOnlySteps -> {
                stepSensorManager.setSessionOnlyMode(intent.enabled)
                viewModelScope.launch {
                    val current = getSettingsUseCase().first()
                    saveSettingsUseCase(current.copy(sessionOnlySteps = intent.enabled))
                }
            }
        }
    }

    fun startStepSensorIfPermitted(hasPermission: Boolean) {
        _uiState.update { current ->
            current.copy(stepState = current.stepState.copy(hasPermission = hasPermission))
        }
        if (hasPermission) {
            stepSensorManager.startListening()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stepSensorManager.stopListening()
    }


    private fun performUpdateCheck(isManual: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(updateStatus = UpdateStatus.Checking(isManual)) }
            val result = updateRepository.checkForUpdate()
            result.onSuccess { updateInfo ->
                if (updateInfo != null && updateInfo.isUpdateAvailable) {
                    _uiState.update { it.copy(updateStatus = UpdateStatus.UpdateAvailable(updateInfo)) }
                } else {
                    _uiState.update { it.copy(updateStatus = UpdateStatus.UpToDate(isManual)) }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(updateStatus = UpdateStatus.Error(error.localizedMessage ?: "Failed to check update", isManual))
                }
            }
        }
    }

    private fun startDownload(downloadUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(updateStatus = UpdateStatus.Downloading(0, 0, 0)) }
            val result = updateRepository.downloadApk(downloadUrl) { percent, downloaded, total ->
                _uiState.update {
                    it.copy(updateStatus = UpdateStatus.Downloading(percent, downloaded, total))
                }
            }
            result.onSuccess { apkFile ->
                _uiState.update { it.copy(updateStatus = UpdateStatus.ReadyToInstall(apkFile)) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(updateStatus = UpdateStatus.Error("Download failed: ${error.localizedMessage}", true))
                }
            }
        }
    }

    private fun persistCurrentSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            saveSettingsUseCase(
                UserSettings(
                    lapFeet = state.lapFeet,
                    targetKm = state.targetKm,
                    weightKg = state.weightKg,
                    vibrateEnabled = state.vibrateEnabled,
                    isDarkTheme = state.isDarkTheme
                )
            )
        }
    }
}
