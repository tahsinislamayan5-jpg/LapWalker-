package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.repository.WorkoutRepository

class DeleteWorkoutUseCase(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteWorkout(id)
    }
}
