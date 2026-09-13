package com.takwa.lapwalker.core.math

import com.takwa.lapwalker.core.constants.AppConstants
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

object LapMath {

    fun feetToKm(feet: Float): Double =
        feet * AppConstants.FEET_TO_KM_MULTIPLIER

    fun calculateDistanceKm(laps: Int, feetPerLap: Float): Double =
        laps * feetToKm(feetPerLap)

    fun calculateRequiredLaps(targetKm: Float, feetPerLap: Float): Int {
        val lapKm = feetToKm(feetPerLap)
        return if (lapKm > 0) ceil(targetKm / lapKm).toInt() else 0
    }

    fun calculateRemainingKm(targetKm: Float, currentDistanceKm: Double): Double =
        max(0.0, targetKm - currentDistanceKm)

    fun calculateRemainingLaps(requiredLaps: Int, currentLaps: Int): Int =
        max(0, requiredLaps - currentLaps)

    fun calculateCaloriesBurned(distanceKm: Double, weightKg: Float): Double =
        distanceKm * weightKg * AppConstants.CALORIE_FACTOR

    fun calculateAverageLapTime(elapsedSeconds: Long, laps: Int): Double =
        if (laps > 0 && elapsedSeconds > 0) elapsedSeconds.toDouble() / laps else 0.0

    fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, remainingSeconds)
    }
}
