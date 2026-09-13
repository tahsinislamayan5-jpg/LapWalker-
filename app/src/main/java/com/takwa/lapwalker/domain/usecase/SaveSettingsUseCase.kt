package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.model.UserSettings
import com.takwa.lapwalker.domain.repository.SettingsRepository

class SaveSettingsUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(settings: UserSettings) {
        repository.updateSettings(settings)
    }
}
