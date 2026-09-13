package com.takwa.lapwalker.data.repository

import com.takwa.lapwalker.data.local.datastore.SettingsDataStore
import com.takwa.lapwalker.domain.model.UserSettings
import com.takwa.lapwalker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override val settingsFlow: Flow<UserSettings> =
        dataStore.settingsFlow

    override suspend fun getSettings(): UserSettings =
        dataStore.getSettings()

    override suspend fun updateSettings(settings: UserSettings) {
        dataStore.updateSettings(settings)
    }
}
