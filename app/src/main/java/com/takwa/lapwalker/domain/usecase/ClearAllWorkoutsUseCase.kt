package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.repository.WorkoutRepository

class ClearAllWorkoutsUseCase(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke() {
        repository.clearAllWorkouts()
    }
}
