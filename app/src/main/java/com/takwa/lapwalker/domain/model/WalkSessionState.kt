package com.takwa.lapwalker.domain.model

import com.takwa.lapwalker.core.constants.AppConstants
import com.takwa.lapwalker.core.math.LapMath
import kotlin.math.min
import kotlin.math.roundToInt

data class WalkSessionState(
    val isRunning: Boolean = false,
    val laps: Int = 0,
    val elapsedSeconds: Long = 0L,
    val lapLengthFeet: Float = AppConstants.DEFAULT_LAP_FEET,
    val targetKm: Float = AppConstants.DEFAULT_TARGET_KM,
    val weightKg: Float = AppConstants.DEFAULT_WEIGHT_KG,
    val vibrateEnabled: Boolean = AppConstants.DEFAULT_VIBRATE
) {
    val lapDistanceKm: Double
        get() = LapMath.feetToKm(lapLengthFeet)

    val currentDistanceKm: Double
        get() = LapMath.calculateDistanceKm(laps, lapLengthFeet)

    val remainingKm: Double
        get() = LapMath.calculateRemainingKm(targetKm, currentDistanceKm)

    val requiredLaps: Int
        get() = LapMath.calculateRequiredLaps(targetKm, lapLengthFeet)

    val remainingLaps: Int
        get() = LapMath.calculateRemainingLaps(requiredLaps, laps)

    val avgLapTimeSec: Double
        get() = LapMath.calculateAverageLapTime(elapsedSeconds, laps)

    val caloriesBurned: Double
        get() = LapMath.calculateCaloriesBurned(currentDistanceKm, weightKg)

    val isTargetCompleted: Boolean
        get() = currentDistanceKm >= targetKm && laps > 0

    val progressPercent: Int
        get() = if (targetKm > 0) min(100, ((currentDistanceKm / targetKm) * 100).roundToInt()) else 0

    val formattedTime: String
        get() = LapMath.formatDuration(elapsedSeconds)
}
