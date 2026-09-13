package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.repository.CalisthenicsRepository

class GetCalisthenicsProgressUseCase(private val repository: CalisthenicsRepository) {
    operator fun invoke() = repository.getAllProgress()
    suspend fun seedDefaults() = repository.ensureDefaultStepsUnlocked()
}
