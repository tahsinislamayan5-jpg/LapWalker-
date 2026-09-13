package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.data.catalog.HistoricWorkoutCatalog
import com.takwa.lapwalker.domain.repository.WorkoutRepository

class SeedHistoricWorkoutsUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke() {
        workoutRepository.seedHistoricWorkouts(HistoricWorkoutCatalog.initialHistory)
    }
}
