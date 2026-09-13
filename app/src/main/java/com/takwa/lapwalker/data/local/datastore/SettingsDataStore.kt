package com.takwa.lapwalker.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.takwa.lapwalker.core.constants.AppConstants
import com.takwa.lapwalker.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lap_walker_settings")

class SettingsDataStore(
    private val context: Context
) {
    companion object {
        private val KEY_LAP_FEET = floatPreferencesKey("lap_feet")
        private val KEY_TARGET_KM = floatPreferencesKey("target_km")
        private val KEY_WEIGHT = floatPreferencesKey("weight")
        private val KEY_VIBRATE = booleanPreferencesKey("vibrate")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_SESSION_ONLY_STEPS = booleanPreferencesKey("session_only_steps")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserSettings(
                lapFeet = prefs[KEY_LAP_FEET] ?: AppConstants.DEFAULT_LAP_FEET,
                targetKm = prefs[KEY_TARGET_KM] ?: AppConstants.DEFAULT_TARGET_KM,
                weightKg = prefs[KEY_WEIGHT] ?: AppConstants.DEFAULT_WEIGHT_KG,
                vibrateEnabled = prefs[KEY_VIBRATE] ?: AppConstants.DEFAULT_VIBRATE,
                isDarkTheme = prefs[KEY_DARK_THEME] ?: AppConstants.DEFAULT_DARK_THEME,
                sessionOnlySteps = prefs[KEY_SESSION_ONLY_STEPS] ?: false
            )
        }

    suspend fun getSettings(): UserSettings =
        settingsFlow.first()

    suspend fun updateSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAP_FEET] = settings.lapFeet
            prefs[KEY_TARGET_KM] = settings.targetKm
            prefs[KEY_WEIGHT] = settings.weightKg
            prefs[KEY_VIBRATE] = settings.vibrateEnabled
            prefs[KEY_DARK_THEME] = settings.isDarkTheme
            prefs[KEY_SESSION_ONLY_STEPS] = settings.sessionOnlySteps
        }
    }
}
