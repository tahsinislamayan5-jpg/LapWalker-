package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.domain.model.UserSettings
import com.takwa.lapwalker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<UserSettings> =
        repository.settingsFlow

    suspend fun getSync(): UserSettings =
        repository.getSettings()
}
