package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.repository.CalisthenicsRepository

class RecordExerciseAttemptUseCase(private val repository: CalisthenicsRepository) {
    suspend operator fun invoke(stepId: String, setsCompleted: Int, targetSets: Int, repsOrSecondsAchieved: Int, wasBenchmarkPassed: Boolean) =
        repository.recordAttempt(stepId, setsCompleted, targetSets, repsOrSecondsAchieved, wasBenchmarkPassed)
}
