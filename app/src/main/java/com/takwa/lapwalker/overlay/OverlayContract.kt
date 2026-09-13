package com.takwa.lapwalker.overlay

sealed interface OverlayIntent {
    data class Initialize(
        val lapLengthFeet: Float,
        val targetKm: Float,
        val weightKg: Float,
        val vibrateEnabled: Boolean
    ) : OverlayIntent

    object RecordLap : OverlayIntent
    object StopSession : OverlayIntent
}
