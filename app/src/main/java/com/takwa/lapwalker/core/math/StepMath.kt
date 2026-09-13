package com.takwa.lapwalker.core.math

import kotlin.math.roundToInt

object StepMath {

    // Average stride length is ~0.762 meters (2.5 feet)
    private const val DEFAULT_STRIDE_METERS = 0.762f

    fun calculateDistanceKm(steps: Int, strideLengthMeters: Float = DEFAULT_STRIDE_METERS): Double {
        return (steps * strideLengthMeters) / 1000.0
    }

    // Standard baseline: ~0.04 kcal per step for a 70kg individual, scaled by user's weight
    fun calculateCalories(steps: Int, weightKg: Float): Double {
        val baseCalories = steps * 0.04
        val weightFactor = (weightKg / 70.0f).coerceAtLeast(0.5f)
        return baseCalories * weightFactor
    }

    // Walking cadence is roughly 100-110 steps per minute
    fun calculateActiveMinutes(steps: Int): Int {
        return (steps / 100.0).roundToInt()
    }

    fun calculateProgressPercent(steps: Int, goal: Int): Int {
        if (goal <= 0) return 0
        return ((steps.toDouble() / goal.toDouble()) * 100).roundToInt().coerceIn(0, 100)
    }
}
