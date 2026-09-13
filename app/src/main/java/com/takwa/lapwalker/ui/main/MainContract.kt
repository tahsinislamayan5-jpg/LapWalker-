package com.takwa.lapwalker.ui.main

import com.takwa.lapwalker.core.math.LapMath
import com.takwa.lapwalker.domain.model.AppUpdateInfo
import com.takwa.lapwalker.domain.model.WorkoutRecord
import java.io.File

enum class MainTab {
    WALK,
    STEPS,
    HISTORY,
    WORKOUTS
}

sealed interface UpdateStatus {
    object Idle : UpdateStatus
    data class Checking(val isManual: Boolean) : UpdateStatus
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : UpdateStatus
    data class Downloading(val percent: Int, val downloaded: Long, val total: Long) : UpdateStatus
    data class ReadyToInstall(val apkFile: File) : UpdateStatus
    data class UpToDate(val isManual: Boolean) : UpdateStatus
    data class Error(val message: String, val isManual: Boolean) : UpdateStatus
}

data class MainViewState(
    val selectedTab: MainTab = MainTab.WALK,
    val lapFeet: Float = 36.0f,
    val targetKm: Float = 1.0f,
    val weightKg: Float = 70.0f,
    val vibrateEnabled: Boolean = true,
    val isDarkTheme: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val updateStatus: UpdateStatus = UpdateStatus.Idle,
    val workouts: List<WorkoutRecord> = emptyList(),
    val stepState: com.takwa.lapwalker.domain.model.StepState = com.takwa.lapwalker.domain.model.StepState(),
    val calisthenicsProgress: List<com.takwa.lapwalker.data.local.db.entity.CalisthenicsProgressEntity> = emptyList()
) {
    val lapDistanceKm: Double
        get() = LapMath.feetToKm(lapFeet)

    val requiredLaps: Int
        get() = LapMath.calculateRequiredLaps(targetKm, lapFeet)

    val estCalories: Double
        get() = LapMath.calculateCaloriesBurned(targetKm.toDouble(), weightKg)

    val totalLifetimeDistanceKm: Double
        get() = workouts.sumOf { it.distanceKm }

    val totalLifetimeLaps: Int
        get() = workouts.sumOf { it.laps }

    val totalLifetimeCalories: Double
        get() = workouts.sumOf { it.caloriesBurned }

    val totalLifetimeDurationSeconds: Long
        get() = workouts.sumOf { it.durationSeconds }
}

sealed interface MainIntent {
    data class LoadInitialPreferences(
        val feet: Float,
        val targetKm: Float,
        val weight: Float,
        val vibrate: Boolean,
        val darkTheme: Boolean
    ) : MainIntent

    data class UpdateLapFeet(val feet: Float) : MainIntent
    data class UpdateTargetKm(val km: Float) : MainIntent
    data class UpdateWeight(val weight: Float) : MainIntent
    data class ToggleVibration(val enabled: Boolean) : MainIntent
    object ToggleTheme : MainIntent
    data class UpdatePermissionStatus(val granted: Boolean) : MainIntent

    data class CheckForUpdate(val isManual: Boolean = false) : MainIntent
    data class StartDownloadUpdate(val downloadUrl: String) : MainIntent
    object DismissUpdateDialog : MainIntent

    data class SelectTab(val tab: MainTab) : MainIntent
    data class DeleteWorkout(val id: Long) : MainIntent
    object ClearAllWorkouts : MainIntent

    data class UpdateStepPermission(val granted: Boolean) : MainIntent
    data class UpdateStepGoal(val goal: Int) : MainIntent
    object ResetTodaySteps : MainIntent
    data class ToggleSessionOnlySteps(val enabled: Boolean) : MainIntent
}
