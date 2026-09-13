package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.model.WalkSessionState
import com.takwa.lapwalker.domain.model.WorkoutRecord
import com.takwa.lapwalker.domain.repository.GamificationRepository
import com.takwa.lapwalker.domain.repository.WorkoutRepository

class SaveWorkoutUseCase(
    private val repository: WorkoutRepository,
    private val gamificationRepository: GamificationRepository
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
        val id = repository.saveWorkout(record)
        gamificationRepository.onWorkoutCompleted(record)
        return id
    }
}
