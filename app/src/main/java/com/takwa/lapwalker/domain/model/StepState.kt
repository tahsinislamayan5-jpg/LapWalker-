package com.takwa.lapwalker.domain.model

import com.takwa.lapwalker.core.math.StepMath

data class StepState(
    val todaySteps: Int = 0,
    val stepGoal: Int = 10000,
    val weightKg: Float = 70.0f,
    val hasPermission: Boolean = false,
    val isSensorAvailable: Boolean = true,
    val isSessionOnlyMode: Boolean = false,
    val dateString: String = ""
) {
    val distanceKm: Double
        get() = StepMath.calculateDistanceKm(todaySteps)

    val caloriesBurned: Double
        get() = StepMath.calculateCalories(todaySteps, weightKg)

    val activeMinutes: Int
        get() = StepMath.calculateActiveMinutes(todaySteps)

    val progressPercent: Int
        get() = StepMath.calculateProgressPercent(todaySteps, stepGoal)

    val remainingSteps: Int
        get() = (stepGoal - todaySteps).coerceAtLeast(0)

    val isGoalReached: Boolean
        get() = todaySteps >= stepGoal && stepGoal > 0
}
