package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.repository.GamificationRepository

class EnsureGamificationInitializedUseCase(
    private val repository: GamificationRepository
) {
    suspend operator fun invoke() = repository.ensureInitialized()
}
