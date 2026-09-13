package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.model.WorkoutRecord
import com.takwa.lapwalker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow

class GetWorkoutsUseCase(
    private val repository: WorkoutRepository
) {
    operator fun invoke(): Flow<List<WorkoutRecord>> =
        repository.allWorkoutsFlow
}
