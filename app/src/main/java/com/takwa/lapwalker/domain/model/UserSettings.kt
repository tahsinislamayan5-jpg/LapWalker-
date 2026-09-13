package com.takwa.lapwalker.domain.model

import com.takwa.lapwalker.core.constants.AppConstants

data class UserSettings(
    val lapFeet: Float = AppConstants.DEFAULT_LAP_FEET,
    val targetKm: Float = AppConstants.DEFAULT_TARGET_KM,
    val weightKg: Float = AppConstants.DEFAULT_WEIGHT_KG,
    val vibrateEnabled: Boolean = AppConstants.DEFAULT_VIBRATE,
    val isDarkTheme: Boolean = AppConstants.DEFAULT_DARK_THEME,
    val sessionOnlySteps: Boolean = false
)
