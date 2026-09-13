package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.repository.GamificationRepository

class CompleteArmoryQuestUseCase(
    private val repository: GamificationRepository
) {
    suspend operator fun invoke(questId: String) = repository.completeArmoryQuest(questId)
}
