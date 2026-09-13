package com.takwa.lapwalker.domain.repository

import com.takwa.lapwalker.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<UserSettings>
    suspend fun getSettings(): UserSettings
    suspend fun updateSettings(settings: UserSettings)
}
