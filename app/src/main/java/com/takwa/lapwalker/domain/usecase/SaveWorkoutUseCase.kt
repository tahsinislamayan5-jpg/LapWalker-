package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.model.WalkSessionState
import com.takwa.lapwalker.domain.model.WorkoutRecord
import com.takwa.lapwalker.domain.repository.WorkoutRepository

class SaveWorkoutUseCase(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke(sessionState: WalkSessionState): Long {
        if (sessionState.laps <= 0) return -1L

        val record = WorkoutRecord(
            timestamp = System.currentTimeMillis(),
            laps = sessionState.laps,
            distanceKm = sessionState.currentDistanceKm,
            durationSeconds = sessionState.elapsedSeconds,
            caloriesBurned = sessionState.caloriesBurned,
            avgLapTimeSec = sessionState.avgLapTimeSec
        )
        return repository.saveWorkout(record)
    }
}
